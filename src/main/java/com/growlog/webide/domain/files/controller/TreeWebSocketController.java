package com.growlog.webide.domain.files.controller;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growlog.webide.domain.files.dto.tree.TreeNodeDto;
import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
import com.growlog.webide.domain.files.service.TreeService;
import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TreeWebSocketController {

	private final TreeService treeService;
	private final SimpMessagingTemplate messagingTemplate;
	private final ActiveInstanceRepository activeInstanceRepository;

	/**
	 * 클라이언트가 "/app/projects/{projectId}/tree/init"로 메시지를 보내면
	 * 서버가 해당 인스턴스의 전체 파일 트리를 반환함.
	 */
	@MessageMapping("/projects/{projectId}/tree/init")
	public void sendInitialTree(
		@DestinationVariable Long projectId,
		Message<?> message
	) {
		var accessor = SimpMessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
		if (accessor == null || accessor.getSessionAttributes() == null) {
			throw new AccessDeniedException("WebSocket 인증 실패: session 정보 없음");
		}

		Long userId = (Long)accessor.getSessionAttributes().get("userId");

		if (userId == null) {
			throw new AccessDeniedException("WebSocket 인증 실패: userId 없음");
		}

		log.info("[WS 인증] userId={}, projectId={}", userId, projectId);

		// 💡 서버에서 ActiveInstance 조회
		ActiveInstance inst = activeInstanceRepository
			.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		Long instanceId = inst.getId();

		List<TreeNodeDto> tree = treeService.buildTree(projectId, inst.getContainerId());

		if (!inst.getId().equals(instanceId)) {
			throw new AccessDeniedException("이 인스턴스에 접근 권한이 없습니다.");
		}

		WebSocketMessage msg = new WebSocketMessage("tree:init", tree);

		try {
			String json = new ObjectMapper().writeValueAsString(msg); // 💡 여기
			log.info("📤 보내는 메시지: {}", json);
		} catch (Exception e) {
			log.error("❌ 메시지 직렬화 실패", e);
		}

		messagingTemplate.convertAndSend(
			"/topic/projects/" + projectId + "/tree",
			msg
		);
	}
}
