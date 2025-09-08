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

		log.debug("Forwarding log for target [{}] to userId [{}]: {}", logMessage.getTargetId(), userId,
			logMessage.getContent().trim());
		messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/logs", logMessage);
	}
}
