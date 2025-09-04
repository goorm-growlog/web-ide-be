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
	private final String successRoutingKey;
	private final String failureRoutingKey;

	public TaskResponseProducer(RabbitTemplate rabbitTemplate,
		@Value("${container-response.rabbitmq.exchange.name}") String exchangeName,
		@Value("${container-response.rabbitmq.routing.key.acquire-success}") String successRoutingKey,
		@Value("${container-response.rabbitmq.routing.key.acquire-failure}") String failureRoutingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchangeName = exchangeName;
		this.successRoutingKey = successRoutingKey;
		this.failureRoutingKey = failureRoutingKey;
	}

	public void sendAcquireSuccess(Long sessionId, Long projectId, String containerId) {
		ContainerAcquireSuccessResponse response = new ContainerAcquireSuccessResponse(sessionId, projectId,
			containerId);
		log.info("Sending acquire success response: {}", response);
		rabbitTemplate.convertAndSend(exchangeName, successRoutingKey, response);
	}

	public void sendAcquireFailure(Long sessionId, String reason) {
		ContainerAcquireFailureResponse response = new ContainerAcquireFailureResponse(sessionId, reason);
		log.info("Sending acquire failure response: {}", response);
		rabbitTemplate.convertAndSend(exchangeName, failureRoutingKey, response);
	}
}
