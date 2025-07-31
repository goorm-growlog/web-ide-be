package com.growlog.webide.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "로그인 요청 DTO - 이메일과 비밀번호를 통한 로그인 요청")
public class LoginRequestDto {

	@Schema(description = "이메일", example = "hong@gmail.com")
	private String email;

	@Schema(description = "비밀번호", example = "password123")
	private String password;
}
