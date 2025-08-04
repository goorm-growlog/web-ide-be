package com.growlog.webide.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			// CORS 설정을 여기에 추가.

			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
					.allowedOrigins("https://growlog-web-ide.vercel.app")
					.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
					.allowedHeaders("*")
					.allowCredentials(true); // 쿠키나 인증 정보 포함 허용 시 필요
			}
		};
	}
}
