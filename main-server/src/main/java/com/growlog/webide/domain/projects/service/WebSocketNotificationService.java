package com.growlog.webide.domain.projects.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WebSocketNotificationService {

	private final SimpMessagingTemplate simpMessagingTemplate;

	public WebSocketNotificationService(SimpMessagingTemplate simpMessagingTemplate) {
		this.simpMessagingTemplate = simpMessagingTemplate;
	}

	public void sendSessionTerminationMessage(Long targetUserId, String message) {
		final String destination = "/queue/termination/" + targetUserId;
		log.info("sendSessionTerminationMessage: {}", destination);
		simpMessagingTemplate.convertAndSend(destination, message);
	}

}
