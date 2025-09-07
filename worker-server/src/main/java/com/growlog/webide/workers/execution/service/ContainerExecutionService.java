package com.growlog.webide.workers.execution.service;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.Volume;
import com.growlog.webide.workers.execution.dto.CodeExecutionRequestDto;
import com.growlog.webide.workers.execution.dto.ContainerCreationRequest;
import com.growlog.webide.workers.execution.dto.LogMessage;
import com.growlog.webide.workers.execution.dto.TerminalCommandRequestDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerExecutionService {

	private final DockerClient dockerClient;
	private final RabbitTemplate rabbitTemplate;

	@Value("${docker.host.workspace-base-path}")
	private String hostWorkspaceBasePath;

	@Value("${docker.container.workspace-path}")
	private String containerWorkspacePath;

	// [개선] 로그 전송을 위한 RabbitMQ 설정을 외부에서 주입받습니다.
	@Value("${log.rabbitmq.exchange.name}")
	private String logExchangeName;
	@Value("${log.rabbitmq.routing.key}")
	private String logRoutingKey;

	/**
	 * [Stateless] 일회성 코드 실행 요청을 처리합니다.
	 * 임시 컨테이너를 생성하고, 내부에 명령어를 실행한 뒤, 컨테이너를 정리합니다.
	 */
	@RabbitListener(queues = "${code-execution.rabbitmq.queue.name}")
	public void executeCode(CodeExecutionRequestDto request) {
		String containerId = null;
		String executionLogId = request.getExecutionLogId();
		try {
			// 0. 컨테이너 실행에 필요한 이미지가 로컬에 없으면 pull 합니다.
			pullImageIfNotExists(request.getDockerImage());

			// 1. 컨테이너 생성 및 시작
			containerId = createAndStartExecutionContainer(request);
			log.info("Container created and started for code execution: {}", containerId);

			// 2. 실행할 최종 명령어 생성
			String finalCommand = buildFinalCommand(request);
			log.info("Executing command in container {}: {}", containerId, finalCommand);

			// 3. 임시 컨테이너 내부에서 명령어 실행 및 결과 로깅
			log.info("Using execution log ID from main-server: {}", executionLogId);
			executeCommandInContainer(containerId, finalCommand, true, executionLogId,
				request.getUserId()); // isTemporary = true

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
	 * [Stateful] 영구적인 터미널 컨테이너 생성을 요청받아 처리합니다. (RPC)
	 */
	@RabbitListener(queues = "${rpc.rabbitmq.queue.name}")
	public String createPersistentContainer(ContainerCreationRequest request) {
		pullImageIfNotExists(request.getDockerImage());
		HostConfig hostConfig = createHostConfig(request.getProjectId());
		CreateContainerResponse container = dockerClient.createContainerCmd(request.getDockerImage())
			.withHostConfig(hostConfig)
			.withWorkingDir(containerWorkspacePath)
			.withTty(true) // Keep container running
			.withStdInOnce(false)
			.withStdinOpen(true)
			.withCmd("tail", "-f", "/dev/null")
			.exec();
		dockerClient.startContainerCmd(container.getId()).exec();
		log.info("Persistent container created: {}", container.getId());
		return container.getId();
	}

	/**
	 * [Stateful] 이미 존재하는 영구 컨테이너에 터미널 명령어를 실행합니다.
	 */
	@RabbitListener(queues = "${terminal-command.rabbitmq.queue.name}")
	public void executeCommandInPersistentContainer(TerminalCommandRequestDto request) {
		String containerId = request.getContainerId();
		if (containerId == null || containerId.isBlank()) {
			log.error("executeCommandInPersistentContainer called without a containerId.");
			return;
		}
		try {
			String finalCommand = request.getCommand(); // Assuming the command is ready to be executed
			log.info("Executing terminal command in persistent container {}: {}", containerId, finalCommand);
			executeCommandInContainer(containerId, finalCommand, false, containerId,
				request.getUserId()); // isTemporary = false
		} catch (Exception e) {
			log.error("An error occurred during terminal command execution for container {}", containerId, e);
		}
	}

	/**
	 * [추가] 컨테이너 삭제 요청을 수신하고 처리하는 리스너
	 * @param containerId 삭제할 컨테이너의 ID
	 */
	@RabbitListener(queues = "${container-lifecycle.rabbitmq.queue.name}")
	public void handleContainerDeletion(String containerId) {
		log.info("Received request to delete container: {}", containerId);
		try {
			cleanupContainer(containerId);
			log.info("Successfully deleted container: {}", containerId);
		} catch (Exception e) {
			log.error("Failed to delete container {}", containerId, e);
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

	private void executeCommandInContainer(String containerId, String finalCommand, boolean isTemporary,
		String logTargetId, Long userId) throws InterruptedException {
		String[] command = {"/bin/sh", "-c", finalCommand};
		ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
			.withAttachStdout(true)
			.withAttachStderr(true)
			.withCmd(command)
			.withWorkingDir(containerWorkspacePath) // <-- 이 줄을 추가하여 명령어 실행 위치를 지정합니다.
			.exec();

		LogContainerCallback callback = new LogContainerCallback(containerId, rabbitTemplate, logTargetId, userId,
			logExchangeName, logRoutingKey);
		try (LogContainerCallback a = callback) {
			dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback);
			// 명령어 실행이 완료될 때까지 대기합니다. 최대 대기 시간을 10초로 줄여 불필요한 지연을 방지합니다.
			callback.awaitCompletion(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("Error during command execution in container {}", containerId, e);
		}
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
	 *
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
		private final String containerId;
		private final RabbitTemplate rabbitTemplate;
		private final String logTargetId; // 로그를 보낼 ID (터미널은 containerId, 일회성 실행은 임시 ID)
		private final Long userId; // 로그를 수신할 사용자의 ID
		private final String exchangeName;
		private final String routingKey;

		public LogContainerCallback(String containerId, RabbitTemplate rabbitTemplate, String logTargetId, Long userId,
			String exchangeName, String routingKey) {
			this.containerId = containerId;
			this.rabbitTemplate = rabbitTemplate;
			this.logTargetId = logTargetId;
			this.userId = userId;
			this.exchangeName = exchangeName;
			this.routingKey = routingKey;
		}

		@Override
		public void onNext(Frame frame) {
			String streamType = frame.getStreamType() == StreamType.STDERR ? "stderr" : "stdout";
			// [수정] 컨테이너 출력(byte[])을 문자열로 변환할 때 UTF-8 인코딩을 명시적으로 지정합니다.
			String content = new String(frame.getPayload(), StandardCharsets.UTF_8);
			LogMessage logMessage = new LogMessage(logTargetId, userId, streamType, content);

			log.trace("Sending log for [{}]: {}", logTargetId, content.trim());
			// [개선] 하드코딩된 값을 제거하고 주입받은 설정 값을 사용합니다.
			rabbitTemplate.convertAndSend(exchangeName, routingKey, logMessage);
		}

		@Override
		public void onError(Throwable throwable) {
			log.error("Error in LogContainerCallback for container {}", containerId, throwable);
			super.onError(throwable);
		}

		@Override
		public void close() {
			// try-with-resources 구문에 의해 이 메서드가 호출되어 리소스가 정리됩니다.
		}
	}
}
