package com.growlog.webide.domain.terminal.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.terminal.dto.LogMessage;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogBrokerService {

	private final SimpMessagingTemplate messagingTemplate;
	private final UserRepository userRepository; // userId로 username을 찾기 위해 필요

	/**
	 * Worker 서버로부터 로그 메시지를 수신하여 해당 사용자에게 WebSocket으로 전달합니다.
	 * @param logMessage Worker에서 보낸 로그 메시지 DTO
	 */
	@RabbitListener(queues = "${log-reception.rabbitmq.queue.name}")
	// 이 부분이 routing.key로 잘못 설정되었을 가능성이 높습니다. queue.name으로 사용해야 합니다.
	public void forwardLogToUser(LogMessage logMessage) {
		Long userId = logMessage.getUserId();
		if (userId == null) {
			log.warn("Received a log message without a userId. Cannot forward. TargetId: {}", logMessage.getTargetId());
			return;
		}

		// SimpMessagingTemplate.convertAndSendToUser는 내부적으로 STOMP 세션에 저장된 Principal의 name을 사용합니다.
		// StompHandler와 JwtTokenProvider를 보면 UserPrincipal이 생성되고, 그 Principal의 이름(username)은
		// 사용자의 이메일일 가능성이 높습니다. 따라서 DB에서 사용자의 이메일을 조회합니다.
		String username = userRepository.findById(userId)
			.map(Users::getEmail)
			.orElse(null);

		if (username == null) {
			log.warn("User not found for userId: {}. Cannot forward log for targetId: {}", userId,
				logMessage.getTargetId());
			return;
		}

		log.debug("Forwarding log for target [{}] to user [{}]: {}", logMessage.getTargetId(), username,
			logMessage.getContent().trim());
		messagingTemplate.convertAndSendToUser(username, "/queue/logs", logMessage);
	}
}
