package com.growlog.webide.workers.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
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
}
