package com.growlog.webide.domain.files.controller;

import com.growlog.webide.domain.files.dto.tree.TreeNodeDto;
import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
import com.growlog.webide.domain.files.service.TreeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

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

		messagingTemplate.convertAndSend(
			"/topic/instances/" + instanceId + "/tree",
			msg
		);
	}
}
