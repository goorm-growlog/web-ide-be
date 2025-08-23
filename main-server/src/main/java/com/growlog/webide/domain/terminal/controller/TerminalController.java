package com.growlog.webide.domain.terminal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.terminal.dto.CodeExecutionApiRequest;
import com.growlog.webide.domain.terminal.dto.TerminalCommandApiRequest;
import com.growlog.webide.domain.terminal.service.TerminalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class TerminalController {

	private final TerminalService terminalService;

	@PostMapping("/{projectId}/run/code")
	public ResponseEntity<String> runCode(
		@PathVariable Long projectId,
		@RequestBody CodeExecutionApiRequest requestDto
	) {
		terminalService.sendCodeExecutionRequest(projectId, requestDto);
		return ResponseEntity.ok("Code execution request sent to worker");
	}

	@PostMapping("/{projectId}/run/terminal")
	public ResponseEntity<String> runTerminalCommand(
		@PathVariable Long projectId,
		@RequestBody TerminalCommandApiRequest requestDto
	) {
		terminalService.sendTerminalCommand(projectId, requestDto);
		return ResponseEntity.ok("Terminal command request sent to worker.");
	}
}
