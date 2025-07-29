package com.growlog.webide.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.auth.dto.LoginRequestDto;
import com.growlog.webide.domain.auth.dto.LoginResponseDto;
import com.growlog.webide.domain.auth.service.AuthService;
import com.growlog.webide.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "auth API - 회원 인증", description = "로그인 및 로그아웃 기능을 제공.")
public class AuthController {

	private final AuthService authService;

	//로그인
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto requestDto,
		HttpServletRequest request) {
		LoginResponseDto response = authService.login(requestDto, request);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@PostMapping("/logout")
	public ApiResponse<String> logout(Authentication authentication) {
		if (authentication != null) {
			String email = authentication.getName(); // JWT에서 추출한 email
			log.info("로그아웃 요청 : {}", email);
		}

		// 토큰 무효화는 JWT의 경우 서버에서 관리하지 않으므로, 클라이언트 측에서 토큰을 삭제하는 방식으로 처리(프론트에서 토큰 삭제 조치)
		return ApiResponse.ok("로그아웃 되었습니다.");
	}
}
