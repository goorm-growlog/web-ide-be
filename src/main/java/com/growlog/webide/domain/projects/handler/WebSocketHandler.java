package com.growlog.webide.domain.projects.handler;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.growlog.webide.domain.projects.service.WorkspaceManagerService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

	private final WorkspaceManagerService workspaceManagerService;

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

		String containerId = (String)session.getAttributes().get("containerId");

		// if (containerId != null) {
		// 	workspaceManagerService.closeProjectSession(containerId,);
		// }
	}

}
