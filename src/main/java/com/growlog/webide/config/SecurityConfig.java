package com.growlog.webide.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.growlog.webide.global.common.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// CSRF 보호 비활성화
			.csrf(csrf -> csrf.disable())

			// 세션 관리 정책을 STATELESS로 설정
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// HTTP 요청에 대한 인가 규칙 설정
			.authorizeHttpRequests(authz -> authz
				// ✅ 웹소켓 연결을 위한 /ws/** 경로는 인증 없이 허용
				.requestMatchers("/ws/**", "/get-token", "/api/**").permitAll()

				// 위 경로들을 제외한 나머지 모든 요청은 인증 필요
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * WebSocket 메시지(STOMP)에 대한 보안 규칙 설정
	 */
	@Bean
	public AuthorizationManager<Message<?>> messageAuthorizationManager() {
		MessageMatcherDelegatingAuthorizationManager.Builder messages =
			MessageMatcherDelegatingAuthorizationManager.builder();

		messages
			.simpSubscribeDestMatchers("/topic/**", "/queue/**").authenticated()
			.simpDestMatchers("/app/**").authenticated()
			.anyMessage().permitAll();

		return messages.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("*"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
