package com.growlog.webide.domain.projects.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProjectManagementProducer {

	private final RabbitTemplate rabbitTemplate;

	private final String projectDeleteExchangeName;
	private final String projectDeleteRoutingKey;

	private final String projectDbCleanupExchangeName;
	private final String projectDbCleanupRoutingKey;

	public ProjectManagementProducer(RabbitTemplate rabbitTemplate,
		@Value("${project-delete.rabbitmq.exchange.name}") String projectDeleteExchangeName,
		@Value("${project-delete.rabbitmq.routing.key}") String projectDeleteRoutingKey,
		@Value("${project-db-cleanup.rabbitmq.exchange.name}") String projectDbCleanupExchangeName,
		@Value("${project-db-cleanup.rabbitmq.routing.key}") String projectDbCleanupRoutingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.projectDeleteExchangeName = projectDeleteExchangeName;
		this.projectDeleteRoutingKey = projectDeleteRoutingKey;
		this.projectDbCleanupExchangeName = projectDbCleanupExchangeName;
		this.projectDbCleanupRoutingKey = projectDbCleanupRoutingKey;
	}

	/*
	워커 서버에 해당 프로젝트 관련 EFS 폴더 삭제를 요청하는 메시지를 발행
	 */
	public void requestDeleteProject(Long projectId) {
		log.info("Sending project delete request object: {}", projectId);
		rabbitTemplate.convertAndSend(projectDeleteExchangeName, projectDeleteRoutingKey, projectId);
	}

	/*
	메인 서버에 해당 프로젝트 관련 DB 정리를 요청하는 메시지를 발행
	 */
	public void requestCleanupProjectDb(Long projectId) {
		log.info("Sending project cleanup request object: {}", projectId);
		rabbitTemplate.convertAndSend(projectDbCleanupExchangeName, projectDbCleanupRoutingKey, projectId);
	}

}
