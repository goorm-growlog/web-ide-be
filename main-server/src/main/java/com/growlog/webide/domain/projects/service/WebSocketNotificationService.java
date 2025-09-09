package com.growlog.webide.domain.projects.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.projects.dto.WebSocketMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WebSocketNotificationService {

	private final SimpMessagingTemplate simpMessagingTemplate;
	private final SimpUserRegistry simpUserRegistry;

	public WebSocketNotificationService(SimpMessagingTemplate simpMessagingTemplate,
		SimpUserRegistry simpUserRegistry) {
		this.simpMessagingTemplate = simpMessagingTemplate;
		this.simpUserRegistry = simpUserRegistry;
	}

	public void sendLogSessionTerminationMessage(String targetUserId, String message) {
		final String destination = "/queue/termination";
		log.info("send Log Session Termination Message: {}", destination);
		final String userId = String.valueOf(targetUserId);

		WebSocketMessage sessionTerminateMessage = new WebSocketMessage("LOG_SESSION_TERMINATE", message);
		simpMessagingTemplate.convertAndSendToUser(userId, destination, sessionTerminateMessage);
	}

	public void sendSessionTerminationMessage(Long targetUserId, String message) {
		final String destination = "/queue/termination";
		log.info("send Project Session Termination Message: {}", destination);
		final String userId = String.valueOf(targetUserId);

		WebSocketMessage sessionTerminateMessage = new WebSocketMessage("SESSION_TERMINATE", message);
		simpMessagingTemplate.convertAndSendToUser(userId, destination, sessionTerminateMessage);
	}

	public void sendProjectDeletedMessage(Long targetUserId, String message) {
		final String destination = "/queue/termination";
		log.info("send Project Deleted Message: {}", destination);
		final String userId = String.valueOf(targetUserId);

		WebSocketMessage projectDeletedMessage = new WebSocketMessage("PROJECT_DELETED", message);
		simpMessagingTemplate.convertAndSendToUser(userId, destination, projectDeletedMessage);
	}

}
