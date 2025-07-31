package com.growlog.webide.domain.files.controller;

import java.util.List;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growlog.webide.domain.files.dto.tree.TreeNodeDto;
import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
import com.growlog.webide.domain.files.service.TreeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TreeWebSocketController {

	private final TreeService treeService;
	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * 클라이언트가 "/app/instances/{instanceId}/tree/init"로 메시지를 보내면
	 * 서버가 해당 인스턴스의 전체 파일 트리를 반환함.
	 */
	@MessageMapping("/instances/{instanceId}/tree/init")
	public void sendInitialTree(@DestinationVariable Long instanceId) {
		log.info("[WebSocket] tree:init 요청 → instanceId={}", instanceId);

		List<TreeNodeDto> tree = treeService.buildTree(instanceId);
		WebSocketMessage msg = new WebSocketMessage("tree:init", tree);

		try {
			String json = new ObjectMapper().writeValueAsString(msg); // 💡 여기
			log.info("📤 보내는 메시지: {}", json);
		} catch (Exception e) {
			log.error("❌ 메시지 직렬화 실패", e);
		}

		messagingTemplate.convertAndSend(
			"/topic/instances/" + instanceId + "/tree",
			msg
		);
	}
}
