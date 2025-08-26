package com.growlog.webide.domain.auth.dto;

import lombok.Getter;

public class KakaoDto {

	@Getter
	public static class oAuthTokenDto {
		private String access_token;
		private String refresh_token;
		private Integer expires_in;                // 액세스 토큰 만료 시간(초)
		private Integer refresh_token_expires_in;  // 리프레시 토큰 만료 시간(초)
		private String token_type;                 // 토큰 타입, bearer로 고정
		private String scope;                        // 인증된 사용자의 정보 조회 권한 범위
	}

	@Getter
	public static class KakaoProfile {
		private Long id;                            // 회원번호(카카오)
		private String connected_at;
		private Properties properties;
		private KakaoAccount kakao_account;         // 카카오계정 정보

		@Getter
		public static class Properties {
			private String nickname;
		}

		@Getter
		public static class KakaoAccount {
			private Boolean profile_nickname_needs_agreement;    // 사용자 동의 시 닉네임 제공 가능
			private Boolean profile_image_needs_agreement;       // 사용자 동의 시 프로필 사진 제공 가능
			private Profile profile;                             // 프로필 정보
			private Boolean has_email;
			private Boolean email_needs_agreement;     // 사용자 동의 시 카카오계정 대표 이메일 제공 가능
			private Boolean is_email_valid;          // 이메일 유효 여부 (true: 유효, false: 이메일이 다른 카카오계정에 사용돼 만료)
			private Boolean is_email_verified;         // 이메일 인증 여부
			private String email;                    // 카카오계정 대표 이메일

			@Getter
			public static class Profile {
				private String nickname;             // 닉네임 (이름 X)
				private Boolean is_default_nickname; // 닉네임이 기본 닉네임인지 여부
				private String thumbnail_image_url;  // 프로필 미리보기 이미지 URL (110px * 110px 또는 100px * 100px)
			}
		}
	}
}
