package com.growlog.webide.domain.files.controller;

import java.util.Collections;
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
import com.growlog.webide.domain.files.service.TreeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TreeWebSocketController {

	private final TreeService treeService;
	private final SimpMessagingTemplate messagingTemplate;

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

		// 🌳 트리 구성
		TreeNodeDto rootNode = treeService.getInitialTree(projectId);
		List<TreeNodeDto> payload = Collections.singletonList(rootNode);

		WebSocketMessage msg = new WebSocketMessage("tree:init", payload);
		messagingTemplate.convertAndSend(
			"/topic/projects/" + projectId + "/tree",
			msg
		);
	}

}
