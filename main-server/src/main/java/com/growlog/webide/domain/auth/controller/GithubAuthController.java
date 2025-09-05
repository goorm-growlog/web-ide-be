package com.growlog.webide.domain.auth.controller;

import java.io.IOException;
import java.net.URLEncoder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.growlog.webide.domain.auth.dto.RotatedTokens;
import com.growlog.webide.domain.auth.service.AuthService;
import com.growlog.webide.domain.auth.util.GithubOAuth;

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

	@Value("${spring.github.redirect-front}")
	private String redirectFrontUrl;
	private final GithubOAuth githubOAuth;
	private final AuthService authService;

	@GetMapping("/github")
	public void githubAuthUrl(HttpServletResponse response) throws IOException {
		log.info("start to github auth");
		String redirectUrl = githubOAuth.responseUrl();
		log.info("redirect url: {}", redirectUrl);
		// response.sendRedirect(redirectUrl);
	}

	// Authorization callback URL (Github 로그인 완료 시 리다이렉트)
	@GetMapping("/github/callback")
	public void githubLogin(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
		// 로그인 요청 (access token 발급 및 반환)
		RotatedTokens tokens = authService.githubLogin(code);

		// refresh token 쿠키
		ResponseCookie cookie = ResponseCookie.from("refresh", tokens.refreshToken())
			.httpOnly(true)
			.secure(true)
			.sameSite("Lax")
			.path("/")
			.build();
		response.addHeader("Set-Cookie", cookie.toString());

		// 로그인 완료 페이지 리다이렉트
		String redirectUrl = UriComponentsBuilder.fromHttpUrl(redirectFrontUrl)
			.queryParam("token", tokens.accessToken())
			.queryParam("userId", tokens.userId())
			.queryParam("name", URLEncoder.encode(tokens.name(), "UTF-8"))
			.build(false)
			.toUriString();
		response.sendRedirect(redirectUrl);
	}
}
