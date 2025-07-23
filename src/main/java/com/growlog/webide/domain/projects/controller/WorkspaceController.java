package com.growlog.webide.domain.projects.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.projects.dto.CreateProjectRequest;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.service.WorkspaceManagerService;
import com.growlog.webide.domain.users.entity.User;
import com.growlog.webide.domain.users.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "workspaces", description = "워크스페이스(프로젝트) 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class WorkspaceController {

	private final WorkspaceManagerService workspaceManagerService;
	private final UserRepository userRepository;

	@Operation(summary = "프로젝트 생성", description = "새로운 프로젝트와 Docker 볼륨을 생성합니다.")
	@PostMapping
	public ResponseEntity<Project> createProject(
		@RequestBody CreateProjectRequest request
	) {
		User owner = userRepository.findById(1L)
			.orElseThrow(() -> new IllegalArgumentException("Test user(1) not found."));

		Project createdProject = workspaceManagerService.createProject(request, owner);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
	}

}
