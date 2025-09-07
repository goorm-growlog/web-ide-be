package com.growlog.webide.workers.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.growlog.webide.workers.execution.dto.CodeExecutionRequestDto;
import com.growlog.webide.workers.execution.dto.ContainerCreationRequest;
import com.growlog.webide.workers.execution.dto.TerminalCommandRequestDto;
import com.growlog.webide.workers.execution.service.ContainerExecutionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionConsumer {

	private final ContainerExecutionService containerExecutionService;

	/**
	 * [Stateless] 일회성 코드 실행 요청을 처리합니다.
	 */
	@RabbitListener(queues = "${code-execution.rabbitmq.queue.name}")
	public void handleCodeExecution(CodeExecutionRequestDto request) {
		log.info("Received code execution request: projectId={}, image={}", request.getProjectId(),
			request.getDockerImage());
		containerExecutionService.executeCode(request);
	}

	/**
	 * [Stateful] 영구 컨테이너 생성 요청을 처리합니다. (RPC)
	 */
	@RabbitListener(queues = "${rpc.rabbitmq.queue.name}")
	public String handleCreateContainerRequest(ContainerCreationRequest request) {
		log.info("Received RPC request to create persistent container for project: {}", request.getProjectId());
		return containerExecutionService.createPersistentContainer(request);
	}

	/**
	 * [Stateful] 영구 컨테이너에 터미널 명령어를 전달하는 요청을 처리합니다.
	 */
	@RabbitListener(queues = "${terminal-command.rabbitmq.queue.name}")
	public void handleTerminalCommand(TerminalCommandRequestDto request) {
		log.info("Received terminal command for container {}: {}", request.getContainerId(), request.getCommand());
		containerExecutionService.executeCommandInPersistentContainer(request);
	}
}
