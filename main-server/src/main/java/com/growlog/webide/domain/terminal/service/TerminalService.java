package com.growlog.webide.domain.terminal.service;

import java.util.Optional;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.projects.entity.InstanceStatus;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.terminal.dto.CodeExecutionApiRequest;
import com.growlog.webide.domain.terminal.dto.CodeExecutionRequestDto;
import com.growlog.webide.domain.terminal.dto.ContainerCreationRequest;
import com.growlog.webide.domain.terminal.dto.TerminalCommandApiRequest;
import com.growlog.webide.domain.terminal.dto.TerminalCommandRequestDto;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
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
	private final ActiveInstanceRepository activeInstanceRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;

	@Value("${code-execution.rabbitmq.exchange.name}")
	private String codeExecutionExchangeName;
	@Value("${code-execution.rabbitmq.routing.key}")
	private String codeExecutionRoutingKey;

	@Value("${terminal-command.rabbitmq.exchange.name}")
	private String terminalCommandExchangeName;
	@Value("${terminal-command.rabbitmq.routing.key}")
	private String terminalCommandRoutingKey;

	/**
	 * [Stateless] 일회성 코드 실행을 요청합니다.
	 * ActiveInstance와 무관하게, 임시 컨테이너에서 실행됩니다.
	 */
	public void requestStatelessCodeExecution(Long projectId, Long userId, CodeExecutionApiRequest apiRequest) {
		// 1. DB에서 언어에 맞는 실행 환경(이미지) 정보를 조회합니다.
		Image image = imageRepository.findByImageNameIgnoreCase(apiRequest.getLanguage())
			.orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

		// 2. Worker Server에 전달할 메시지를 생성합니다.
		CodeExecutionRequestDto messageDto = new CodeExecutionRequestDto();
		messageDto.setProjectId(projectId);
		messageDto.setUserId(userId); // 컨트롤러에서 전달받은 안전한 userId를 사용합니다.
		messageDto.setLanguage(apiRequest.getLanguage());
		messageDto.setFilePath(apiRequest.getFilePath());
		messageDto.setDockerImage(image.getDockerBaseImage()); // 2. 조회한 Docker 이미지 이름을 DTO에 추가
		messageDto.setBuildCommand(image.getBuildCommand());   // 3. 빌드 명령어 추가
		messageDto.setRunCommand(image.getRunCommand());       // 4. 실행 명령어 추가

		// 3. RabbitMQ로 메시지를 전송합니다.
		rabbitTemplate.convertAndSend(codeExecutionExchangeName, codeExecutionRoutingKey, messageDto);
		log.info("Stateless code execution request sent for project {}", projectId);
	}

	/**
	 * [Stateful] 상태 유지가 필요한 터미널 세션에 명령어를 전달합니다.
	 * 필요한 경우, 컨테이너를 생성하고 ActiveInstance에 기록합니다.
	 * @return 컨테이너 ID
	 */
	@Transactional
	public String requestStatefulTerminalCommand(Long projectId, Long userId, TerminalCommandApiRequest apiRequest) {
		// 1. Find or create an active container for the user and project.
		ActiveInstance activeInstance = findOrCreateActiveInstance(projectId, userId);
		activeInstance.updateActivity(); // ★★★ 활동 시간 갱신 ★★★
		activeInstanceRepository.save(activeInstance);

		// 2. Worker Server에 전달할 메시지를 생성합니다.
		TerminalCommandRequestDto messageDto = new TerminalCommandRequestDto();
		messageDto.setProjectId(projectId);
		messageDto.setUserId(userId);
		messageDto.setContainerId(activeInstance.getContainerId()); // ★★★ ActiveInstance의 컨테이너 ID를 사용
		messageDto.setCommand(apiRequest.getCommand());
		messageDto.setDockerImage(activeInstance.getProject().getImage().getDockerBaseImage());

		// 3. RabbitMQ로 메시지를 전송합니다.
		rabbitTemplate.convertAndSend(terminalCommandExchangeName, terminalCommandRoutingKey, messageDto);
		log.info("Stateful terminal command sent to container {}", activeInstance.getContainerId());
		return activeInstance.getContainerId();
	}

	/**
	 * 사용자의 명시적인 요청에 의해 터미널 컨테이너 삭제를 요청합니다.
	 */
	@Transactional
	public void requestContainerDeletion(Long projectId, Long userId) {
		ActiveInstance activeInstance = activeInstanceRepository.findByUser_UserIdAndProject_IdAndStatus(userId,
				projectId, InstanceStatus.ACTIVE)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		log.info("User {} requested to delete container {} for project {}", userId, activeInstance.getContainerId(),
			projectId);

		activeInstance.disconnect(); // 상태를 PENDING으로 변경
		activeInstanceRepository.save(activeInstance);

		rabbitTemplate.convertAndSend("container.lifecycle.exchange", "container.delete.key",
			activeInstance.getContainerId());
	}

	private ActiveInstance findOrCreateActiveInstance(Long projectId, Long userId) {
		Optional<ActiveInstance> existingInstance = activeInstanceRepository.findByUser_UserIdAndProject_IdAndStatus(
			userId, projectId, InstanceStatus.ACTIVE);

		if (existingInstance.isPresent()) {
			log.info("Found existing active instance for user {} and project {}", userId, projectId);
			return existingInstance.get();
		}

		log.info("No active instance found for user {} and project {}. Creating a new one.", userId, projectId);
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
		Users user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		ContainerCreationRequest creationRequest = new ContainerCreationRequest(projectId, userId,
			project.getImage().getDockerBaseImage());

		// RPC call to worker server to create a container and get its ID back
		// Note: RabbitMQConfig needs to be set up for RPC.
		String containerId = (String)rabbitTemplate.convertSendAndReceive("rpc.exchange", "rpc.container.create",
			creationRequest);

		if (containerId == null || containerId.isBlank()) {
			log.error("Failed to create container for project {}. Worker did not return a container ID.", projectId);
			throw new CustomException(ErrorCode.CONTAINER_CREATION_FAILED);
		}

		log.info("Container {} created by worker. Saving new ActiveInstance.", containerId);

		ActiveInstance newInstance = ActiveInstance.builder()
			.project(project)
			.user(user)
			.containerId(containerId)
			.status(InstanceStatus.ACTIVE)
			.build();

		return activeInstanceRepository.save(newInstance);
	}
}
