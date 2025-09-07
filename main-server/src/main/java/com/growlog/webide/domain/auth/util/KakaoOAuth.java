package com.growlog.webide.domain.auth.util;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growlog.webide.domain.auth.dto.KakaoDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class KakaoOAuth {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	@Value("${spring.kakao.oauth.rest-api-key}")
	private String restAPiKey;
	@Value("${spring.kakao.oauth.redirect-uri}")
	private String redirectUri;

	public String responseUrl() {
		return UriComponentsBuilder.fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
			.queryParam("client_id", restAPiKey)
			.queryParam("redirect_uri", redirectUri) // 자동 인코딩
			.queryParam("response_type", "code")
			.build(true)
			.toUriString();
	}

	// 토큰 발급 요청
	public KakaoDto.OAuthTokenDto requestAccessToken(String code) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", restAPiKey);
		params.add("redirect_uri", redirectUri);
		params.add("code", code);

		// 위 body와 header 연결
		HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);

		// KakaoAuthServer에 요청
		ResponseEntity<byte[]> response = restTemplate.exchange(
			"https://kauth.kakao.com/oauth/token",
			HttpMethod.POST,
			kakaoTokenRequest,
			byte[].class
		);
		String body = new String(response.getBody(), StandardCharsets.UTF_8);
		// response -> DTO 클래스로 변환
		KakaoDto.OAuthTokenDto oAuthToken = null;
		try {
			oAuthToken = objectMapper.readValue(body, KakaoDto.OAuthTokenDto.class);
			log.info("oAuthToken : " + oAuthToken.getAccessToken());
		} catch (JsonProcessingException e) {
			// throw new CustomException(ErrorCode.PARSING_ERROR);
			throw new RuntimeException("requestAccessToken JsonProcessingException", e);
		}
		return oAuthToken;
	}

	// 사용자 정보 조회 - 요청: 액세스 토큰 방식
	public KakaoDto.KakaoProfile requestProfile(KakaoDto.OAuthTokenDto token) {
		HttpHeaders headers = new HttpHeaders();

		headers.add("Authorization", "Bearer " + token.getAccessToken());
		headers.add("Content-Type", "application/json;charset=utf-8");

		HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest = new HttpEntity<>(headers);

		ResponseEntity<byte[]> response = restTemplate.exchange(
			"https://kapi.kakao.com/v2/user/me",
			HttpMethod.GET,
			kakaoProfileRequest,
			byte[].class
		);

		String body = new String(response.getBody(), StandardCharsets.UTF_8);
		log.info(body);

		KakaoDto.KakaoProfile kakaoProfile = null;

		try {
			kakaoProfile = objectMapper.readValue(body, KakaoDto.KakaoProfile.class);
		} catch (JsonProcessingException e) {
			// throw new CustomException(ErrorCode.PARSING_ERROR);
			throw new RuntimeException("requestAccessToken JsonProcessingException", e);
		}
		return kakaoProfile;
	}
}
