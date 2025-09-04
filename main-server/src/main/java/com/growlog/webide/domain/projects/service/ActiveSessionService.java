package com.growlog.webide.domain.projects.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.growlog.webide.domain.projects.config.ContainerTaskProducer;
import com.growlog.webide.domain.projects.dto.ContainerAcquireFailureResponse;
import com.growlog.webide.domain.projects.dto.ContainerAcquireSuccessResponse;
import com.growlog.webide.domain.projects.entity.ActiveSession;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ActiveSessionRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ActiveSessionService {

	private final ActiveSessionRepository activeSessionRepository;
	private final ContainerTaskProducer containerTaskProducer;

	public ActiveSessionService(ActiveSessionRepository activeSessionRepository,
		ContainerTaskProducer containerTaskProducer) {
		this.activeSessionRepository = activeSessionRepository;
		this.containerTaskProducer = containerTaskProducer;
	}

	/**
	 * 사용자와 프로젝트 ID를 기반으로 활성 세션을 정리합니다.
	 */
	@Transactional
	public void cleanupSession(Long userId, Long projectId) {
		Optional<ActiveSession> sessionOpt = activeSessionRepository.findByUser_UserIdAndProject_Id(
			userId, projectId);

		if (sessionOpt.isEmpty()) {
			log.warn("No active session found to clean up for userId: {} and projectId: {}", userId, projectId);
			return;
		}

		ActiveSession activeSession = sessionOpt.get();
		log.info("Found active session to clean up: {}", activeSession.getId());

		requestCleanup(activeSession);

		activeSessionRepository.delete(activeSession);
		log.info("Cleaned up session for userId: {}", userId);
	}

	/**
	 * 워커 서버에서 받은 컨테이너 할당 성공 응답을 처리합니다.
	 */
	@Transactional
	public void handleAcquireSuccessResponse(ContainerAcquireSuccessResponse response) {
		// sessionId를 통해 ActiveSession를 찾아 containerId를 업데이트
		final Optional<ActiveSession> sessionOpt = activeSessionRepository.findById(response.sessionId());

		sessionOpt.ifPresentOrElse(
			session -> { // 세션이 존재할 때
				final Project project = session.getProject();
				final String projectId = project.getId().toString();

				session.setContainerId(response.containerId());
				if (StringUtils.hasLength(projectId)) {
					project.activate();
				}

				activeSessionRepository.save(session);
				log.info("Successfully linked container {} to session {}", response.containerId(),
					response.sessionId());
			},
			() -> { // 세션이 존재하지 않을 때
				log.warn("Orphan container detected! Session {} not found for container {}. Requesting cleanup.",
					response.sessionId(), response.containerId());

				// worker-server에 즉시 정리 요청을 보냄
				containerTaskProducer.requestContainerCleanup(response.sessionId(), response.projectId(),
					response.containerId());
			}
		);
	}

	/**
	 * 워커 서버에서 받은 컨테이너 할당 실패 응답을 처리합니다.
	 */
	@Transactional
	public void handleAcquireFailureResponse(ContainerAcquireFailureResponse response) {
		log.error("Received container acquire failure for session {}: {}", response.sessionId(), response.reason());

		// 실패했으므로 관련 ActiveInstance가 있다면 삭제
		activeSessionRepository.deleteById(response.sessionId());
	}

	private void requestCleanup(ActiveSession activeSession) {
		if (StringUtils.hasText(activeSession.getContainerId())) {
			containerTaskProducer.requestContainerCleanup(activeSession.getId(),
				activeSession.getProject().getId(),
				activeSession.getContainerId());
		}
	}

}
