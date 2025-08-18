package com.growlog.webide.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "이메일 인증 확인 DTO - 이메일과 인증 코드를 통해 인증 확인 요청")
public class EmailVerifyRequest {

	@Schema(description = "이메일", example = "hong@gmail.com")
	private String email;

	@Schema(description = "인증 코드", example = "123456")
	private String code;
}
