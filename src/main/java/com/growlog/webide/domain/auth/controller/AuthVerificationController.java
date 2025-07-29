package com.growlog.webide.domain.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.auth.dto.EmailSendRequest;
import com.growlog.webide.domain.auth.dto.EmailVerifyRequest;
import com.growlog.webide.domain.auth.service.EmailVerificationService;
import com.growlog.webide.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "auth API - 본인 인증", description = "이메일 인증 기능을 제공.")
public class AuthVerificationController {
	private final EmailVerificationService emailVerificationService;

	@PostMapping("/email/send")
	public ResponseEntity<ApiResponse<Map<String, String>>> sendEmailVerificationCode(
		@RequestBody EmailSendRequest request) {
		emailVerificationService.sendVerificationEmail(request.getEmail());
		return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Verification email sent successfully")));
	}

	@PostMapping("/email/verify")
	public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyEmail(@RequestBody EmailVerifyRequest request) {
		boolean isVerified = emailVerificationService.verifyEmail(request.getEmail(), request.getCode());
		return ResponseEntity.ok(ApiResponse.ok(Map.of("verified", isVerified)));
	}

}
