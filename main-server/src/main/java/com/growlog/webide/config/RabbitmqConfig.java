package com.growlog.webide.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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

	// 로그 수신 큐 관련 설정 값
	@Value("${log-reception.rabbitmq.exchange.name}")
	private String logReceptionExchangeName;
	@Value("${log-reception.rabbitmq.queue.name}")
	private String logReceptionQueueName;
	@Value("${log-reception.rabbitmq.routing.key}")
	private String logReceptionRoutingKey;

	// [추가] RPC 관련 설정 값
	@Value("${rpc.rabbitmq.exchange.name}")
	private String rpcExchangeName;
	@Value("${rpc.rabbitmq.request-routing-key}")
	private String rpcRequestRoutingKey;
	@Value("${rpc.rabbitmq.reply-queue-name}")
	private String rpcReplyQueueName;

	// 컨테이너 생명주기(삭제) 관련 설정 값
	@Value("${container-lifecycle.rabbitmq.exchange.name}")
	private String containerLifecycleExchangeName;
	@Value("${container-lifecycle.rabbitmq.response.queue-name}")
	private String containerDeletedAckQueueName;
	@Value("${container-lifecycle.rabbitmq.response.routing-key}")
	private String containerDeletedAckRoutingKey;

	// [신규] PTY 세션 관련 설정 값
	@Value("${pty-session.rabbitmq.start.exchange}")
	private String ptyStartExchangeName;
	@Value("${pty-session.rabbitmq.command.exchange}")
	private String ptyCommandExchangeName;

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

	// 프로젝트 DB 정리 요청 큐 관련 설정 값
	@Value("${project-db-cleanup.rabbitmq.exchange.name}")
	private String projectDbCleanupExchangeName;
	@Value("${project-db-cleanup.rabbitmq.queue.name}")
	private String projectDbCleanupQueueName;
	@Value("${project-db-cleanup.rabbitmq.routing.key}")
	private String projectDbCleanupRoutingKey;

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

	// 로그 수신 큐, 교환기, 바인딩
	@Bean
	public Queue logReceptionQueue() {
		return new Queue(logReceptionQueueName, true);
	}

	@Bean
	public TopicExchange logReceptionExchange() {
		return new TopicExchange(logReceptionExchangeName);
	}

	@Bean
	public Binding logReceptionBinding() {
		return BindingBuilder.bind(logReceptionQueue()).to(logReceptionExchange()).with(logReceptionRoutingKey);
	}

	// 컨테이너 생명주기(삭제) 관련 교환기
	@Bean
	public TopicExchange containerLifecycleExchange() {
		return new TopicExchange(containerLifecycleExchangeName);
	}

	// 컨테이너 삭제 완료 응답을 수신할 큐
	@Bean
	public Queue containerDeletedAckQueue() {
		return new Queue(containerDeletedAckQueueName, true);
	}

	@Bean
	public Binding containerDeletedAckBinding() {
		return BindingBuilder.bind(containerDeletedAckQueue())
			.to(containerLifecycleExchange())
			.with(containerDeletedAckRoutingKey);
	}

	// [신규] PTY 세션 관련 교환기 (Main-server는 보내기만 하므로 Exchange만 선언)
	@Bean
	public TopicExchange ptyStartExchange() {
		return new TopicExchange(ptyStartExchangeName);
	}

	@Bean
	public TopicExchange ptyCommandExchange() {
		return new TopicExchange(ptyCommandExchangeName);
	}

	// =================================================================
	// 2. RPC(원격 프로시저 호출) 관련 설정
	// =================================================================

	/**
	 * [추가] Worker 서버로부터 RPC 응답을 수신할 전용 큐(Queue)를 생성합니다.
	 * 이것이 바로 "답장을 받을 우편함"입니다.
	 */
	@Bean
	public Queue rpcReplyQueue() {
		return new Queue(rpcReplyQueueName, true);
	}

	/**
	 * [추가] RPC 요청/응답을 위한 RabbitTemplate을 별도로 설정합니다.
	 * 이 템플릿은 지정된 응답 큐를 리스닝하도록 설정됩니다.
	 * 이것이 바로 "내 전화번호가 찍히는 발신자 표시 전화기"입니다.
	 */
	@Bean("rpcRabbitTemplate")
	public RabbitTemplate rpcRabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(messageConverter);
		template.setReplyAddress(rpcReplyQueueName); // ★★★ 응답을 받을 주소(큐 이름)를 지정
		template.setReplyTimeout(60000); // 응답 대기 시간 (60초)
		return template;
	}

	/**
	 * [추가] RPC 응답을 수신하는 전용 리스너 컨테이너를 설정합니다.
	 * 이 컨테이너는 'rpcReplyQueue'를 리스닝하고, 수신된 메시지를
	 * 'rpcRabbitTemplate'으로 전달하여 대기 중인 요청에 대한 응답을 처리합니다.
	 */
	@Bean
	public SimpleMessageListenerContainer rpcReplyMessageListenerContainer(
		ConnectionFactory connectionFactory,
		@Qualifier("rpcRabbitTemplate") RabbitTemplate rpcRabbitTemplate) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		container.setQueues(rpcReplyQueue());
		container.setMessageListener(rpcRabbitTemplate); // rpcRabbitTemplate이 직접 리스너 역할을 합니다.
		return container;
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

	@Bean Queue containerAcquireFailureQueue() {
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
			.to(projectDeleteExchange())
			.with(projectDeleteSuccessRoutingKey);
	}

	@Bean
	public Queue projectDbCleanupQueue() {
		return new Queue(projectDbCleanupQueueName, true);
	}

	@Bean
	public TopicExchange projectDbCleanupExchange() {
		return new TopicExchange(projectDbCleanupExchangeName);
	}

	@Bean
	public Binding projectDbCleanupBinding() {
		return BindingBuilder.bind(projectDbCleanupQueue())
			.to(projectDbCleanupExchange())
			.with(projectDbCleanupRoutingKey);
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		// DTO 객체를 JSON으로 직렬화/역직렬화하기 위한 메시지 컨버터입니다.
		return new Jackson2JsonMessageConverter();
	}

	@Bean("rabbitTemplate")
	@Primary // 여러 RabbitTemplate 중 기본으로 사용될 Bean을 지정합니다.
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
