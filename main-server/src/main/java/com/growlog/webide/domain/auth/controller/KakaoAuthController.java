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
import com.growlog.webide.domain.auth.util.KakaoOAuth;
import com.growlog.webide.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
			**브라우저에서만 사용하세요.** \s
			이 엔드포인트로 GET 요청 시 `302 Found`로 카카오 로그인 페이지로 이동합니다. \s
			[브라우저에서 바로 열기](/auth/kakao)
			""")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "302",
			description = "카카오 로그인 페이지로 리다이렉트",
			headers = {
				@Header(
					name = "Location",
					description = "카카오 인증 페이지 URL",
					schema = @Schema(type = "string"))
			})
	})
	@GetMapping("/kakao")
	public void kakaoAuthUrl(HttpServletResponse response) throws IOException {
		response.sendRedirect(kakaoOAuth.responseUrl());
	}

	@Operation(summary = "카카오 로그인",
		description = """
			카카오 로그인 및 필수 제공 동의 후 인가 코드 발급, 토큰 발급, 로그인/가입 처리\n
			Swagger가 아닌 아래 카카오 로그인 화면에서 테스트 가능합니다.\n
			[브라우저에서 바로 열기](/auth/kakao)""")
	@GetMapping("/login/kakao")
	public ResponseEntity<ApiResponse<LoginResponseDto>> kakaoLogin(@RequestParam("code") String code,
		HttpServletResponse response) throws IOException {
		// 로그인 요청
		RotatedTokens tokens = authService.kakaoLogin(code);

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
