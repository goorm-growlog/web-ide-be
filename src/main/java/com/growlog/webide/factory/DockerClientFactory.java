package com.growlog.webide.factory;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

@Component
public class DockerClientFactory {
	/**
	 * 필요할 때마다 새로운 DockerClient 인스턴스 생성
	 * @return 새로 생성된 DockerClient 객체
	 */
	public DockerClient buildDockerClient() {
		DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
			.withDockerHost("unix://var/run/docker.sock").build();
		DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
			.dockerHost(config.getDockerHost())
			.sslConfig(config.getSSLConfig())
			.maxConnections(100)
			.connectionTimeout(Duration.ofSeconds(30))
			.responseTimeout(Duration.ofSeconds(45)).build();

		return DockerClientImpl.getInstance(config, httpClient);
	}
}
