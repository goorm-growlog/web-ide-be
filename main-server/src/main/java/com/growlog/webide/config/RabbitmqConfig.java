package com.growlog.webide.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {

	// 코드 실행 큐 관련 설정 값
	@Value("${code-execution.rabbitmq.exchange.name}")
	private String codeExecutionExchangeName;
	@Value("${code-execution.rabbitmq.queue.name}")
	private String codeExecutionQueueName;
	@Value("${code-execution.rabbitmq.routing.key}")
	private String codeExecutionRoutingKey;

	// 터미널 명령어 큐 관련 설정 값
	@Value("${terminal-command.rabbitmq.exchange.name}")
	private String terminalCommandExchangeName;
	@Value("${terminal-command.rabbitmq.queue.name}")
	private String terminalCommandQueueName;
	@Value("${terminal-command.rabbitmq.routing.key}")
	private String terminalCommandRoutingKey;

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

	// 터미널 명령어 큐, 교환기, 바인딩
	@Bean
	public Queue terminalCommandQueue() {
		return new Queue(terminalCommandQueueName, true);
	}

	@Bean
	public TopicExchange terminalCommandExchange() {
		return new TopicExchange(terminalCommandExchangeName);
	}

	@Bean
	public Binding terminalCommandBinding() {
		return BindingBuilder.bind(terminalCommandQueue())
			.to(terminalCommandExchange())
			.with(terminalCommandRoutingKey);
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		// DTO 객체를 JSON으로 직렬화/역직렬화하기 위한 메시지 컨버터입니다.
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
}
