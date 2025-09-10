package com.growlog.webide.domain.projects.config;

import java.util.Optional;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.growlog.webide.domain.chats.config.WebSocketEventListener;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ActiveSessionRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.projects.service.ActiveSessionService;
import com.growlog.webide.domain.projects.service.WebSocketNotificationService;
import com.growlog.webide.domain.terminal.repository.ActiveInstanceRepository;
import com.growlog.webide.domain.terminal.service.TerminalService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProjectSessionEventListener {

	private final ActiveSessionService activeSessionService;
	private final ProjectRepository projectRepository;
	private final ActiveSessionRepository activeSessionRepository;
	private final TerminalService terminalService;
	private final WebSocketNotificationService webSocketNotificationService;
	private final ActiveInstanceRepository activeInstanceRepository;
	private final WebSocketEventListener webSocketEventListener;

	public ProjectSessionEventListener(ActiveSessionService activeSessionService,
		ProjectRepository projectRepository,
		ActiveSessionRepository activeSessionRepository,
		TerminalService terminalService,
		WebSocketNotificationService webSocketNotificationService,
		ActiveInstanceRepository activeInstanceRepository,
		WebSocketEventListener webSocketEventListener) {
		this.activeSessionService = activeSessionService;
		this.projectRepository = projectRepository;
		this.activeSessionRepository = activeSessionRepository;
		this.terminalService = terminalService;
		this.webSocketNotificationService = webSocketNotificationService;
		this.activeInstanceRepository = activeInstanceRepository;
		this.webSocketEventListener = webSocketEventListener;
	}

	@Transactional
	@EventListener
	public void handleSessionDisconnect(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);

		WebSocketEventListener.extractSessionInfo(accessor).ifPresent(sessionInfo -> {
			log.info("Websocket session disconnected: {}", sessionInfo);

			final Long userId = sessionInfo.userId();
			final Long projectId = sessionInfo.projectId();

			final Optional<Project> projectOpt = projectRepository.findWithLockById(projectId);

			closeRelatedInstances(userId, projectId);

			boolean isLogSubscription = webSocketEventListener.extractLogSubscription(accessor);

			if (!isLogSubscription) {
				closeRelatedSessions(userId, projectId);
				handleRemainingSessions(projectOpt, projectId, userId);
			}

			final String sessionId = event.getSessionId();
			log.info("WebSocket session {} disconnected. Triggering full cleanup process.", sessionId);
			// PTY 세션 정리+컨테이너 정리를 위한 메서드를 호출.
			terminalService.handleSessionDisconnect(sessionId);
		});

	}

	// 방금 삭제한 세션에 대한 DB를 정리하고 남은 세션이 없을 경우 프로젝트 비활성화
	private void handleRemainingSessions(Optional<Project> projectOpt, Long projectId, Long userId) {
		if (projectOpt.isEmpty()) {
			return;
		}

		activeSessionService.cleanupSession(userId, projectId);

		final Long remainingSessions = activeSessionRepository.countAllByProjectId(projectId);
		Project project = projectOpt.get();

		if (remainingSessions == 0) {
			log.info("Last session disconnected from project {}. Inactivating project.", projectId);
			project.inactivate();
		} else {
			log.info("{} sessions still active in project {}. Project remains active.",
				remainingSessions, projectId);
		}
	}

	// userId, projectId를 통해 관련된 세션 정리
	private void closeRelatedSessions(Long userId, Long projectId) {
		activeSessionRepository.findAllByUser_UserIdAndProject_Id(userId, projectId).forEach(activeSession -> {
			final Long targetUserId = activeSession.getUser().getUserId();
			final String message = "Connection terminated by the project owner";
			log.info("inactivateProject: {}", message);
			webSocketNotificationService.sendSessionTerminationMessage(targetUserId, message);
		});
	}

	// userId, projectId를 통해 인스턴스(컨테이너) 정리
	private void closeRelatedInstances(Long userId, Long projectId) {
		activeInstanceRepository.findAllByUser_UserIdAndProject_Id(userId, projectId).forEach(activeInstance -> {
			final Long targetUserId = activeInstance.getUser().getUserId();
			final String message = "Connection terminated by the project owner";
			log.info("inactivateProject: {}", message);
			webSocketNotificationService.sendLogSessionTerminationMessage(targetUserId, message);
		});
	}

}
