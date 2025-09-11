package com.growlog.webide.domain.projects.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.growlog.webide.domain.projects.dto.ContainerAcquireRequest;
import com.growlog.webide.domain.projects.dto.ContainerCleanupRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ContainerTaskProducer {
	private final RabbitTemplate rabbitTemplate;

	private final String acquireExchangeName;
	private final String acquireRoutingKey;
	private final String cleanupExchangeName;
	private final String cleanupRoutingKey;

	public ContainerTaskProducer(RabbitTemplate rabbitTemplate,
		@Value("${container-acquire.rabbitmq.exchange.name}") String acquireExchangeName,
		@Value("${container-acquire.rabbitmq.routing.key}") String acquireRoutingKey,
		@Value("${container-cleanup.rabbitmq.exchange.name}") String cleanupExchangeName,
		@Value("${container-cleanup.rabbitmq.routing.key}") String cleanupRoutingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.acquireExchangeName = acquireExchangeName;
		this.acquireRoutingKey = acquireRoutingKey;
		this.cleanupExchangeName = cleanupExchangeName;
		this.cleanupRoutingKey = cleanupRoutingKey;
	}

	/**
	 * 워커 서버에 컨테이너 할당을 요청하는 메시지를 발행
	 */
	public void requestContainerAcquire(Long sessionId, Long projectId) {
		ContainerAcquireRequest request = new ContainerAcquireRequest(sessionId, projectId);
		log.info("Sending container acquire request object: {}", request);
		rabbitTemplate.convertAndSend(acquireExchangeName, acquireRoutingKey, request);
	}

	/**
	 * 워커 서버에 컨테이너 정리를 요청하는 메시지를 발행
	 */
	public void requestContainerCleanup(Long sessionId, Long projectId, String containerId) {
		ContainerCleanupRequest request = new ContainerCleanupRequest(sessionId, projectId, containerId);
		log.info("Sending container cleanup request object: {}", request);
		rabbitTemplate.convertAndSend(cleanupExchangeName, cleanupRoutingKey, request);
	}
}
