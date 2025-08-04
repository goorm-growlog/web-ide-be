package com.growlog.webide.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {
	@Bean
	public OpenAPI openApi() {
		final String securitySchemeName = "bearerAuth";

		return new OpenAPI()
			.addServersItem(new Server().url("https://growlog-web-ide.duckdns.org"))
			.addSecurityItem(new SecurityRequirement().addList(securitySchemeName)) // Security Requirement 추가
			.components(new Components().addSecuritySchemes(securitySchemeName,  // Security Scheme 추가
				new SecurityScheme()
					.name(securitySchemeName)
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.in(SecurityScheme.In.HEADER)))
			.info(new Info()
				.title("[Growlog] Web IDE API")
				.version("v1.0")
				.description("Growlog팀 Web IDE API입니다."))
			.components(new Components()
				.addSecuritySchemes(securitySchemeName,
					new SecurityScheme()
						.name(securitySchemeName)
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
				)
			);
	}

}
