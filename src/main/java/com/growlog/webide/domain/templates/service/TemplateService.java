package com.growlog.webide.domain.templates.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.growlog.webide.config.TemplateProperties;
import com.growlog.webide.factory.DockerClientFactory;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {
	private final DockerClientFactory dockerClientFactory;
	private final TemplateProperties templateProperties;

	public void applyTemplate(String language, String version, String volumeName) {
		final String dockerUsername = templateProperties.getDockerUsername();  // 주입 필요
		final String imageName = String.format("%s/%s-template:%s", dockerUsername, language.toLowerCase(), version);
		final DockerClient dockerClient = dockerClientFactory.buildDockerClient();
		String containerId = null;

		try {
			// 1. 컨테이너 생성
			CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
				.withHostConfig(new HostConfig().withBinds(
					new Bind(volumeName, new Volume("/app")) // /app에 볼륨 마운트
				))
				.withTty(true)
				.withCmd("sh") // 복사 명령은 exec로 실행
				.exec();

			containerId = container.getId();
			dockerClient.startContainerCmd(containerId).exec();
			log.info("템플릿 컨테이너 {} 시작 (image: {})", containerId, imageName);

			// 2. 복사 명령 실행 (/template/src → /app/src)
			String copyCommand = "mkdir -p /app/src && cp -r /template/src/* /app/src/";
			ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
				.withAttachStdout(true)
				.withAttachStderr(true)
				.withCmd("sh", "-c", copyCommand)
				.exec();

			dockerClient.execStartCmd(exec.getId())
				.exec(new ExecStartResultCallback(System.out, System.err))
				.awaitCompletion(1, TimeUnit.MINUTES);

			log.info("템플릿 복사 완료: {} → /app/src", imageName);

		} catch (Exception e) {
			log.error("템플릿 적용 실패 (image: {})", imageName, e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		} finally {
			if (dockerClient != null && containerId != null) {
				try {
					dockerClient.stopContainerCmd(containerId).exec();
					dockerClient.removeContainerCmd(containerId).exec();
					log.info("템플릿 컨테이너 {} 정리 완료", containerId);
				} catch (Exception e) {
					log.warn("컨테이너 정리 중 오류: {}", e.getMessage());
				}
				try {
					dockerClient.close();
				} catch (IOException e) {
					log.warn("DockerClient 닫기 실패", e);
				}
			}
		}
	}
}
