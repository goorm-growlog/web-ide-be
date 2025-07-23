package com.growlog.webide.config;

import java.time.*;

import org.springframework.context.annotation.*;

import com.github.dockerjava.api.*;
import com.github.dockerjava.core.*;
import com.github.dockerjava.httpclient5.*;
import com.github.dockerjava.transport.*;

@Configuration
public class DockerConfig {

	@Bean
	public DockerClient dockerClient() {
		// 1. 도커 설정 불러오기
		DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

		// 2. HTTP 클라이언트 설정
		DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
			.dockerHost(config.getDockerHost())
			.sslConfig(config.getSSLConfig())
			.maxConnections(100)
			.connectionTimeout(Duration.ofSeconds(30))
			.responseTimeout(Duration.ofSeconds(45))
			.build();

		// 3. 도커 클라이언트 생성 및 반환
		return DockerClientImpl.getInstance(config, httpClient);
	}
}
