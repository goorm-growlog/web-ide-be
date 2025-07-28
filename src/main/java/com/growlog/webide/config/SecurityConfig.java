package com.growlog.webide.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.jwt.JwtAuthenticationFilter;
import com.growlog.webide.global.common.jwt.JwtTokenProvider;
import com.growlog.webide.global.security.CustomUserDetailService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private static final String[] SWAGGER_WHITELIST = {
		"/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/swagger-resources/**", "/webjars/**"
	};

	@Bean
	public SecurityFilterChain filterChain(
		HttpSecurity http,
		JwtTokenProvider jwtTokenProvider,
		UserRepository userRepository
	) throws Exception {

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
			.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userRepository),
				UsernamePasswordAuthenticationFilter.class)
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable());

		return http.build();
	}
}
