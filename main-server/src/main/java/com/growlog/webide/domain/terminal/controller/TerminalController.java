package com.growlog.webide.domain.terminal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.terminal.dto.CodeExecutionApiRequest;
import com.growlog.webide.domain.terminal.service.TerminalService;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class TerminalController {

	private final TerminalService terminalService;

	@PostMapping("/{projectId}/run")
	public ResponseEntity<ApiResponse<String>> runCode(
		@PathVariable Long projectId,
		@RequestBody CodeExecutionApiRequest requestDto,
		@AuthenticationPrincipal UserPrincipal userPrincipal
	) {
		// 일회성 코드 실행 요청. ActiveInstance와 무관하게 동작합니다.
		String executionLogId = terminalService.requestStatelessCodeExecution(projectId, userPrincipal.getUserId(),
			requestDto);
		// [수정] 클라이언트가 로그를 필터링할 수 있도록 고유 실행 ID를 반환합니다.
		return ResponseEntity.ok(ApiResponse.ok(executionLogId));
	}

	@DeleteMapping("/{projectId}/terminal")
	public ResponseEntity<ApiResponse<String>> closeTerminal(
		@PathVariable Long projectId,
		@AuthenticationPrincipal UserPrincipal userPrincipal
	) {
		terminalService.requestContainerDeletion(projectId, userPrincipal.getUserId());
		return ResponseEntity.ok(ApiResponse.ok("Terminal session deletion request sent."));
	}
}
