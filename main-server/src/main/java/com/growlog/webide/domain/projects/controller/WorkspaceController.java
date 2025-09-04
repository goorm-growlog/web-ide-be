package com.growlog.webide.domain.projects.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.projects.dto.CreateProjectRequest;
import com.growlog.webide.domain.projects.dto.ProjectResponse;
import com.growlog.webide.domain.projects.dto.UpdateProjectRequest;
import com.growlog.webide.domain.projects.service.WorkspaceManagerService;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "workspaces", description = "워크스페이스(프로젝트) 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class WorkspaceController {

	private final WorkspaceManagerService workspaceManagerService;

	@Operation(summary = "프로젝트 생성", description = "새로운 프로젝트와 Docker 볼륨을 생성합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
		@RequestBody CreateProjectRequest request,
		@AuthenticationPrincipal UserPrincipal userPrincipal
	) {
		ProjectResponse response = workspaceManagerService.createProject(request, userPrincipal.getUserId());
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@Operation(summary = "프로젝트 열기(컨테이너 실행)", description = "사용자를 위한 개인 컨테이너를 실행하고 접속 정보를 반환합니다.")
	@PostMapping("/{projectId}/open")
	public ResponseEntity<ApiResponse<String>> openProject(
		@PathVariable Long projectId,
		@AuthenticationPrincipal UserPrincipal userPrincipal
	) {
		Long userId = userPrincipal.getUserId();
		workspaceManagerService.openProject(projectId, userId);
		return ResponseEntity.ok(ApiResponse.ok("Complete Opening"));
	}

	@Operation(summary = "프로젝트 삭제",
		description = "프로젝트 정보와 Docker 볼륨을 삭제합니다.")
	@DeleteMapping("/{projectId}")
	public ResponseEntity<ApiResponse<String>> deleteProject(
		@PathVariable Long projectId,
		@AuthenticationPrincipal UserPrincipal userPrincipal) {
		Long userId = userPrincipal.getUserId();
		workspaceManagerService.deleteProject(projectId, userId);
		return ResponseEntity.ok(ApiResponse.ok("Complete Deletion."));
	}

	@Operation(summary = "프로젝트 정보 조회",
		description = "프로젝트의 상세 정보를 조회합니다")
	@GetMapping("/{projectId}")
	public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
		@PathVariable Long projectId,
		@AuthenticationPrincipal UserPrincipal userPrincipal) {
		Long userId = userPrincipal.getUserId();
		ProjectResponse response = workspaceManagerService.getProjectDetails(projectId, userId);

		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@Operation(summary = "프로젝트 정보 수정",
		description = "프로젝트의 이름과 설명을 수정합니다.")
	@PatchMapping("/{projectId}")
	public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
		@PathVariable Long projectId,
		@RequestBody UpdateProjectRequest request,
		@AuthenticationPrincipal UserPrincipal userPrincipal) {
		Long userId = userPrincipal.getUserId();
		ProjectResponse response = workspaceManagerService.updateProject(projectId, request, userId);

		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@Operation(summary = "내 프로젝트 목록 조회",
		description = """
			프로젝트 목록을 조회합니다. \n
			- **?type=own** : 자신이 만든 프로젝트만 필터링합니다. \n
			- **?type=joined** : 참여 중인 프로젝트만 필터링합니다. """)
	@GetMapping
	public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjectList(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@RequestParam(required = false) String type) {
		Long userId = userPrincipal.getUserId();
		List<ProjectResponse> projectList = workspaceManagerService.findProjectListByUser(userId, type);
		return ResponseEntity.ok(ApiResponse.ok(projectList));
	}
}
