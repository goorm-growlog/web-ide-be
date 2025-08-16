package com.growlog.webide.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "로그인 응답 DTO - 로그인 성공 시 사용자 정보와 인증 토큰 반환")
public class LoginResponseDto {

	@Schema(description = "사용자 ID", example = "1")
	private Long userId;

	@Schema(description = "이름", example = "홍길동")
	private String name;

	@Schema(description = "Access Token (Bearer 토큰 형식, Authorization 헤더에 포함)",
		example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
	private String accessToken;
}
