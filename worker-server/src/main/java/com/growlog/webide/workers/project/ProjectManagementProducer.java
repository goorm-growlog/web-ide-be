package com.growlog.webide.workers.project;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProjectManagementProducer {

	private final RabbitTemplate rabbitTemplate;

	private final String exchangeName;
	private final String successRoutingKey;

	public ProjectManagementProducer(RabbitTemplate rabbitTemplate,
		@Value("${project-response.rabbitmq.exchange.name}") String exchangeName,
		@Value("${project-response.rabbitmq.routing.key.delete-success}") String successRoutingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchangeName = exchangeName;
		this.successRoutingKey = successRoutingKey;
	}

	public void sendDeleteSuccess(Long projectId) {
		log.info("Sending delete success response for project: {}", projectId);
		rabbitTemplate.convertAndSend(exchangeName, successRoutingKey, projectId);
	}

}
