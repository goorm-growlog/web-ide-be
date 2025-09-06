package com.growlog.webide.domain.auth.controller;

import java.io.IOException;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.auth.dto.GithubDto;
import com.growlog.webide.domain.auth.dto.LoginResponseDto;
import com.growlog.webide.domain.auth.dto.RotatedTokens;
import com.growlog.webide.domain.auth.service.AuthService;
import com.growlog.webide.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "auth API - 소셜 로그인")
public class GithubAuthController {

	private final AuthService authService;

	@PostMapping("/login/github")
	public ResponseEntity<ApiResponse<LoginResponseDto>> githubLogin(@RequestBody GithubDto.UserDto request,
		HttpServletResponse response) throws IOException {

		RotatedTokens tokens = authService.githubLogin(request);

		ResponseCookie cookie = ResponseCookie.from("refresh", tokens.refreshToken())
			.httpOnly(true)
			.secure(true)
			.sameSite("Lax")
			.path("/")
			.build();
		response.addHeader("Set-Cookie", cookie.toString());

		LoginResponseDto loginResponse = new LoginResponseDto(tokens.userId(), tokens.name(), tokens.accessToken());

		return ResponseEntity.ok(ApiResponse.ok(loginResponse));
	}
}
