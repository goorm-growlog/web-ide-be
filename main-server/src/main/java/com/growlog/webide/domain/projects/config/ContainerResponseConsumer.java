package com.growlog.webide.domain.projects.config;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.growlog.webide.domain.projects.dto.ContainerAcquireFailureResponse;
import com.growlog.webide.domain.projects.dto.ContainerAcquireSuccessResponse;
import com.growlog.webide.domain.projects.repository.ActiveSessionRepository;
import com.growlog.webide.domain.projects.service.ActiveSessionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ContainerResponseConsumer {

	private final ActiveSessionService activeSessionService;
	private final ActiveSessionRepository activeSessionRepository;

	public ContainerResponseConsumer(ActiveSessionService activeSessionService,
		ActiveSessionRepository activeSessionRepository) {
		this.activeSessionService = activeSessionService;
		this.activeSessionRepository = activeSessionRepository;
	}

	@RabbitListener(queues = "${container-response.rabbitmq.queue.name.acquire-success}") // 응답 큐를 리스닝
	public void handleAcquireSuccess(ContainerAcquireSuccessResponse response) {
		log.info("Received container acquire success for session: {}", response.sessionId());

		activeSessionService.handleAcquireSuccessResponse(response);
	}

	@RabbitListener(queues = "${container-response.rabbitmq.queue.name.acquire-failure}")
	public void handleAcquireFailure(ContainerAcquireFailureResponse response) {
		activeSessionService.handleAcquireFailureResponse(response);
	}

	@RabbitListener(queues = "${container-response.rabbitmq.queue.name.cleanup-success}")
	public void handleCleanupSuccess(Long sessionId) {
		log.info("Received container cleanup success for session: {}", sessionId);
		activeSessionRepository.deleteById(sessionId);
	}

}
