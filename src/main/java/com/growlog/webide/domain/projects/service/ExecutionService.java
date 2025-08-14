package com.growlog.webide.domain.projects.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.projects.dto.CodeExecutionResponse;
import com.growlog.webide.domain.projects.dto.ExecutionResult;
import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.factory.DockerClientFactory;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

	private final DockerClientFactory dockerClientFactory;
	private final ActiveInstanceRepository activeInstanceRepository;

	// 특정 프로젝트의 워크스페이스에서 코드 실행함
	public CodeExecutionResponse executeCode(Long projectId, Long userId, String filePath) {
		// 1. projectId와 userId로 containerId 조회
		ActiveInstance activeInstance = activeInstanceRepository.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		String containerId = activeInstance.getContainerId();
		Image imageInfo = activeInstance.getProject().getImage();

		// 2. image 에서 명령어 템플릿 불러와 실행
		String buildCommandTemplate = imageInfo.getBuildCommand();
		// buildCommandTemplate: "javac -cp src -d {compileOutputDirectory} {filePath}"
		String runCommandTemplate = imageInfo.getRunCommand();
		// runCommandTemplate: "java -cp src {className}"

		// 3. 빌드 명령어 실행
		if (buildCommandTemplate != null && !buildCommandTemplate.isBlank()) {
			String[] finalBuildCommand = buildCommandTemplate
				.replace("{filePath}", filePath).split(" ");
			// build command ex: "javac -cp src -d bin src/com/mycompany/app/Main.java"
			// finalBuildCommand = ["javac", "-cp", "src", "-d", "bin", "src/com/mycompany/app/Main.java"]

			ExecutionResult compileResult = executeCommand(containerId, finalBuildCommand);
			log.info("[Compile] Project: {}, User: {}, ExitCode: {}", projectId, userId, compileResult.getExitCode());

			if (compileResult.getExitCode() != 0) {
				log.error("Compile failed");
				return new CodeExecutionResponse(false, "", compileResult.getStderr(), compileResult.getExitCode());
			}
		}

		// 4. 실행 명령어 실행
		String className = extractClassName(filePath);
		String[] finalRunCommand = runCommandTemplate
			.replace("{className}", className)
			.split(" ");
		// run command ex: "java -cp bin com.mycompany.app.Main"
		// finalRunCommand ex: ["javac", "-cp", "bin", "com.mycompany.app.Main"]

		ExecutionResult runResult = executeCommand(containerId, finalRunCommand);
		log.info("[Run] Project: {}, User: {}, ExitCode: {}", projectId, userId, runResult.getExitCode());

		return new CodeExecutionResponse(
			runResult.getExitCode() == 0, runResult.getStdout(), runResult.getStderr(), runResult.getExitCode()
		);

	}

	// Docker 컨테이너 내부에서 명령어 실행하고 결과 반환
	public ExecutionResult executeCommand(String containerId, String... command) {
		DockerClient dockerClient = dockerClientFactory.buildDockerClient();

		try {
			log.info("Executing command: {}", String.join(" ", command));

			// 1. Exec 인스턴스 생성
			ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
				.withAttachStdout(true)
				.withAttachStderr(true)
				.withWorkingDir("/app")
				.withCmd(command)
				.exec();
			// cli 명령어: "docker exec <containerId> javac -cp ...."

			ByteArrayOutputStream stdout = new ByteArrayOutputStream();
			ByteArrayOutputStream stderr = new ByteArrayOutputStream();

			// 2. Exec 인스턴스 시작 및 결과 수신 : 위 cli 명령어 실행
			dockerClient.execStartCmd(execCreateCmdResponse.getId())
				.exec(new ResultCallback.Adapter<Frame>() {
					@Override
					public void onNext(Frame frame) {
						try {
							if (StreamType.STDOUT.equals(frame.getStreamType())) {
								stdout.write(frame.getPayload());
							} else {
								stderr.write(frame.getPayload());
							}
						} catch (IOException e) {
							log.error("Error writing to stream for container " + containerId, e);
						}
					}
				}).awaitCompletion(30, TimeUnit.SECONDS);

			// 3. Exec 인스턴스 검사 및 종료 코드 확인
			long exitCode = dockerClient.inspectExecCmd(execCreateCmdResponse.getId()).exec().getExitCodeLong();
			String finalStdout = stdout.toString().trim();
			String finalStderr = stderr.toString().trim();

			if (!finalStdout.isEmpty()) {
				log.info("Container [{}] STDOUT: {}", containerId.substring(0, 12), finalStdout);
			}
			if (!finalStderr.isEmpty()) {
				log.error("Container [{}] STDERR: {}", containerId.substring(0, 12), finalStderr);
			}

			return new ExecutionResult(finalStdout, finalStderr, (int)exitCode);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Command execution was interrupted", e);
		} finally {
			try {
				dockerClient.close();
			} catch (IOException e) {
				log.warn("Error closing Dockerclient for container " + containerId, e);
			}
		}
	}

	// 파일 경로에서 Java 클래스 이름 추출하는 메서드
	public String extractClassName(String filePath) {
		// filePath ex: "src/com/mycompany/app/Main.java"

		String pathWithoutExtension = filePath.replace(".java", "");
		// "src/com/mycompany/app/Main"

		int srcIndex = pathWithoutExtension.lastIndexOf("src/");
		// srcIndex = 0

		String classPath;
		if (srcIndex != -1) {
			classPath = pathWithoutExtension.substring(srcIndex + 4);
			// "com/mycompany/app/Main"
		} else {
			// src 없으면 루트 패키지
			classPath = pathWithoutExtension.substring(0);
		}

		return classPath.replace("/", ".");
		// "com.mycompany.app.Main"

	}
}
