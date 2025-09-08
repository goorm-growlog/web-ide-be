package com.growlog.webide.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
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
}
