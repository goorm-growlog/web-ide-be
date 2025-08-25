package com.growlog.webide.domain.terminal.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.terminal.dto.CodeExecutionRequestDto;
import com.growlog.webide.domain.terminal.dto.TerminalCommandRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TerminalService {
	private final RabbitTemplate rabbitTemplate;

	@Value("${code-execution.rabbitmq.exchange.name}")
	private String codeExecutionExchangeName;
	@Value("${code-execution.rabbitmq.routing.key}")
	private String codeExecutionRoutingKey;

	@Value("${terminal-command.rabbitmq.exchange.name}")
	private String terminalCommandExchangeName;
	@Value("${terminal-command.rabbitmq.routing.key}")
	private String terminalCommandRoutingKey;

	public void sendCodeExecutionRequest(Long projectId, CodeExecutionRequestDto requestDto) {
		// DTO에 projectId를 추가하여 메시지 큐에 보냅니다.
		// DTO에 projectId가 포함되어 있지 않으므로, 아래와 같이 DTO에 직접 값을 설정하거나,
		// DTO를 조합하는 새로운 클래스를 만들어 보낼 수 있습니다.
		// 여기서는 예시를 위해 DTO에 setter가 있다고 가정합니다.
		requestDto.setProjectId(projectId);

		rabbitTemplate.convertAndSend(codeExecutionExchangeName, codeExecutionRoutingKey, requestDto);
		System.out.println("Code execution request message sent for project " + projectId);
	}

	public void sendTerminalCommand(Long projectId, TerminalCommandRequestDto requestDto) {
		// 터미널 DTO에 projectId를 추가합니다.
		requestDto.setProjectId(projectId);

		rabbitTemplate.convertAndSend(terminalCommandExchangeName, terminalCommandRoutingKey, requestDto);
		System.out.println("Terminal command request message sent for project " + projectId);
	}
}
