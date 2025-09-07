package com.growlog.webide.workers.container;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.growlog.webide.workers.container.dto.ContainerAcquireRequest;
import com.growlog.webide.workers.container.dto.ContainerCleanupRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ContainerTaskConsumer {

	private final ContainerManager containerManager;
	private final TaskResponseProducer taskResponseProducer;

	public ContainerTaskConsumer(ContainerManager containerManager,
		TaskResponseProducer taskResponseProducer) {
		this.containerManager = containerManager;
		this.taskResponseProducer = taskResponseProducer;
	}

	@RabbitListener(queues = "${container-acquire.rabbitmq.queue.name}") // 컨테이너 할당 요청 큐
	public void handleAcquireRequest(ContainerAcquireRequest request) {
		log.info("Received container acquire request: {}", request);
		final Long sessionId = request.sessionId();
		final Long projectId = request.projectId();

		try {
			String containerId = containerManager.getOrCreateContainerForSession(sessionId, projectId);
			log.info("Acquired container '{}' for project '{}'", containerId, projectId);
			taskResponseProducer.sendAcquireSuccess(sessionId, projectId, containerId);
		} catch (Exception e) {
			log.error("Failed to acquire container for project {}", projectId, e);
			taskResponseProducer.sendAcquireFailure(sessionId,
				"Failed to create or start a container for the project.");
		}
	}

	@RabbitListener(queues = "${container-cleanup.rabbitmq.queue.name}") // 컨테이너 정리 요청 큐
	public void handleCleanupRequest(ContainerCleanupRequest request) {
		log.info("Received container cleanup request: {}", request);
		try {
			containerManager.cleanupContainerForSession(request.sessionId());
			taskResponseProducer.sendCleanupSuccess(request.sessionId());
		} catch (Exception e) {
			log.error("Failed to process cleanup request", e);
			throw new RuntimeException("Failed to process cleanup request", e);
		}
	}
}
