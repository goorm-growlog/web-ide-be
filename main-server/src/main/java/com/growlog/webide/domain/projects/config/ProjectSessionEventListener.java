package com.growlog.webide.domain.projects.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.growlog.webide.domain.chats.config.WebSocketEventListener;
import com.growlog.webide.domain.projects.service.ActiveSessionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProjectSessionEventListener {

	private final ActiveSessionService activeSessionService;

	public ProjectSessionEventListener(ActiveSessionService activeSessionService) {
		this.activeSessionService = activeSessionService;
	}

	@EventListener
	public void handleSessionDisconnect(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);

		WebSocketEventListener.extractSessionInfo(accessor).ifPresent(sessionInfo -> {
			log.info("Websocket session disconnected: {}", sessionInfo);
			activeSessionService.cleanupSession(sessionInfo.userId(), sessionInfo.projectId());
		});
	}

}
