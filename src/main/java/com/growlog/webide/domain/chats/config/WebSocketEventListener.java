package com.growlog.webide.domain.chats.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.service.ChatService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

	private final SimpMessagingTemplate template;
	private final ChatService chatService;

	@Transactional(readOnly = true)
	@EventListener
	public void webSocketDisconnectListener(SessionDisconnectEvent event) {
		final StompHeaderAccessor accessor = MessageHeaderAccessor
			.getAccessor(event.getMessage(), StompHeaderAccessor.class);

		if (accessor == null || accessor.getSessionAttributes() == null) {
			return; // 세션 종료만 됐고 handshake 전에 끊긴 경우
		}

		Object userIdObj = accessor.getSessionAttributes().get("userId");
		Object projectIdObj = accessor.getSessionAttributes().get("projectId");

		if (userIdObj == null || projectIdObj == null) {
			return; // handshake는 되었지만 제대로 저장 안된 경우
		}

		Long userId = Long.parseLong(userIdObj.toString());
		Long projectId = Long.parseLong(projectIdObj.toString());

		final ChattingResponseDto response = chatService.leave(projectId, userId);

		final String destination = "/topic/projects/" + projectId + "/chat";

		template.convertAndSend(destination, response);
	}

}
