package com.growlog.webide.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.auth.dto.ResetPasswordRequestDto;
import com.growlog.webide.domain.auth.service.AuthRecoveryService;
import com.growlog.webide.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "auth API - 인증 복구", description = " 비밀번호 재설정 기능을 제공.")
public class AuthRecoveryController {
	private final AuthRecoveryService authRecoveryService;

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequestDto requestDto) {
		authRecoveryService.resetPassword(requestDto);
		log.info("[GrowLog - 여행서비스] 비밀번호 재설정 요청: {}", requestDto);
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
