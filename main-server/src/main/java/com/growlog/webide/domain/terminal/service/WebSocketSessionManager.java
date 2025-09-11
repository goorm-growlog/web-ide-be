package com.growlog.webide.domain.terminal.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

	private final TerminalService terminalService;

	@EventListener
	public void onDisconnectEvent(SessionDisconnectEvent event) {
		String sessionId = event.getSessionId();
		log.info("WebSocket session {} disconnected. Triggering full cleanup process.", sessionId);
		// PTY 세션 정리+컨테이너 정리를 위한 메서드를 호출.
		terminalService.handleSessionDisconnect(sessionId);
	}
}
