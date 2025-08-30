package com.growlog.webide.workers.execution.service;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.growlog.webide.workers.execution.dto.CodeExecutionRequestDto;
import com.growlog.webide.workers.execution.dto.TerminalCommandRequestDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerExecutionService {

	private final DockerClient dockerClient;

	@Value("${docker.host.workspace-base-path}")
	private String hostWorkspaceBasePath;

	@Value("${docker.container.workspace-path}")
	private String containerWorkspacePath;

	/**
	 * 코드 실행 요청을 처리합니다.
	 * 컨테이너를 생성하고, 내부에 명령어를 실행한 뒤, 결과를 반환하고 컨테이너를 정리합니다.
	 */
	public void executeCode(CodeExecutionRequestDto request) {
		String containerId = null;
		try {
			// 0. 컨테이너 실행에 필요한 이미지가 로컬에 없으면 pull 합니다.
			pullImageIfNotExists(request.getDockerImage());

			// 1. 컨테이너 생성 및 시작
			containerId = createAndStartExecutionContainer(request);
			log.info("Container created and started for code execution: {}", containerId);

			// 2. 실행할 최종 명령어 생성
			String finalCommand = buildFinalCommand(request);
			log.info("Executing command in container {}: {}", containerId, finalCommand);

			// 3. 컨테이너 내부에서 명령어 실행 및 결과 로깅
			String output = executeCommandInContainer(containerId, finalCommand);
			log.info("Execution finished for container {}. Full output:\n{}", containerId, output);
			// To-Do: 이 결과를 WebSocket 등을 통해 사용자에게 다시 보내야 합니다.

		} catch (Exception e) {
			log.error("An error occurred during code execution for projectId {}", request.getProjectId(), e);
		} finally {
			// 4. 작업이 끝나면 컨테이너를 정리합니다.
			if (containerId != null) {
				cleanupContainer(containerId);
			}
		}
	}

	/**
	 * [테스트용] 터미널 컨테이너 생성이 잘 되는지 확인합니다.
	 * 간단한 명령어를 실행하고 결과를 로그로 남긴 뒤 컨테이너를 정리합니다.
	 */
	public void testTerminalContainer(TerminalCommandRequestDto request) {
		String containerId = null;
		try {
			// 0. 컨테이너 실행에 필요한 이미지가 로컬에 없으면 pull 합니다.
			pullImageIfNotExists(request.getDockerImage());

			// 1. 테스트용 컨테이너 생성 및 시작
			containerId = createAndStartTestContainer(request);
			log.info("Terminal test container created and started: {}", containerId);

			// 2. 컨테이너 로그를 가져와 출력
			String output = getLogsFromContainer(containerId);
			log.info("Terminal test finished for container {}. Full output:\n{}", containerId, output);

		} catch (Exception e) {
			log.error("An error occurred during terminal container test for projectId {}", request.getProjectId(), e);
		} finally {
			// 3. 테스트가 끝나면 컨테이너를 정리합니다.
			if (containerId != null) {
				cleanupContainer(containerId);
			}
		}
	}

	private String createAndStartExecutionContainer(CodeExecutionRequestDto request) {
		HostConfig hostConfig = createHostConfig(request.getProjectId());
		CreateContainerResponse container = dockerClient.createContainerCmd(request.getDockerImage())
			.withHostConfig(hostConfig)
			.withWorkingDir(containerWorkspacePath)
			.withCmd("tail", "-f", "/dev/null")
			.exec();
		dockerClient.startContainerCmd(container.getId()).exec();
		return container.getId();
	}

	private String createAndStartTestContainer(TerminalCommandRequestDto request) {
		HostConfig hostConfig = createHostConfig(request.getProjectId());
		CreateContainerResponse container = dockerClient.createContainerCmd(request.getDockerImage())
			.withHostConfig(hostConfig)
			.withWorkingDir(containerWorkspacePath)
			.withCmd("ls", "-al")
			.exec();
		dockerClient.startContainerCmd(container.getId()).exec();
		return container.getId();
	}

	private HostConfig createHostConfig(Long projectId) {
		// [수정] String.format 대신 Path.of를 사용하여 OS에 독립적인 경로 생성
		String hostProjectPath = Path.of(hostWorkspaceBasePath, String.valueOf(projectId)).toString();
		return new HostConfig().withBinds(new Bind(hostProjectPath, new Volume(containerWorkspacePath)));
	}

	private String buildFinalCommand(CodeExecutionRequestDto request) {
		String buildCmd = request.getBuildCommand();
		String runCmd = request.getRunCommand();
		// 윈도우의 역슬래시(\)를 슬래시(/)로 정규화하여 일관성 유지
		String normalizedFilePath = request.getFilePath().replace('\\', '/');

		if (buildCmd != null && !buildCmd.isBlank()) {
			buildCmd = buildCmd.replace("{filePath}", normalizedFilePath);
			// [개선] 패키지 구조를 고려하여 FQCN(정규화된 클래스 이름)을 추출
			String fqcn = getFullyQualifiedClassName(normalizedFilePath);
			runCmd = runCmd.replace("{className}", fqcn);
			return buildCmd + " && " + runCmd;
		} else {
			return runCmd.replace("{filePath}", normalizedFilePath);
		}
	}

	// 'src/'를 기준으로 파일 경로에서 정규화된 클래스 이름을 추출
	private String getFullyQualifiedClassName(String normalizedPath) {
		if (normalizedPath.startsWith("src/")) {
			return normalizedPath.substring(4) // "src/" 제거
				.replace(".java", "") // ".java" 확장자 제거
				.replace('/', '.'); // 디렉터리 구분자를 패키지 구분자로 변경
		}
		// 'src/'로 시작하지 않는 예외적인 경우, 파일 이름만 추출
		if (normalizedPath.contains("/")) {
			return normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1).replace(".java", "");
		}
		return normalizedPath.replace(".java", "");
	}

	private String executeCommandInContainer(String containerId, String finalCommand) throws InterruptedException {
		String[] command = {"/bin/sh", "-c", finalCommand};
		ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
			.withAttachStdout(true)
			.withAttachStderr(true)
			.withCmd(command)
			.withWorkingDir(containerWorkspacePath) // <-- 이 줄을 추가하여 명령어 실행 위치를 지정합니다.
			.exec();

		LogContainerCallback callback = new LogContainerCallback(containerId, "[Container {}] {}");
		try (LogContainerCallback a = callback) {
			dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback);
			// 명령어 실행이 완료될 때까지 대기합니다. 최대 대기 시간을 10초로 줄여 불필요한 지연을 방지합니다.
			callback.awaitCompletion(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("Error during command execution in container {}", containerId, e);
		}
		return callback.getLogs();
	}

	private String getLogsFromContainer(String containerId) throws InterruptedException {
		LogContainerCallback callback = new LogContainerCallback(containerId, "[Terminal Test {}] {}");
		try (LogContainerCallback a = callback) {
			dockerClient.logContainerCmd(containerId)
				.withStdOut(true)
				.withStdErr(true)
				.withFollowStream(true)
				.exec(callback);
			callback.awaitCompletion(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("Error during log retrieval from container {}", containerId, e);
		}
		return callback.getLogs();
	}

	private void cleanupContainer(String containerId) {
		log.info("Stopping and removing container: {}", containerId);
		try {
			// 컨테이너가 정상 종료될 때까지 기다리는 최대 시간을 5초로 지정합니다.
			dockerClient.stopContainerCmd(containerId).withTimeout(5).exec();
		} catch (NotModifiedException e) {
			log.warn("Container {} was already stopped.", containerId);
		}
		dockerClient.removeContainerCmd(containerId).exec();
	}

	/**
	 * 지정된 Docker 이미지가 로컬에 존재하지 않으면 Docker Hub(또는 원격 레지스트리)에서 pull 합니다.
	 * @param imageName 확인할 Docker 이미지 이름 (예: "ubuntu:22.04")
	 */
	private void pullImageIfNotExists(String imageName) {
		try {
			dockerClient.inspectImageCmd(imageName).exec();
			log.info("Image '{}' already exists locally.", imageName);
		} catch (NotFoundException e) {
			log.info("Image '{}' not found locally. Pulling from registry...", imageName);
			try {
				dockerClient.pullImageCmd(imageName)
					.exec(new ResultCallback.Adapter<>() {
						@Override
						public void onNext(PullResponseItem item) {
							log.debug("Pulling status: {}", item.getStatus());
						}
					}).awaitCompletion(5, TimeUnit.MINUTES); // 최대 5분 대기
				log.info("Image '{}' pulled successfully.", imageName);
			} catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				log.error("Image pull for '{}' was interrupted.", imageName, interruptedException);
				throw new RuntimeException("Image pull was interrupted", interruptedException);
			}
		}
	}

	/**
	 * 컨테이너의 출력을 로깅하고, 리소스를 안전하게 닫기 위한 ResultCallback 구현체
	 */
	static class LogContainerCallback extends ResultCallback.Adapter<Frame> implements Closeable {
		private final StringBuilder logs = new StringBuilder();
		private final String containerId;
		private final String logFormat;

		public LogContainerCallback(String containerId, String logFormat) {
			this.containerId = containerId;
			this.logFormat = logFormat;
		}

		@Override
		public void onNext(Frame frame) {
			String logLine = new String(frame.getPayload()).trim();
			if (!logLine.isEmpty()) {
				log.info(logFormat, containerId, logLine);
				logs.append(logLine).append("\n");
			}
		}

		@Override
		public void onError(Throwable throwable) {
			log.error("Error in LogContainerCallback for container {}", containerId, throwable);
			super.onError(throwable);
		}

		public String getLogs() {
			return logs.toString();
		}

		@Override
		public void close() {
			// try-with-resources 구문에 의해 이 메서드가 호출되어 리소스가 정리됩니다.
		}
	}
}
