package com.growlog.webide.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

public class GithubDto {

	@Getter
	public static class OAuthTokenDto {
		@JsonProperty("access_token")
		private String accessToken;
		@JsonProperty("expires_in")
		private Integer expiresIn;
		@JsonProperty("refresh_token")
		private String refreshToken;
		@JsonProperty("refresh_token_expires_in")
		private Integer refreshTokenExpiresIn;
		private String scope;            // user:email
		@JsonProperty("token_type")
		private String tokenType;        // bearer
	}

	@Getter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class UserDto {
		private String login;
		private Integer id;
		@JsonProperty("avatar_url")
		private String avatarUrl;
		private String name;
		private String email;
	}
}
