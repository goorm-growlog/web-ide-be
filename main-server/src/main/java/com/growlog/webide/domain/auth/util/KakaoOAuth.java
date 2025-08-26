package com.growlog.webide.domain.auth.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growlog.webide.domain.auth.dto.KakaoDto;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class KakaoOAuth {

	@Value("${spring.kakao.oauth.rest-api-key}")
	private String restAPiKey;
	@Value("${spring.kakao.oauth.redirect-uri}")
	private String redirectUri;

	public String responseUrl() {
		String kakaoLoginUrl = "https://kauth.kakao.com/oauth/authorize?client_id=" + restAPiKey
							   + "&redirect_uri=" + redirectUri + "&response_type=code";
		return kakaoLoginUrl;
	}

	// 토큰 발급 요청
	public KakaoDto.oAuthTokenDto requestAccessToken(String code) {
		RestTemplate restTemplate = new RestTemplate();
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
		ResponseEntity<String> response = restTemplate.exchange(
			"https://kauth.kakao.com/oauth/token",
			HttpMethod.POST,
			kakaoTokenRequest,
			String.class
		);
		// response -> DTO 클래스로 변환
		ObjectMapper objectMapper = new ObjectMapper();
		KakaoDto.oAuthTokenDto oAuthToken = null;
		try {
			oAuthToken = objectMapper.readValue(response.getBody(), KakaoDto.oAuthTokenDto.class);
			log.info("oAuthToken : " + oAuthToken.getAccess_token());
		} catch (JsonProcessingException e) {
			// throw new CustomException(ErrorCode.PARSING_ERROR);
			throw new RuntimeException("requestAccessToken JsonProcessingException", e);
		}
		return oAuthToken;
	}

	// 사용자 정보 조회 - 요청: 액세스 토큰 방식
	public KakaoDto.KakaoProfile requestProfile(KakaoDto.oAuthTokenDto token) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();

		headers.add("Authorization", "Bearer " + token.getAccess_token());
		headers.add("Content-Type", "application/json;charset=utf-8");

		HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(
			"https://kapi.kakao.com/v2/user/me",
			HttpMethod.GET,
			kakaoProfileRequest,
			String.class
		);
		log.info(response.getBody());

		ObjectMapper objectMapper = new ObjectMapper();
		KakaoDto.KakaoProfile kakaoProfile = null;

		try {
			kakaoProfile = objectMapper.readValue(response.getBody(), KakaoDto.KakaoProfile.class);
		} catch (JsonProcessingException e) {
			// throw new CustomException(ErrorCode.PARSING_ERROR);
			throw new RuntimeException("requestAccessToken JsonProcessingException", e);
		}
		return kakaoProfile;
	}
}
