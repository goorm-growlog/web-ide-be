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
	public void WebSocketDisconnectListener(SessionDisconnectEvent event) {
		final StompHeaderAccessor accessor = MessageHeaderAccessor
			.getAccessor(event.getMessage(), StompHeaderAccessor.class);

		final Long userId = Long.parseLong(accessor.getSessionAttributes().get("AUTHENTICATED").toString());
		final Long projectId = Long.parseLong(accessor.getSessionAttributes().get("projectId").toString());
		final ChattingResponseDto response = chatService.leave(projectId, userId);

		final String destination = "/topic/projects/" + projectId + "/chat";

		template.convertAndSend(destination, response);
	}

}
