package com.growlog.webide.domain.auth.controller;

import java.io.IOException;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.auth.dto.LoginResponseDto;
import com.growlog.webide.domain.auth.dto.RotatedTokens;
import com.growlog.webide.domain.auth.service.AuthService;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.util.KakaoOAuth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "auth API - 소셜 로그인")
public class KakaoAuthController {
	private final KakaoOAuth kakaoOAuth;
	private final AuthService authService;

	@Operation(summary = "카카오 로그인 화면으로 redirect",
		description = """
						'카카오 로그인' 버튼 클릭 시 호출 \n
						카카오 로그인 화면 열림
			""")
	@GetMapping("/kakao")
	public void kakaoAuthUrl(HttpServletResponse response) throws IOException {
		response.sendRedirect(kakaoOAuth.responseUrl());
	}

	@Operation(summary = "카카오 로그인",
		description = "카카오 로그인 및 필수 제공 동의 후 인가 코드 발급, 토큰 발급, 로그인/가입 처리")
	@GetMapping("/login/kakao")
	public ResponseEntity<ApiResponse<LoginResponseDto>> kakaoLogin(@RequestParam("code") String code,
		HttpServletResponse response) throws IOException {
		// 로그인 요청
		RotatedTokens tokens = authService.kakaoLogin(code, response);

		ResponseCookie cookie = ResponseCookie.from("refresh", tokens.refreshToken())
			.httpOnly(true)
			.secure(true)
			.sameSite("Lax")
			.path("/auth")
			.build();
		response.addHeader("Set-Cookie", cookie.toString());

		LoginResponseDto loginResponse = new LoginResponseDto(tokens.userId(), tokens.name(), tokens.accessToken());

		return ResponseEntity.ok(ApiResponse.ok(loginResponse));
	}

}
