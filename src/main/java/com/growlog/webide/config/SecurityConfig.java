package com.growlog.webide.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.growlog.webide.global.common.jwt.JwtAuthenticationFilter;
import com.growlog.webide.global.common.jwt.JwtTokenProvider;

@Configuration
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private static final String[] SWAGGER_WHITELIST = {
		"/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/swagger-resources/**", "/webjars/**"
	};

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
		JwtTokenProvider jwtTokenProvider) throws Exception {

		http
			.cors(withDefaults())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(SWAGGER_WHITELIST).permitAll()
				.requestMatchers(
					"/auth/email/**", // 인증 요청
					"/auth/reset-password",
					"/auth/login",
					"/users/signup"
				).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
				UsernamePasswordAuthenticationFilter.class)
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable());

		return http.build();
	}
	/*@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable()) // CSRF 비활성화
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/swagger-ui/**",
					"/v3/api-docs/**",
					"api-docs/**",
					"/swagger-resources/**",
					"/webjars/**"
				).permitAll() // Swagger 허용
				.requestMatchers(
					"/auth/email/**",         // 이메일 인증
					"/auth/reset-password",   // 비밀번호 재설정
					"/users/signup",          // 회원가입
					"/auth/login"           // 로그인 (토큰 발급용)
				).permitAll()
				.anyRequest().authenticated() // 나머지는 인증 필요
			)
			.formLogin(form -> form.disable()); // 기본 로그인 페이지 비활성화 (JWT 기반이면 보통 disable)
		return http.build();
	}*/
}
