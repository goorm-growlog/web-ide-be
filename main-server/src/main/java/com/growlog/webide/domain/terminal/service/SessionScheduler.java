package com.growlog.webide.domain.terminal.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.projects.entity.InstanceStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionScheduler {
	private static final long IDLE_TIMEOUT_MINUTES = 30;
	private final ActiveInstanceRepository activeInstanceRepository;
	private final RabbitTemplate rabbitTemplate;

	/**
	 * 1분마다 실행되어 30분 이상 활동이 없는 컨테이너를 정리합니다.
	 */
	@Scheduled(fixedRate = 60000) // 1분마다 실행
	@Transactional
	public void cleanupIdleContainers() {
		log.debug("Running scheduled task to clean up idle containers...");
		LocalDateTime idleThreshold = LocalDateTime.now().minusMinutes(IDLE_TIMEOUT_MINUTES);

		List<ActiveInstance> idleInstances = activeInstanceRepository.findAllByStatusAndLastActivityAtBefore(
			InstanceStatus.ACTIVE,
			idleThreshold
		);

		if (!idleInstances.isEmpty()) {
			log.info("Found {} idle containers to clean up.", idleInstances.size());
			idleInstances.forEach(instance -> {
				log.info("Container {} has been idle since {}. Sending delete request.", instance.getContainerId(),
					instance.getLastActivityAt());
				instance.disconnect(); // 상태를 PENDING으로 변경
				activeInstanceRepository.save(instance);
				rabbitTemplate.convertAndSend("container.lifecycle.exchange", "container.delete.key",
					instance.getContainerId());
			});
		}
	}
}
