package com.growlog.webide.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "template")
public class TemplateProperties {
	private String dockerUsername;

	public void setDockerUsername(String dockerUsername) {
		this.dockerUsername = dockerUsername;
	}
}
