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
import com.growlog.webide.domain.terminal.service.ActiveInstanceService;
import com.growlog.webide.domain.terminal.service.TerminalService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProjectSessionEventListener {

	private final ActiveInstanceService activeInstanceService;
	private final ActiveSessionService activeSessionService;
	private final ProjectRepository projectRepository;
	private final ActiveSessionRepository activeSessionRepository;
	private final TerminalService terminalService;
	private final WebSocketNotificationService webSocketNotificationService;
	private final ActiveInstanceRepository activeInstanceRepository;

	public ProjectSessionEventListener(ActiveInstanceService activeInstanceService,
		ActiveSessionService activeSessionService,
		ProjectRepository projectRepository,
		ActiveSessionRepository activeSessionRepository,
		TerminalService terminalService,
		WebSocketNotificationService webSocketNotificationService, ActiveInstanceRepository activeInstanceRepository) {
		this.activeInstanceService = activeInstanceService;
		this.activeSessionService = activeSessionService;
		this.projectRepository = projectRepository;
		this.activeSessionRepository = activeSessionRepository;
		this.terminalService = terminalService;
		this.webSocketNotificationService = webSocketNotificationService;
		this.activeInstanceRepository = activeInstanceRepository;
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

			closeRelatedSessions(userId, projectId);

			if (projectOpt.isPresent()) {
				projectOpt.ifPresent(project -> {
					final Long remainingSessions = activeSessionRepository.countAllByProjectId((projectId));

					if (remainingSessions <= 1) {
						log.info("Last session disconnected from project {}. Inactivating project.", projectId);
						project.inactivate();
					} else {
						log.info("{} sessions still active in project {}. Project remains active.",
							remainingSessions - 1, projectId);
						activeSessionService.cleanupSession(userId, projectId);
					}

				});
			}

			final String sessionId = event.getSessionId();
			log.info("WebSocket session {} disconnected. Triggering full cleanup process.", sessionId);
			// PTY 세션 정리+컨테이너 정리를 위한 메서드를 호출.
			terminalService.handleSessionDisconnect(sessionId);
		});

	}

	private void closeRelatedSessions(Long userId, Long projectId) {
		activeSessionRepository.findAllByUser_UserIdAndProject_Id(userId, projectId).forEach(activeSession -> {
			final Long targetUserId = activeSession.getUser().getUserId();
			final String message = "Connection terminated by the project owner";
			log.info("inactivateProject: {}", message);
			webSocketNotificationService.sendSessionTerminationMessage(targetUserId, message);
		});
		activeInstanceRepository.findAllByUser_UserIdAndProject_Id(userId, projectId).forEach(activeInstance -> {
			final Long targetUserId = activeInstance.getUser().getUserId();
			final String message = "Connection terminated by the project owner";
			log.info("inactivateProject: {}", message);
			webSocketNotificationService.sendSessionTerminationMessage(targetUserId, message);
		});
	}

}
