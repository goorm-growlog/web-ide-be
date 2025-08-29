package com.growlog.webide.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

public class KakaoDto {

	@Getter
	public static class OAuthTokenDto {
		@JsonProperty("access_token")
		private String accessToken;
		@JsonProperty("refresh_token")
		private String refreshToken;
		@JsonProperty("expires_in")
		private Integer expiresIn; // 액세스 토큰 만료 시간(초)

		@JsonProperty("refresh_token_expires_in")
		private Integer refreshTokenExpiresIn; // 리프레시 토큰 만료 시간(초)

		@JsonProperty("token_type")
		private String tokenType; // 토큰 타입, bearer로 고정

		private String scope; // 인증된 사용자의 정보 조회 권한 범위
	}

	@Getter
	public static class KakaoProfile {
		private Long id; // 회원번호(카카오)

		@JsonProperty("connected_at")
		private String connectedAt;

		private Properties properties;

		@JsonProperty("kakao_account")
		private KakaoAccount kakaoAccount; // 카카오계정 정보

		@Getter
		public static class Properties {
			private String nickname;

			@JsonProperty("profile_image")
			private String profileImage;

			@JsonProperty("thumbnail_image")
			private String thumbnailImage;
		}

		@Getter
		public static class KakaoAccount {
			@JsonProperty("profile_nickname_needs_agreement")
			private Boolean profileNicknameNeedsAgreement; // 사용자 동의 시 닉네임 제공 가능

			@JsonProperty("profile_image_needs_agreement")
			private Boolean profileImageNeedsAgreement; // 사용자 동의 시 프로필 사진 제공 가능

			private Profile profile; // 프로필 정보

			@JsonProperty("has_email")
			private Boolean hasEmail;

			@JsonProperty("email_needs_agreement")
			private Boolean emailNeedsAgreement; // 사용자 동의 시 카카오계정 대표 이메일 제공 가능

			@JsonProperty("is_email_valid")
			private Boolean isEmailValid; // 이메일 유효 여부

			@JsonProperty("is_email_verified")
			private Boolean isEmailVerified; // 이메일 인증 여부

			private String email; // 카카오계정 대표 이메일

			@Getter
			public static class Profile {
				private String nickname; // 닉네임 (이름 X)

				@JsonProperty("is_default_nickname")
				private Boolean isDefaultNickname; // 닉네임이 기본 닉네임인지 여부

				@JsonProperty("thumbnail_image_url")
				private String thumbnailImageUrl; // 프로필 미리보기 이미지 URL

				@JsonProperty("profile_image_url")
				private String profileImageUrl;

				@JsonProperty("is_default_image")
				private Boolean isDefaultImage;
			}
		}
	}
}
