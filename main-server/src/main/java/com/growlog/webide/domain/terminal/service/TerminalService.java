package com.growlog.webide.domain.terminal.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.terminal.dto.CodeExecutionApiRequest;
import com.growlog.webide.domain.terminal.dto.CodeExecutionRequestDto;
import com.growlog.webide.domain.terminal.dto.TerminalCommandApiRequest;
import com.growlog.webide.domain.terminal.dto.TerminalCommandRequestDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	public void sendCodeExecutionRequest(Long projectId, CodeExecutionApiRequest apiRequest) {
		// API 요청 DTO를 내부 메시징 DTO로 변환하고, projectId를 설정합니다.
		CodeExecutionRequestDto messageDto = new CodeExecutionRequestDto();
		messageDto.setProjectId(projectId);
		messageDto.setUserId(apiRequest.getUserId());
		messageDto.setLanguage(apiRequest.getLanguage());
		messageDto.setFilePath(apiRequest.getFilePath());

		rabbitTemplate.convertAndSend(codeExecutionExchangeName, codeExecutionRoutingKey, messageDto);
		log.info("Code execution request sent: projectId={}, userId={}, language={}, filePath={}",
			messageDto.getProjectId(), messageDto.getUserId(), messageDto.getLanguage(), messageDto.getFilePath());
	}

	public void sendTerminalCommand(Long projectId, TerminalCommandApiRequest apiRequest) {
		// API 요청 DTO를 내부 메시징 DTO로 변환하고, projectId를 설정합니다.
		TerminalCommandRequestDto messageDto = new TerminalCommandRequestDto();
		messageDto.setProjectId(projectId);
		messageDto.setUserId(apiRequest.getUserId());
		messageDto.setCommand(apiRequest.getCommand());

		rabbitTemplate.convertAndSend(terminalCommandExchangeName, terminalCommandRoutingKey, messageDto);
		log.info("Terminal command request sent: projectId={}, userId={}, command='{}'",
			messageDto.getProjectId(), messageDto.getUserId(), messageDto.getCommand());
	}
}
