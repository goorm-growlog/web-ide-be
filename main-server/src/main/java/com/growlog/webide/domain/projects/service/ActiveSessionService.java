package com.growlog.webide.domain.projects.service;

import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.projects.entity.ActiveSession;
import com.growlog.webide.domain.projects.repository.ActiveSessionRepository;
import com.growlog.webide.domain.terminal.config.ContainerTaskProducer;
import com.growlog.webide.domain.terminal.repository.ActiveInstanceRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ActiveSessionService {

	private final ActiveSessionRepository activeSessionRepository;

	public ActiveSessionService(ActiveSessionRepository activeSessionRepository) {
		this.activeSessionRepository = activeSessionRepository;
	}

	/**
	 * 사용자와 프로젝트 ID를 기반으로 활성 세션을 정리합니다.
	 */
	@Transactional
	public void cleanupSession(Long userId, Long projectId) {
		List<ActiveSession> activeSessions = activeSessionRepository.findAllByUser_UserIdAndProject_Id(
			userId, projectId);

		if (activeSessions.isEmpty()) {
			log.warn("No active session found to clean up for userId: {} and projectId: {}", userId, projectId);
			return;
		}

		for (ActiveSession session : activeSessions) {
			final Long sessionId = session.getId();
			log.info("Found active session to clean up: {}", sessionId);
			activeSessionRepository.deleteById(sessionId);
		}

		log.info("Cleaned up session for userId: {}", userId);
	}

}
