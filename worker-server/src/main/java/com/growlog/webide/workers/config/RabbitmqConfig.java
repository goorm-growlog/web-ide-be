package com.growlog.webide.workers.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
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

	@Bean
	public MessageConverter jsonMessageConverter() {
		// Producer(main-server)가 JSON으로 보낸 메시지를 DTO 객체로 자동 변환하기 위해
		// 동일한 MessageConverter를 설정합니다.
		return new Jackson2JsonMessageConverter();
	}
}
