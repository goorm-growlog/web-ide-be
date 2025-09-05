package com.growlog.webide.domain.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growlog.webide.domain.auth.dto.GithubDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class GithubOAuth {

	@Value("${spring.github.oauth.client-id}")
	private String clientId;
	@Value("${spring.github.oauth.client-secret}")
	private String clientSecret;
	@Value("${spring.github.oauth.redirect-uri}")
	private String redirectUri;

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	public String responseUrl() {
		return UriComponentsBuilder.fromHttpUrl("https://github.com/login/oauth/authorize")
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.build(true)
			.toUriString();

	}

	// 토큰 발급 요청
	public GithubDto.OAuthTokenDto requestAccessToken(String code) {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("client_id", clientId);
		params.add("client_secret", clientSecret);
		params.add("code", code);
		params.add("redirect_uri", "http://localhost:8080/auth/github/callback");

		// 위 body와 header 연결
		HttpEntity<MultiValueMap<String, String>> githubTokenRequest = new HttpEntity<>(params, headers);

		// GithubAuthServer에 요청
		ResponseEntity<byte[]> response = restTemplate.exchange(
			"https://github.com/login/oauth/access_token",
			HttpMethod.POST,
			githubTokenRequest,
			byte[].class
		);
		String body = new String(response.getBody(), StandardCharsets.UTF_8);
		log.warn("response: {}", new String(response.getBody(), StandardCharsets.UTF_8));

		// response -> DTO 클래스로 변환
		GithubDto.OAuthTokenDto oAuthToken = null;
		try {
			oAuthToken = objectMapper.readValue(body, GithubDto.OAuthTokenDto.class);
		} catch (JsonProcessingException e) {
			// throw new CustomException(ErrorCode.PARSING_ERROR);
			throw new RuntimeException("requestAccessToken JsonProcessingException", e);
		}
		return oAuthToken;
	}

	// 사용자 정보 조회 - 요청: 액세스 토큰 방식
	public GithubDto.UserDto requestProfile(GithubDto.OAuthTokenDto token) {
		HttpHeaders headers = new HttpHeaders();

		// headers.add("Authorization", "Bearer " + token.getAccessToken());
		headers.setBearerAuth(token.getAccessToken());
		// headers.add("Content-Type", "application/json;charset=utf-8");
		headers.setContentType(MediaType.valueOf("application/json;charset=utf-8"));

		HttpEntity<MultiValueMap<String, String>> githubProfileRequest = new HttpEntity<>(headers);

		ResponseEntity<byte[]> response = restTemplate.exchange(
			"https://api.github.com/user",
			HttpMethod.GET,
			githubProfileRequest,
			byte[].class
		);

		String body = new String(response.getBody(), StandardCharsets.UTF_8);

		GithubDto.UserDto userProfile = null;

		try {
			userProfile = objectMapper.readValue(body, GithubDto.UserDto.class);
		} catch (JsonProcessingException e) {
			// throw new CustomException(ErrorCode.PARSING_ERROR);
			throw new RuntimeException("requestAccessToken JsonProcessingException", e);
		}
		return userProfile;
	}
}
