package com.growlog.webide.domain.projects.config;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.growlog.webide.domain.projects.dto.ContainerAcquireFailureResponse;
import com.growlog.webide.domain.projects.dto.ContainerAcquireSuccessResponse;
import com.growlog.webide.domain.projects.service.ActiveSessionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ContainerResponseConsumer {

	private final ActiveSessionService activeSessionService;

	public ContainerResponseConsumer(ActiveSessionService activeSessionService) {
		this.activeSessionService = activeSessionService;
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
}
