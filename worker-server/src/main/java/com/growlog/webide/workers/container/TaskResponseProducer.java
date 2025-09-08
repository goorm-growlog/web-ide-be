package com.growlog.webide.workers.container;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.growlog.webide.workers.container.dto.ContainerAcquireFailureResponse;
import com.growlog.webide.workers.container.dto.ContainerAcquireSuccessResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TaskResponseProducer {

	private final RabbitTemplate rabbitTemplate;

	private final String exchangeName;
	private final String acquireSuccessRoutingKey;
	private final String acquireFailureRoutingKey;
	private final String cleanupSuccessRoutingKey;

	public TaskResponseProducer(RabbitTemplate rabbitTemplate,
		@Value("${container-response.rabbitmq.exchange.name}") String exchangeName,
		@Value("${container-response.rabbitmq.routing.key.acquire-success}") String acquireSuccessRoutingKey,
		@Value("${container-response.rabbitmq.routing.key.acquire-failure}") String acquireFailureRoutingKey,
		@Value("${container-response.rabbitmq.routing.key.cleanup-success}") String cleanupSuccessRoutingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchangeName = exchangeName;
		this.acquireSuccessRoutingKey = acquireSuccessRoutingKey;
		this.acquireFailureRoutingKey = acquireFailureRoutingKey;
		this.cleanupSuccessRoutingKey = cleanupSuccessRoutingKey;
	}

	public void sendAcquireSuccess(Long sessionId, Long projectId, String containerId) {
		ContainerAcquireSuccessResponse response = new ContainerAcquireSuccessResponse(sessionId, projectId,
			containerId);
		log.info("Sending acquire success response: {}", response);
		rabbitTemplate.convertAndSend(exchangeName, acquireSuccessRoutingKey, response);
	}

	public void sendAcquireFailure(Long sessionId, String reason) {
		ContainerAcquireFailureResponse response = new ContainerAcquireFailureResponse(sessionId, reason);
		log.info("Sending acquire failure response: {}", response);
		rabbitTemplate.convertAndSend(exchangeName, acquireFailureRoutingKey, response);
	}

	public void sendCleanupSuccess(Long sessionId) {
		log.info("Sending cleanup success response for project: {}", sessionId);
		rabbitTemplate.convertAndSend(exchangeName, cleanupSuccessRoutingKey, sessionId);
	}

}
