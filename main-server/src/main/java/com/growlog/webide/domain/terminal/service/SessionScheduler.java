package com.growlog.webide.domain.terminal.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.projects.entity.InstanceStatus;
import com.growlog.webide.domain.terminal.entity.ActiveInstance;
import com.growlog.webide.domain.terminal.repository.ActiveInstanceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionScheduler {
	private static final long IDLE_TIMEOUT_MINUTES = 30;
	private final ActiveInstanceRepository activeInstanceRepository;
	private final TerminalService terminalService; // [수정] RabbitTemplate 대신 TerminalService를 주입받습니다.

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
			for (ActiveInstance instance : idleInstances) {
				try {
					log.info("Requesting deletion for inactive container: {}", instance.getContainerId());
					// [수정] 중복 로직을 제거하고, 이미 구현된 TerminalService의 삭제 요청 메서드를 재사용합니다.
					terminalService.requestContainerDeletion(instance.getProject().getId(),
						instance.getUser().getUserId());
				} catch (Exception e) {
					log.error("Error requesting deletion for inactive instance with containerId: {}",
						instance.getContainerId(), e);
				}
			}
		}
	}
}
