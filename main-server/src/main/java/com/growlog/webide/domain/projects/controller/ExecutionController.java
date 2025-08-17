package com.growlog.webide.domain.projects.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.projects.dto.CodeExecutionRequest;
import com.growlog.webide.domain.projects.dto.CodeExecutionResponse;
import com.growlog.webide.domain.projects.service.ExecutionService;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Executions", description = "코드 실행 관련 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class ExecutionController {
	private final ExecutionService executionService;

	@Operation(summary = "프로젝트 코드 실행",
		description = """
			해당 워크스페이스에서 지정된 파일을 컴파일하고 실행합니다. \n
			- filePath ex: src/project/Main.java""")
	@PostMapping("/{projectId}/run")
	public ResponseEntity<ApiResponse<CodeExecutionResponse>> executeCode(
		@PathVariable Long projectId,
		@RequestBody CodeExecutionRequest request,
		@AuthenticationPrincipal UserPrincipal userPrincipal
	) {
		Long userId = userPrincipal.getUserId();
		CodeExecutionResponse response = executionService.executeCode(projectId, userId, request.getFilePath());

		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
