package com.growlog.webide.domain.terminal.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.terminal.dto.CodeExecutionApiRequest;
import com.growlog.webide.domain.terminal.dto.CodeExecutionRequestDto;
import com.growlog.webide.domain.terminal.dto.TerminalCommandApiRequest;
import com.growlog.webide.domain.terminal.dto.TerminalCommandRequestDto;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminalService {
	private final RabbitTemplate rabbitTemplate;
	private final ImageRepository imageRepository;

	@Value("${code-execution.rabbitmq.exchange.name}")
	private String codeExecutionExchangeName;
	@Value("${code-execution.rabbitmq.routing.key}")
	private String codeExecutionRoutingKey;

	@Value("${terminal-command.rabbitmq.exchange.name}")
	private String terminalCommandExchangeName;
	@Value("${terminal-command.rabbitmq.routing.key}")
	private String terminalCommandRoutingKey;

	public void sendCodeExecutionRequest(Long projectId, Long userId, CodeExecutionApiRequest apiRequest) {
		// 1. DB에서 언어에 맞는 실행 환경(이미지) 정보를 조회합니다.
		Image image = imageRepository.findByImageNameIgnoreCase(apiRequest.getLanguage())
			.orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

		// API 요청 DTO를 내부 메시징 DTO로 변환하고, projectId를 설정합니다.
		CodeExecutionRequestDto messageDto = new CodeExecutionRequestDto();
		messageDto.setProjectId(projectId);
		messageDto.setUserId(userId); // 컨트롤러에서 전달받은 안전한 userId를 사용합니다.
		messageDto.setLanguage(apiRequest.getLanguage());
		messageDto.setFilePath(apiRequest.getFilePath());
		messageDto.setDockerImage(image.getDockerBaseImage()); // 2. 조회한 Docker 이미지 이름을 DTO에 추가

		rabbitTemplate.convertAndSend(codeExecutionExchangeName, codeExecutionRoutingKey, messageDto);
		log.info("Code execution request sent: projectId={}, userId={}, language={}, image={}, filePath={}",
			messageDto.getProjectId(), messageDto.getUserId(), messageDto.getLanguage(), messageDto.getDockerImage(),
			messageDto.getFilePath());
	}

	public void sendTerminalCommand(Long projectId, Long userId, TerminalCommandApiRequest apiRequest) {
		// DB에서 터미널 전용 실행 환경(이미지) 정보를 조회합니다. (예: imageName이 "terminal"인 이미지)
		// data.sql에 'terminal' 이름으로 이미지를 추가해야 합니다.
		Image terminalImage = imageRepository.findByImageNameIgnoreCase("terminal")
			.orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

		// API 요청 DTO를 내부 메시징 DTO로 변환하고, projectId를 설정합니다.
		TerminalCommandRequestDto messageDto = new TerminalCommandRequestDto();
		messageDto.setProjectId(projectId);
		messageDto.setUserId(userId); // 컨트롤러에서 전달받은 안전한 userId를 사용합니다.
		messageDto.setCommand(apiRequest.getCommand());
		messageDto.setDockerImage(terminalImage.getDockerBaseImage());

		rabbitTemplate.convertAndSend(terminalCommandExchangeName, terminalCommandRoutingKey, messageDto);
		log.info("Terminal command request sent: projectId={}, userId={}, image={}, command='{}'",
			messageDto.getProjectId(), messageDto.getUserId(), messageDto.getDockerImage(), messageDto.getCommand());
	}
}
