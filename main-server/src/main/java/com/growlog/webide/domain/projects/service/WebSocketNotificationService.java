package com.growlog.webide.domain.projects.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.projects.dto.WebSocketMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WebSocketNotificationService {

	private final SimpMessagingTemplate simpMessagingTemplate;

	public WebSocketNotificationService(SimpMessagingTemplate simpMessagingTemplate) {
		this.simpMessagingTemplate = simpMessagingTemplate;
	}

	/*
	터미널 관련 웹소켓 세션 강제 종료 메시지 발행
	 */
	public void sendLogSessionTerminationMessage(String targetUserId, String message) {
		final String destination = "/queue/termination";
		log.info("send Log Session Termination Message: {}", destination);
		final String userId = String.valueOf(targetUserId);

		WebSocketMessage sessionTerminateMessage = new WebSocketMessage("LOG_SESSION_TERMINATE", message);
		simpMessagingTemplate.convertAndSendToUser(userId, destination, sessionTerminateMessage);
	}

	/*
	프로젝트 웹소켓 세션 강제 종료 메시지 발행
	 */
	public void sendSessionTerminationMessage(Long targetUserId, String message) {
		final String destination = "/queue/termination";
		log.info("send Project Session Termination Message: {}", destination);
		final String userId = String.valueOf(targetUserId);

		WebSocketMessage sessionTerminateMessage = new WebSocketMessage("SESSION_TERMINATE", message);
		simpMessagingTemplate.convertAndSendToUser(userId, destination, sessionTerminateMessage);
	}

	/*
	프로젝트가 삭제됨에 따른 세션 강제 종료 메시지 발행
	 */
	public void sendProjectDeletedMessage(Long targetUserId, String message) {
		final String destination = "/queue/termination";
		log.info("send Project Deleted Message: {}", destination);
		final String userId = String.valueOf(targetUserId);

		WebSocketMessage projectDeletedMessage = new WebSocketMessage("PROJECT_DELETED", message);
		simpMessagingTemplate.convertAndSendToUser(userId, destination, projectDeletedMessage);
	}

}
