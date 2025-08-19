package com.growlog.webide.domain.auth.controller;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.auth.dto.LoginRequestDto;
import com.growlog.webide.domain.auth.dto.LoginResponseDto;
import com.growlog.webide.domain.auth.dto.RotatedTokens;
import com.growlog.webide.domain.auth.dto.TokenResponse;
import com.growlog.webide.domain.auth.service.AuthService;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
		HttpServletResponse response) {

		RotatedTokens tokens = authService.login(requestDto);

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

	@PostMapping("/logout")
	public ApiResponse<String> logout(Authentication authentication) {
		if (authentication != null) {
			UserPrincipal userPrincipal = (UserPrincipal)authentication.getPrincipal();
			String email = authentication.getName(); // JWT에서 추출한 email
			log.info("Logout requested : {}", email);

			authService.logout(userPrincipal.getUserId());
		}

		// 토큰 무효화는 JWT의 경우 서버에서 관리하지 않으므로, 클라이언트 측에서 토큰을 삭제하는 방식으로 처리(프론트에서 토큰 삭제 조치)
		return ApiResponse.ok("Logged out successfully.");
	}

	@Operation(summary = "토큰 재발급")
	@Parameter(in = ParameterIn.COOKIE, name = "refresh", required = true, description = "Refresh Token(HttpOnly)")
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(
		@CookieValue("refresh") String presentedRt,
		HttpServletResponse response) {
		RotatedTokens tokens = authService.refresh(presentedRt);

		// 새 Refresh Token을 쿠키로 갱신
		ResponseCookie cookie = ResponseCookie.from("refresh", tokens.refreshToken())
			.httpOnly(true)
			.secure(true)
			.sameSite("Lax")
			.path("/auth")
			.build();
		response.addHeader("Set-Cookie", cookie.toString());

		TokenResponse tokenResponse = new TokenResponse(tokens.accessToken());
		return ResponseEntity.ok(ApiResponse.ok(tokenResponse));
	}
}
