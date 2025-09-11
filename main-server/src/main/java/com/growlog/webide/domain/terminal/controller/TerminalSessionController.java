package com.growlog.webide.domain.terminal.controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.growlog.webide.domain.terminal.service.TerminalService;
import com.growlog.webide.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TerminalSessionController {

	private final TerminalService terminalService;

	/**
	 * 클라이언트로부터 PTY 기반의 터미널 세션 시작을 요청받습니다.
	 * @param payload projectId를 포함하는 메시지 본문
	 * @param accessor 메시지 헤더 접근자 (세션 및 사용자 정보 포함)
	 */
	@MessageMapping("/terminal/start")
	public void startTerminalSession(@Payload Map<String, Long> payload, SimpMessageHeaderAccessor accessor) {
		Long projectId = payload.get("projectId");
		accessor.getSessionAttributes().put("projectId", projectId);
		Authentication authentication = (Authentication)accessor.getUser();
		UserPrincipal userPrincipal = (UserPrincipal)authentication.getPrincipal();
		String sessionId = accessor.getSessionId();
		if (userPrincipal != null && projectId != null) {
			log.info("Received terminal start request for session: {}, project: {}", sessionId, projectId);
			terminalService.startStatefulTerminalSession(sessionId, projectId, userPrincipal.getUserId());
		}
	}

	@MessageMapping("/terminal/command")
	public void handleTerminalCommand(@Payload String command, SimpMessageHeaderAccessor accessor) {
		String sessionId = accessor.getSessionId();
		terminalService.forwardCommandToWorker(sessionId, command);
	}
}
