package com.growlog.webide.global.docker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DockerCommandServiceImpl implements DockerCommandService {

	@Value("${docker.workspace-path}")
	private String workspaceDir;

	public String readFileContent(String containerName, String filePathInContainer) {

		String fullPath = workspaceDir + "/" + filePathInContainer;
		List<String> command = List.of("docker", "exec", containerName, "cat", fullPath);

		try {
			log.info("📦 실행할 Docker 명령어: {}", String.join(" ", command));

			ProcessBuilder builder = new ProcessBuilder(command);
			builder.redirectErrorStream(false); //stderr 따로 보기 위해 false

			Process process = builder.start();

			try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))
			) {

				String result = reader.lines().collect(Collectors.joining("\n"));
				String error = errorReader.lines().collect(Collectors.joining("\n"));

				if (!error.isBlank()) {
					log.warn("⚠️ Docker stderr: {}", error);

					if (error.contains("No such container")) {
						throw new CustomException(ErrorCode.CONTAINER_NOT_FOUND);
					} else if (error.contains("No such file or directory")) {
						throw new CustomException(ErrorCode.FILE_NOT_FOUND);
					} else {
						throw new CustomException(ErrorCode.DOCKER_COMMAND_FAILED);
					}
				}

				return result;
			}

		} catch (Exception e) {
			log.error("❌ Docker 명령어 실행 중 예외 발생", e);
			throw new CustomException(ErrorCode.DOCKER_COMMAND_FAILED);
		}
	}

}
