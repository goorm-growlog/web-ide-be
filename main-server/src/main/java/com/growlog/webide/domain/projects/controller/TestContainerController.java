package com.growlog.webide.domain.projects.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.projects.config.ContainerTaskProducer;
import com.growlog.webide.domain.projects.entity.ActiveSession;
import com.growlog.webide.domain.projects.repository.ActiveSessionRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "container test", description = "테스트용 컨테이너 관련 API")
@RestController
@RequestMapping("/test/projects")
public class TestContainerController {

	private final ContainerTaskProducer containerTaskProducer;
	private final ActiveSessionRepository activeSessionRepository;

	public TestContainerController(ContainerTaskProducer containerTaskProducer,
		ActiveSessionRepository activeSessionRepository) {
		this.containerTaskProducer = containerTaskProducer;
		this.activeSessionRepository = activeSessionRepository;
	}

	@Operation(summary = " [TEST] 컨테이너 할당 요청")
	@PostMapping("/acquire")
	public ResponseEntity<String> acquire(@RequestParam Long sessionId, @RequestParam Long projectId) {
		containerTaskProducer.requestContainerAcquire(sessionId, projectId);
		return ResponseEntity.ok("Acquire request sent for session: " + sessionId);
	}

	@Operation(summary = "[TEST] 컨테이너 정리 요청")
	@PostMapping("/cleanup")
	public ResponseEntity<String> releaseContainer(
		@RequestParam Long projectId
	) {
		final ActiveSession activeSession = activeSessionRepository.findByProject_Id(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));
		final Long sessionId = activeSession.getId();
		final String containerId = activeSession.getContainerId();
		// Worker 서버에 projectId에 해당하는 컨테이너를 정리하라고 요청합니다.
		containerTaskProducer.requestContainerCleanup(sessionId, projectId, containerId);

		return ResponseEntity.ok("Cleanup request sent for projectId: " + projectId);
	}

}
