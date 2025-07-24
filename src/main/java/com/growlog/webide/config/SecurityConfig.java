package com.growlog.webide.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Bean // 이 메소드가 반환하는 객체를 Spring Bean으로 등록합니다.
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// 1. CSRF 보호 비활성화 (API 서버는 상태를 저장하지 않으므로 일반적으로 비활성화)
			.csrf(AbstractHttpConfigurer::disable)

			// 2. HTTP 요청에 대한 인가(Authorization) 규칙 설정
			.authorizeHttpRequests(authorize -> authorize
				// "/api/**" 경로의 모든 요청을 인증 절차 없이 허용합니다.
				.requestMatchers("/api/**").permitAll()
				// Swagger UI 접근을 위한 경로들도 모두 허용합니다.
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
				// 💡 WebSocket 엔드포인트 허용
				.requestMatchers("/ws/**").permitAll()
				// 그 외의 모든 요청은 인증을 요구하도록 설정할 수 있습니다. (지금은 모든 것을 허용)
				.anyRequest().permitAll()
			);

		return http.build();
	}
}
