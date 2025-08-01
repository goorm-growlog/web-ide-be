package com.growlog.webide.global.docker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.util.ProjectPathResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerCommandServiceImpl implements DockerCommandService {

	private final ProjectPathResolver pathResolver;

	@Override
	public String readFileFromVolume(String volumeName, String filePathInContainer) {
		String fullPath = pathResolver.getBaseDirectory() + "/" + filePathInContainer;
		List<String> command = List.of(
			"docker", "run", "--rm",
			"-v", volumeName + ":" + pathResolver.getBaseDirectory(),
			"busybox", "cat", fullPath
		);
		return executeDockerCommand(command);
	}

	/*
	 * 컨테이너 내부 파일 읽기 - activeinstance 기반
	 * */
	@Override
	public String readFileContent(String containerId, String filePathInContainer) {

		String fullPath = pathResolver.getBaseDirectory() + "/" + filePathInContainer;
		List<String> command = List.of("docker", "exec", containerId, "cat", fullPath);

		return executeDockerCommand(command);

	}

	@Override
	public void execInContainer(String containerId, String shellCommand) {
		List<String> cmd = List.of(
			"docker", "exec", containerId,
			"sh", "-c", shellCommand
		);
		executeDockerCommand(cmd);
	}

	/*
	 * 공통  docker 명령 실행 함수
	 * */
	private String executeDockerCommand(List<String> command) {
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

		} catch (CustomException ce) {
			// 우리가 의도적으로 던진 에러는 그대로 유지
			throw ce;

		} catch (Exception e) {
			log.error("❌ Docker 명령어 실행 중 예외 발생", e);
			throw new CustomException(ErrorCode.DOCKER_COMMAND_FAILED);
		}
	}

	@Override
	public void writeFileContent(String containerId, String filePathInContainer, String content) {
		// 파일 내용을 safe하게 처리 (큰 따옴표, 줄바꿈 주의)
		String escapedContent = content
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("$", "\\$") // 변수 치환 방지
			.replace("`", "\\`");

		String fullPath = pathResolver.getBaseDirectory() + "/" + filePathInContainer;

		List<String> command = List.of(
			"docker", "exec", containerId,
			"sh", "-c", "echo \"" + escapedContent + "\" > \"" + fullPath + "\""
		);

		executeDockerCommand(command); // 이미 구현된 공용 실행 함수
	}

	@Override
	public String execAndReturn(String containerId, String shellCommand) {
		List<String> cmd = List.of(
			"docker", "exec", containerId,
			"sh", "-c", shellCommand
		);
		return executeDockerCommand(cmd);
	}

}
