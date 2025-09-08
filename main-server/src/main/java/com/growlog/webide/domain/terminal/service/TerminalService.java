package com.growlog.webide.domain.terminal.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.growlog.webide.domain.terminal.entity.ActiveInstance;
import com.growlog.webide.domain.terminal.repository.ActiveInstanceRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TerminalService {

	private final RabbitTemplate rabbitTemplate;
	private final RabbitTemplate rpcRabbitTemplate;
	private final ImageRepository imageRepository;
	private final ActiveInstanceRepository activeInstanceRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final Map<String, Long> sessionToInstanceId = new ConcurrentHashMap<>();

	@Value("${code-execution.rabbitmq.exchange.name}")
	private String codeExecutionExchangeName;
	@Value("${code-execution.rabbitmq.routing.key}")
	private String codeExecutionRoutingKey;
	// [추가] RPC 관련 설정 값
	@Value("${rpc.rabbitmq.exchange.name}")
	private String rpcExchangeName;
	@Value("${rpc.rabbitmq.request-routing-key}")
	private String rpcRequestRoutingKey;

	// [추가] 컨테이너 삭제 요청을 위한 RabbitMQ 설정
	@Value("${container-lifecycle.rabbitmq.exchange.name}")
	private String containerLifecycleExchangeName;
	@Value("${container-lifecycle.rabbitmq.request.routing-key}")
	private String containerDeleteKey;

	// [신규] PTY 세션 관련 설정 값
	@Value("${pty-session.rabbitmq.start.exchange}")
	private String ptyStartExchangeName;
	@Value("${pty-session.rabbitmq.start.routing-key}")
	private String ptyStartRoutingKey;
	@Value("${pty-session.rabbitmq.command.exchange}")
	private String ptyCommandExchangeName;
	@Value("${pty-session.rabbitmq.command.routing-key}")
	private String ptyCommandRoutingKey;
	@Value("${pty-session.rabbitmq.stop.exchange}")
	private String ptyStopExchangeName;
	@Value("${pty-session.rabbitmq.stop.routing-key}")
	private String ptyStopRoutingKey;

	public TerminalService(
		@Qualifier("rabbitTemplate") RabbitTemplate rabbitTemplate,
		@Qualifier("rpcRabbitTemplate") RabbitTemplate rpcRabbitTemplate,
		ImageRepository imageRepository,
		ActiveInstanceRepository activeInstanceRepository,
		ProjectRepository projectRepository,
		UserRepository userRepository) {
		this.rabbitTemplate = rabbitTemplate;
		this.rpcRabbitTemplate = rpcRabbitTemplate;
		this.imageRepository = imageRepository;
		this.activeInstanceRepository = activeInstanceRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
	}

	/**
	 * [Stateless] 일회성 코드 실행을 요청합니다.
	 * ActiveInstance와 무관하게, 임시 컨테이너에서 실행됩니다.
	 */
	public String requestStatelessCodeExecution(Long projectId, Long userId, CodeExecutionApiRequest apiRequest) {
		// 1. DB에서 언어에 맞는 실행 환경(이미지) 정보를 조회합니다.
		Image image = imageRepository.findByImageNameIgnoreCase(apiRequest.getLanguage())
			.orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

		// [추가] 클라이언트와 Worker가 공유할 고유 실행 ID 생성
		String executionLogId = UUID.randomUUID().toString();

		// 2. Worker Server에 전달할 메시지를 생성합니다.
		CodeExecutionRequestDto messageDto = new CodeExecutionRequestDto();
		messageDto.setProjectId(projectId);
		messageDto.setUserId(userId);
		messageDto.setExecutionLogId(executionLogId); // DTO에 ID 추가
		messageDto.setLanguage(apiRequest.getLanguage());
		messageDto.setFilePath(apiRequest.getFilePath());
		messageDto.setDockerImage(image.getDockerBaseImage()); // 조회한 Docker 이미지 이름을 DTO에 추가
		messageDto.setBuildCommand(image.getBuildCommand());   // 빌드 명령어 추가
		messageDto.setRunCommand(image.getRunCommand());       // 실행 명령어 추가

		// 3. RabbitMQ로 메시지를 전송합니다.
		rabbitTemplate.convertAndSend(codeExecutionExchangeName, codeExecutionRoutingKey, messageDto);
		log.info("Stateless code execution request sent for project {} with executionLogId {}", projectId,
			executionLogId);
		return executionLogId;
	}

	/**
	 * PTY 기반의 Stateful 터미널 세션 시작을 요청합니다.
	 */
	@Transactional
	public void startStatefulTerminalSession(String sessionId, Long projectId, Long userId) {
		// 1. 기존 로직을 재사용하여 사용자의 영구 컨테이너를 찾거나 생성합니다.
		ActiveInstance activeInstance = findOrCreateActiveInstance(projectId, userId);
		activeInstance.updateActivity();
		sessionToInstanceId.put(sessionId, activeInstance.getId());

		// 2. Worker 서버에 PTY 세션 시작을 요청합니다.
		//    WebSocket 세션 ID와 컨테이너 ID를 함께 보냅니다.
		Map<String, String> message = Map.of(
			"sessionId", sessionId,
			"containerId", activeInstance.getContainerId(),
			"userId", userId.toString() // Worker가 로그 전송 시 사용하도록 userId도 전달
		);
		rabbitTemplate.convertAndSend(ptyStartExchangeName, ptyStartRoutingKey, message);
		log.info("PTY session start request sent for session {} and container {}", sessionId,
			activeInstance.getContainerId());
	}

	/**
	 * [신규] 클라이언트로부터 받은 명령어를 Worker 서버로 전달합니다.
	 */
	public void forwardCommandToWorker(String sessionId, String input) {
		Map<String, String> message = Map.of(
			"sessionId", sessionId,
			"input", input
		);
		rabbitTemplate.convertAndSend(ptyCommandExchangeName, ptyCommandRoutingKey, message);
	}

	/**
	 * WebSocket 연결 종료 시 Worker 서버에 PTY 세션 정리를 요청합니다.
	 * @param sessionId 종료할 WebSocket 세션 ID
	 */
	public void stopPtySession(String sessionId) {
		Map<String, String> message = Map.of("sessionId", sessionId);
		rabbitTemplate.convertAndSend(ptyStopExchangeName, ptyStopRoutingKey, message);
		log.info("PTY session stop request sent for session {}", sessionId);
	}

	/**
	 * [신규] WebSocket 연결 종료를 종합적으로 처리합니다.
	 * PTY 리소스 정리와 컨테이너 삭제를 모두 수행합니다.
	 * @param sessionId 종료된 WebSocket 세션 ID
	 */
	@Transactional
	public void handleSessionDisconnect(String sessionId) {
		// 1. Worker에 PTY 세션 리소스 정리를 요청합니다.
		stopPtySession(sessionId);

		// 2. 세션 ID에 매핑된 ActiveInstance를 찾아 컨테이너 삭제를 요청합니다.
		Long instanceId = sessionToInstanceId.remove(sessionId);
		if (instanceId == null) {
			log.warn("No ActiveInstance mapping found for disconnected session {}. No container to clean up.",
				sessionId);
			return;
		}

		log.info("Handling disconnect for session {}. Found mapping to ActiveInstance ID {}", sessionId, instanceId);
		activeInstanceRepository.findById(instanceId).ifPresent(instance -> {
			// 컨테이너가 ACTIVE 상태일 때만 삭제를 요청하여 중복 처리를 방지합니다.
			if (instance.getStatus() == InstanceStatus.ACTIVE) {
				log.info(
					"Requesting container deletion for instance ID {} (containerId: {}) due to session disconnect.",
					instance.getId(), instance.getContainerId());
				requestContainerDeletion(instance.getProject().getId(), instance.getUser().getUserId());
			}
		});
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

		// [수정] 하드코딩된 값을 설정 파일에서 주입받은 값으로 변경
		rabbitTemplate.convertAndSend(containerLifecycleExchangeName, containerDeleteKey,
			activeInstance.getContainerId());
	}

	/**
	 * [추가] Worker-Server로부터 컨테이너 삭제 완료 알림을 수신하는 리스너
	 * @param containerId 삭제된 컨테이너의 ID
	 */
	@Transactional
	@RabbitListener(queues = "${container-lifecycle.rabbitmq.response.queue-name}")
	public void handleContainerDeletedAck(String containerId) {
		log.info("Received deletion acknowledgement for container: {}", containerId);
		// PENDING 상태인 ActiveInstance를 containerId로 찾아 삭제합니다.
		activeInstanceRepository.findByContainerIdAndStatus(containerId, InstanceStatus.PENDING)
			.ifPresent(activeInstanceRepository::delete);
		log.info("Cleaned up ActiveInstance for container: {}", containerId);
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
		// [수정] RPC 전용으로 설정된 rpcRabbitTemplate을 사용합니다.
		String containerId = (String)rpcRabbitTemplate.convertSendAndReceive(rpcExchangeName, rpcRequestRoutingKey,
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
