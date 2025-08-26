package com.growlog.webide.workers.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.growlog.webide.workers.execution.dto.CodeExecutionRequestDto;
import com.growlog.webide.workers.execution.dto.TerminalCommandRequestDto;
import com.growlog.webide.workers.execution.service.ContainerExecutionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionConsumer {

	private final ContainerExecutionService containerExecutionService;

	@RabbitListener(queues = "${code-execution.rabbitmq.queue.name}")
	public void handleCodeExecution(CodeExecutionRequestDto request) {
		log.info("Received code execution request: projectId={}, image={}", request.getProjectId(),
			request.getDockerImage());
		// To-Do: 요청에 포함된 userId와 projectId를 기반으로 권한 검증 로직 추가
		containerExecutionService.executeCode(request);
	}

	@RabbitListener(queues = "${terminal-command.rabbitmq.queue.name}")
	public void handleTerminalCommand(TerminalCommandRequestDto request) {
		log.info("Received terminal creation request: projectId={}, image={}", request.getProjectId(),
			request.getDockerImage());
		// WebSocket 구현 전까지, 터미널 컨테이너 생성 및 기본 명령어 실행을 테스트합니다.
		containerExecutionService.testTerminalContainer(request);
	}
}
