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

	@Value("${rabbitmq.exchange.name}")
	private String exchangeName;
	@Value("${rabbitmq.queue.name}")
	private String queueName;
	@Value("${rabbitmq.routing.key}")
	private String routingKey;

	@Bean
	public Queue queue() {
		// 메시지가 쌓일 큐를 정의합니다.
		// durable: true는 RabbitMQ가 재시작되어도 큐가 유지되도록 설정합니다.
		return new Queue(queueName, true);
	}

	@Bean
	public TopicExchange exchange() {
		// 메시지를 라우팅할 교환기를 정의합니다.
		return new TopicExchange(exchangeName);
	}

	@Bean
	public Binding binding(Queue queue, TopicExchange exchange) {
		// 교환기와 큐를 연결하여, 라우팅 키가 일치하는 메시지를 큐로 보냅니다.
		return BindingBuilder.bind(queue).to(exchange).with(routingKey);
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
