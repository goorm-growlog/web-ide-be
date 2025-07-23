package com.growlog.webide.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

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
