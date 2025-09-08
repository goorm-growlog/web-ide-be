package com.growlog.webide.domain.projects.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.growlog.webide.domain.terminal.service.ActiveInstanceService;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProjectSessionEventListener {

	private final ActiveInstanceService activeInstanceService;
	private final ActiveSessionService activeSessionService;
	private final ProjectRepository projectRepository;
	private final ActiveSessionRepository activeSessionRepository;

	public ProjectSessionEventListener(ActiveInstanceService activeInstanceService,
		ActiveSessionService activeSessionService, ProjectRepository projectRepository,
		ActiveSessionRepository activeSessionRepository) {
		this.activeInstanceService = activeInstanceService;
		this.activeSessionService = activeSessionService;
		this.projectRepository = projectRepository;
		this.activeSessionRepository = activeSessionRepository;
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

			projectOpt.ifPresent(project -> {
				final Long remainingSessions = activeSessionRepository.countAllByProjectId((projectId));

				if (remainingSessions == 1) {
					log.info("Last user disconnected from project {}. Inactivating project.", projectId);
					project.inactivate();
				} else {
					log.info("{} users still active in project {}. Project remains active.", remainingSessions - 1,
						projectId);
				}

				activeSessionService.cleanupSession(userId, projectId);
				activeInstanceService.cleanupInstance(userId, projectId);
			});
		});
	}

}
