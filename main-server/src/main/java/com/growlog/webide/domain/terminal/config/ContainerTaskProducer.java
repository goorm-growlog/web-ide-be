package com.growlog.webide.domain.terminal.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ContainerTaskProducer {

	private final RabbitTemplate rabbitTemplate;
	private final String containerLifecycleExchangeName;
	private final String containerDeleteKey;

	public ContainerTaskProducer(RabbitTemplate rabbitTemplate,
		@Value("${container-lifecycle.rabbitmq.exchange.name}") String containerLifecycleExchangeName,
		@Value("${container-lifecycle.rabbitmq.request.routing-key}") String containerDeleteKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.containerLifecycleExchangeName = containerLifecycleExchangeName;
		this.containerDeleteKey = containerDeleteKey;
	}

	/**
	 * 워커 서버에 컨테이너 정리를 요청하는 메시지를 발행
	 */
	public void requestContainerDeletion(String containerId) {
		log.info("Sending container deletion request... containerId: {}", containerId);
		rabbitTemplate.convertAndSend(containerLifecycleExchangeName, containerDeleteKey, containerId);
	}

}
