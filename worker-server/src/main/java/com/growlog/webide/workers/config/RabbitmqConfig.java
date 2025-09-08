package com.growlog.webide.workers.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitmqConfig {

	// 코드 실행 큐 관련 설정 값
	@Value("${code-execution.rabbitmq.exchange.name}")
	private String codeExecutionExchangeName;
	@Value("${code-execution.rabbitmq.queue.name}")
	private String codeExecutionQueueName;
	@Value("${code-execution.rabbitmq.routing.key}")
	private String codeExecutionRoutingKey;

	// RPC 큐 관련 설정 값
	@Value("${rpc.rabbitmq.exchange.name}")
	private String rpcExchangeName;
	@Value("${rpc.rabbitmq.queue.name}")
	private String rpcQueueName;
	@Value("${rpc.rabbitmq.routing.key}")
	private String rpcRoutingKey;

	// [추가] 컨테이너 삭제 큐 관련 설정 값
	@Value("${container-lifecycle.rabbitmq.exchange.name}")
	private String containerLifecycleExchangeName;
	@Value("${container-lifecycle.rabbitmq.request.queue-name}")
	private String containerDeleteQueueName;
	@Value("${container-lifecycle.rabbitmq.request.routing-key}")
	private String containerDeleteRoutingKey;

	// [신규] PTY 세션 관련 설정 값
	@Value("${pty-session.rabbitmq.start.exchange}")
	private String ptyStartExchangeName;
	@Value("${pty-session.rabbitmq.start.queue}")
	private String ptyStartQueueName;
	@Value("${pty-session.rabbitmq.start.routing-key}")
	private String ptyStartRoutingKey;
	@Value("${pty-session.rabbitmq.command.exchange}")
	private String ptyCommandExchangeName;
	@Value("${pty-session.rabbitmq.command.queue}")
	private String ptyCommandQueueName;
	@Value("${pty-session.rabbitmq.command.routing-key}")
	private String ptyCommandRoutingKey;

	// 컨테이너 할당 큐 관련 설정 값
	@Value("${container-acquire.rabbitmq.exchange.name}")
	private String containerAcquireExchangeName;
	@Value("${container-acquire.rabbitmq.queue.name}")
	private String containerAcquireQueueName;
	@Value("${container-acquire.rabbitmq.routing.key}")
	private String containerAcquireRoutingKey;

	// 컨테이너 할당 응답 큐 관련 설정 값
	@Value("${container-response.rabbitmq.exchange.name}")
	private String containerResponseExchangeName;
	@Value("${container-response.rabbitmq.queue.name.acquire-success}")
	private String containerAcquireSuccessQueueName;
	@Value("${container-response.rabbitmq.queue.name.acquire-failure}")
	private String containerAcquireFailureQueueName;
	@Value("${container-response.rabbitmq.routing.key.acquire-success}")
	private String acquireSuccessRoutingKey;
	@Value("${container-response.rabbitmq.routing.key.acquire-failure}")
	private String acquireFailureRoutingKey;

	// 컨테이너 정리 큐 관련 설정 값
	@Value("${container-cleanup.rabbitmq.exchange.name}")
	private String containerCleanupExchangeName;
	@Value("${container-cleanup.rabbitmq.queue.name}")
	private String containerCleanupQueueName;
	@Value("${container-cleanup.rabbitmq.routing.key}")
	private String containerCleanupRoutingKey;

	// 컨테이너 정리 응답 큐 관련 설정 값
	@Value("${container-response.rabbitmq.queue.name.cleanup-success}")
	private String containerCleanupSuccessQueueName;
	@Value("${container-response.rabbitmq.routing.key.cleanup-success}")
	private String containerCleanupSuccessRoutingKey;

	// 프로젝트 삭제 큐 관련 설정 값
	@Value("${project-delete.rabbitmq.exchange.name}")
	private String projectDeleteExchangeName;
	@Value("${project-delete.rabbitmq.queue.name}")
	private String projectDeleteQueueName;
	@Value("${project-delete.rabbitmq.routing.key}")
	private String projectDeleteRoutingKey;

	// 프로젝트 삭제 응답 큐 관련 설정 값
	@Value("${project-response.rabbitmq.exchange.name}")
	private String projectResponseExchangeName;
	@Value("${project-response.rabbitmq.queue.name.delete-success}")
	private String projectDeleteSuccessQueueName;
	@Value("${project-response.rabbitmq.routing.key.delete-success}")
	private String projectDeleteSuccessRoutingKey;

	// 코드 실행 큐, 교환기, 바인딩
	@Bean
	public Queue codeExecutionQueue() {
		return new Queue(codeExecutionQueueName, true);
	}

	@Bean
	public TopicExchange codeExecutionExchange() {
		return new TopicExchange(codeExecutionExchangeName);
	}

	@Bean
	public Binding codeExecutionBinding() {
		return BindingBuilder.bind(codeExecutionQueue()).to(codeExecutionExchange()).with(codeExecutionRoutingKey);
	}

	// RPC 큐, 교환기, 바인딩
	@Bean
	public Queue rpcQueue() {
		return new Queue(rpcQueueName, true);
	}

	@Bean
	public TopicExchange rpcExchange() {
		return new TopicExchange(rpcExchangeName);
	}

	@Bean
	public Binding rpcBinding() {
		return BindingBuilder.bind(rpcQueue()).to(rpcExchange()).with(rpcRoutingKey);
	}

	// [추가] 컨테이너 삭제 큐, 교환기, 바인딩
	@Bean
	public Queue containerDeleteQueue() {
		return new Queue(containerDeleteQueueName, true);
	}

	@Bean
	public TopicExchange containerLifecycleExchange() {
		return new TopicExchange(containerLifecycleExchangeName);
	}

	@Bean
	public Binding containerDeleteBinding() {
		return BindingBuilder.bind(containerDeleteQueue())
			.to(containerLifecycleExchange())
			.with(containerDeleteRoutingKey);
	}

	// [신규] PTY 세션 시작 큐, 교환기, 바인딩
	@Bean
	public Queue ptyStartQueue() {
		return new Queue(ptyStartQueueName, true);
	}

	@Bean
	public TopicExchange ptyStartExchange() {
		return new TopicExchange(ptyStartExchangeName);
	}

	@Bean
	public Binding ptyStartBinding() {
		return BindingBuilder.bind(ptyStartQueue()).to(ptyStartExchange()).with(ptyStartRoutingKey);
	}

	// [신규] PTY 명령어 큐, 교환기, 바인딩
	@Bean
	public Queue ptyCommandQueue() {
		return new Queue(ptyCommandQueueName, true);
	}

	@Bean
	public TopicExchange ptyCommandExchange() {
		return new TopicExchange(ptyCommandExchangeName);
	}

	@Bean
	public Binding ptyCommandBinding() {
		return BindingBuilder.bind(ptyCommandQueue()).to(ptyCommandExchange()).with(ptyCommandRoutingKey);
	}

	// 컨테이너 할당 큐, 교환기, 바인딩
	@Bean
	public Queue containerAcquireQueue() {
		return new Queue(containerAcquireQueueName, true);
	}

	@Bean
	public TopicExchange containerAcquireExchange() {
		return new TopicExchange(containerAcquireExchangeName);
	}

	@Bean
	public Binding containerAcquireBinding() {
		return BindingBuilder.bind(containerAcquireQueue())
			.to(containerAcquireExchange())
			.with(containerAcquireRoutingKey);
	}

	// 컨테이너 할당 응답 큐, 교환기, 바인딩
	@Bean
	public Queue containerAcquireSuccessQueue() {
		return new Queue(containerAcquireSuccessQueueName, true);
	}

	@Bean
	Queue containerAcquireFailureQueue() {
		return new Queue(containerAcquireFailureQueueName, true);
	}

	@Bean
	public TopicExchange containerResponseExchange() {
		return new TopicExchange(containerResponseExchangeName);
	}

	@Bean
	public Binding acquireSuccessBinding() {
		return BindingBuilder.bind(containerAcquireSuccessQueue())
			.to(containerResponseExchange())
			.with(acquireSuccessRoutingKey);
	}

	@Bean
	public Binding acquireFailureBinding() {
		return BindingBuilder.bind(containerAcquireFailureQueue())
			.to(containerResponseExchange())
			.with(acquireFailureRoutingKey);
	}

	// 컨테이너 정리 큐, 교환기, 바인딩
	@Bean
	public Queue containerCleanupQueue() {
		return new Queue(containerCleanupQueueName, true);
	}

	@Bean
	public TopicExchange containerCleanupExchange() {
		return new TopicExchange(containerCleanupExchangeName);
	}

	@Bean
	public Binding containerCleanupBinding() {
		return BindingBuilder.bind(containerCleanupQueue())
			.to(containerCleanupExchange())
			.with(containerCleanupRoutingKey);
	}

	// 컨테이너 정리 응답 큐, 바인딩 (교환기는 할당 응답 교환기와 동일)
	@Bean
	public Queue containerCleanupSuccessQueue() {
		return new Queue(containerCleanupSuccessQueueName, true);
	}

	@Bean
	public Binding containerCleanupSuccessBinding() {
		return BindingBuilder.bind(containerCleanupSuccessQueue())
			.to(containerResponseExchange())
			.with(containerCleanupSuccessRoutingKey);
	}

	// 프로젝트 삭제 큐, 교환기, 바인딩
	@Bean
	public Queue projectDeleteQueue() {
		return new Queue(projectDeleteQueueName, true);
	}

	@Bean
	public TopicExchange projectDeleteExchange() {
		return new TopicExchange(projectDeleteExchangeName);
	}

	@Bean
	public Binding projectDeleteBinding() {
		return BindingBuilder.bind(projectDeleteQueue())
			.to(projectDeleteExchange())
			.with(projectDeleteRoutingKey);
	}

	// 프로젝트 삭제 응답 큐, 교환기, 바인딩
	@Bean
	public Queue projectDeleteSuccessQueue() {
		return new Queue(projectDeleteSuccessQueueName, true);
	}

	@Bean
	public TopicExchange projectResponseExchange() {
		return new TopicExchange(projectResponseExchangeName);
	}

	@Bean
	public Binding projectDeleteSuccessBinding() {
		return BindingBuilder.bind(projectDeleteSuccessQueue())
			.to(projectResponseExchange())
			.with(projectDeleteSuccessRoutingKey);
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		// Producer(main-server)가 JSON으로 보낸 메시지를 DTO 객체로 자동 변환하기 위해
		// 동일한 MessageConverter를 설정합니다.
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
		// Spring AMQP가 DTO를 JSON으로 자동 변환하여 메시지를 보낼 수 있도록
		// 우리가 정의한 MessageConverter를 사용하는 RabbitTemplate을 생성합니다.
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(messageConverter);
		return rabbitTemplate;
	}

	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
		ConnectionFactory connectionFactory,
		SimpleRabbitListenerContainerFactoryConfigurer configurer,
		MessageConverter messageConverter) {

		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setMessageConverter(messageConverter);
		return factory;
	}
}
