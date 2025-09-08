package com.growlog.webide.domain.chats.config;

import java.util.Optional;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

	private final SimpMessagingTemplate template;
	private final ChatService chatService;

	public static Optional<SessionInfo> extractSessionInfo(StompHeaderAccessor accessor) {
		if (accessor == null || accessor.getSessionAttributes() == null) {
			log.warn("Accessor or session attributes are null. Could be a connection closed before handshake.");
			return Optional.empty();
		}

		try {
			Object userIdObj = accessor.getSessionAttributes().get("userId");
			Object projectIdObj = accessor.getSessionAttributes().get("projectId");

			log.info("Retrieved from session -> userId: {}, projectId: {}", userIdObj, projectIdObj);

			if (userIdObj == null || projectIdObj == null) {
				log.warn("userId or projectId is missing from session attributes.");
				return Optional.empty();
			}

			Long userId = Long.parseLong(userIdObj.toString());
			Long projectId = Long.parseLong(projectIdObj.toString());
			return Optional.of(new SessionInfo(userId, projectId));
		} catch (Exception e) {
			log.warn("Failed to extract session info from session attributes.", e);
			return Optional.empty();
		}
	}

	@Transactional(readOnly = true)
	@EventListener
	public void webSocketDisconnectListener(SessionDisconnectEvent event) {
		log.info("WebSocket session disconnected: {}", event.getSessionId());

		final StompHeaderAccessor accessor = MessageHeaderAccessor
			.getAccessor(event.getMessage(), StompHeaderAccessor.class);

		extractSessionInfo(accessor).ifPresent(sessionInfo -> {
			log.info("Processing leave action for userId: {} in projectId: {}", sessionInfo.userId(),
				sessionInfo.projectId());

			final ChattingResponseDto response = chatService.leave(sessionInfo.projectId(), sessionInfo.userId());

			final String destination = "/topic/projects/" + sessionInfo.projectId() + "/chat";

			log.info("Sending leave message to destination: {}. Payload: {}", destination, response);

			template.convertAndSend(destination, response);
		});
	}

	public record SessionInfo(Long userId, Long projectId) {
	}

}
