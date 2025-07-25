package com.growlog.webide.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 관련 전체 설정을 담당하는 클래스.
 * STOMP 프로토콜 기반 실시간 메시징을 활성화함.
 */
@Configuration
@EnableWebSocketMessageBroker // WebSocket 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	/**
	 * 클라이언트가 WebSocket으로 접속할 엔드포인트를 등록.
	 * 예: 프론트엔드에서 new SockJS('/ws')로 연결 가능.
	 */
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {

		// 순수 WebSocket 전용 (Postman에서 접속 가능)
		registry.addEndpoint("/ws")
			.setAllowedOriginPatterns("*");

		// "/ws" 엔드포인트로 WebSocket handshake 허용, SockJS fallback 지원
		// registry.addEndpoint("/ws")
		// 	.setAllowedOriginPatterns("*") // CORS 허용 (운영시 제한 권장)
		// 	.withSockJS();                 // SockJS 사용 (브라우저 호환성)
	}

	/**
	 * 메시지 브로커(경로) 설정.
	 * 구독/발행(pub/sub) 경로와 서버 메시지 전송 prefix를 지정.
	 */
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// "/topic" 경로를 메시지 브로커의 구독 주제(topic)로 사용 (브로드캐스트용)
		// "/queue" 경로를 메시지 브로커의 큐(queue)로 사용 (개별 사용자 전용 메시지용)
		registry.enableSimpleBroker("/topic", "/queue");

		// "/app" prefix로 들어온 메시지는 @MessageMapping 컨트롤러로 전달
		registry.setApplicationDestinationPrefixes("/app");

		// 특정 사용자 1:1 메시징을 위한 prefix, @SendToUser 어노테이션 사용 시 적용됨
		registry.setUserDestinationPrefix("/user");
	}
}
