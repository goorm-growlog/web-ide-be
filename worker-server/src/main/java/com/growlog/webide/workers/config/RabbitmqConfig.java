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
		// Producer(main-server)가 JSON으로 보낸 메시지를 DTO 객체로 자동 변환하기 위해
		// 동일한 MessageConverter를 설정합니다.
		return new Jackson2JsonMessageConverter();
	}
}
