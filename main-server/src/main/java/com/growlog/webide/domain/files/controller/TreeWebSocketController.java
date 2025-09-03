package com.growlog.webide.domain.files.controller;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import com.growlog.webide.domain.files.dto.tree.TreeNodeDto;
import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
import com.growlog.webide.domain.files.repository.FileMetaRepository;
import com.growlog.webide.domain.files.service.TreeService;
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
	private final FileMetaRepository fileMetaRepository;

	@MessageMapping("/projects/{projectId}/tree/init")
	public void sendInitialTree(
		@DestinationVariable Long projectId,
		Message<?> message
	) {
		var accessor = SimpMessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
		if (accessor == null || accessor.getSessionAttributes() == null) {
			throw new AccessDeniedException("WebSocket authentication failed: No session info");
		}

		Long userId = (Long)accessor.getSessionAttributes().get("userId");
		if (userId == null) {
			throw new AccessDeniedException("WebSocket authentication failed: No userId");
		}

		log.info("[WS] 트리 요청 userId={}, projectId={}", userId, projectId);

		// 📦 인스턴스 조회
		ActiveInstance inst = activeInstanceRepository
			.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		String containerId = inst.getContainerId();

		// ✅ 최초 1회만 동기화 수행 (DB에 아무 파일도 없다면)
		boolean isEmpty = fileMetaRepository.findAllByProjectIdAndDeletedFalse(projectId).isEmpty();
		if (isEmpty) {
			log.info("[WS] 최초 트리 요청 - 컨테이너에서 파일 구조 동기화 시작");
			treeService.syncFromContainer(projectId, containerId);
		}

		// 🌳 트리 구성
		List<TreeNodeDto> tree = treeService.buildTreeFromDb(projectId);

		WebSocketMessage msg = new WebSocketMessage("tree:init", tree);
		messagingTemplate.convertAndSend(
			"/topic/projects/" + projectId + "/tree",
			msg
		);
	}

}
