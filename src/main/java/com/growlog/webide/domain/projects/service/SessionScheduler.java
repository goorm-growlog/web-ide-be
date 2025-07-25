package com.growlog.webide.domain.projects.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionScheduler {
	private final TaskScheduler taskScheduler;
	private final WorkspaceManagerService workspaceManagerService;

	// 예약된 작업을 저장할 Map
	// Key: "user:{userId}:project:{projectId}", Value: ScheduledFuture
	private final Map<String, ScheduledFuture> scheduledTasks = new ConcurrentHashMap<>();

	// 지연 시간 10분
	private static final long DELETION_DELAY_MINUTES = 10;

	/**
	* 지정된 컨테이너를 10분 뒤에 삭제하도록 예약
	* @param containerId 삭제할 컨테이너 Id
	* @param userId 작업을 식별하기 위한 사용자 Id
	 * @param projectId 작업을 식별하기 위한 사용자 Id
	 */
	public void scheduleDeletion(String containerId, Long userId, Long projectId) {
		String taskKey = createTaskKey(userId, projectId);

		cancelDeletion(taskKey);

		// 예약 실행 시점 계산
		Instant executionTime = Instant.now().plusSeconds(DELETION_DELAY_MINUTES);

		// 실행할 작업(Task) 정의
		Runnable deletionTask = () -> {
			log.info("Execution scheduled deletion for taskKey: {}", taskKey);
			try {
				workspaceManagerService.deleteContainer(containerId); // 실제로 컨테이너 삭제하는 서비스 로직
			} catch (Exception e) {
				log.error("Error during scheduled deletion for containerId: '{}': {}", containerId, e.getMessage(), e);
			} finally {
				scheduledTasks.remove(taskKey);
			}
		};

		// 스케줄러에 작업 예약
		ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(deletionTask, executionTime);

		// 예약 정보를 Map에 저장
		scheduledTasks.put(taskKey, scheduledFuture);
		log.info("Scheduled deletion for container '{}' with taskKey '{}' at {}", containerId, taskKey, executionTime);
	}

	/**
	 * 예약된 컨테이너 삭제 작업 취소
	 * @param userId 취소할 작업의 사용자 ID
	 * @param projectId 취소할 작업의 프로젝트 ID
	 * */
	public void cancelDeletion(Long userId, Long projectId) {
		String taskKey = createTaskKey(userId, projectId);
		ScheduledFuture<?> scheduledFuture = scheduledTasks.get(taskKey);

		if (scheduledFuture != null) {
			boolean cancelled = scheduledFuture.cancel(true);
			if (cancelled) {
				scheduledTasks.remove(taskKey);
				log.info("Cancelled scheduled deletion for taskKey '{}'", taskKey);
			} else {
				log.warn("Couldn't cancel scheduled deletion for taskKey '{}'", taskKey);
			}
		}
	}

	// Map에서 예약 취소하는 내부 메소드
	private void cancelDeletion(String taskKey) {
		ScheduledFuture<?> scheduledFuture = scheduledTasks.get(taskKey);
		if (scheduledFuture != null) {
			scheduledFuture.cancel(false);
			scheduledTasks.remove(taskKey);
			log.info("Replaced previos scheduled deletion for taskKey '{}'", taskKey);
		}
	}


	// 예약 작업 고유 키 생성
	private String createTaskKey(Long userId, Long projectId) {
		return String.format("user:%d:project:%d", userId, projectId);
	}
}
