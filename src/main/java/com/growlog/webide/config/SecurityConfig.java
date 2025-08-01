package com.growlog.webide.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.jwt.JwtAuthenticationFilter;
import com.growlog.webide.global.common.jwt.JwtTokenProvider;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private static final String[] SWAGGER_WHITELIST = {
		"/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/swagger-resources/**", "/webjars/**"
	};

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(
		HttpSecurity http,
		JwtTokenProvider jwtTokenProvider,
		UserRepository userRepository
	) throws Exception {
		http
			// CSRF 보호 비활성화 (API 서버는 상태를 저장하지 않으므로 일반적으로 비활성화)
			.csrf(AbstractHttpConfigurer::disable)
			// CORS 설정
			.cors(withDefaults())
			// 세션 관리 (STATELESS)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// 헤더 설정
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
			// HTTP 요청에 대한 인가(Authorization) 규칙 설정
			.authorizeHttpRequests(authorize -> authorize
				// Swagger UI 접근을 위한 경로들 허용
				.requestMatchers(SWAGGER_WHITELIST).permitAll()
				.requestMatchers(
					"/", "/index.html", "/favicon.ico",
					"/css/**", "/js/**", "/img/**", "/static/**"
				).permitAll()
				.requestMatchers("/swagger-ui.html").permitAll()
				// API 경로 허용
				.requestMatchers("/api/**").permitAll()
				// WebSocket 엔드포인트 허용
				.requestMatchers("/ws/**").permitAll()
				// 채팅 테스트용 토큰 발급 페이지 허용
				.requestMatchers("get-token").permitAll()
				// 웹소켓 테스트 페이지 허용
				.requestMatchers("/websockettest.html").permitAll()
				// 인증 관련 경로 허용
				.requestMatchers(
					"/auth/email/**", // 인증 요청
					"/auth/reset-password",
					"/auth/login",
					"/users/signup"
				).permitAll()
				// 프로젝트 관련 경로는 인증 필요
				.requestMatchers("/projects/**").authenticated()
				// 그 외의 모든 요청은 인증 필요
				.anyRequest().authenticated()
			)
			// JWT 인증 필터 추가
			.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userRepository),
				UsernamePasswordAuthenticationFilter.class)
			// 폼 로그인 비활성화
			.formLogin(form -> form.disable())
			// HTTP Basic 인증 비활성화
			.httpBasic(basic -> basic.disable());

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
