package com.growlog.webide.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "비밀번호 재설정 요청 DTO - 이름, 휴대폰 번호, 이메일을 통한 비밀번호 재설정 요청")
public class ResetPasswordRequestDto {

	@Schema(description = "이름", example = "홍길동")
	private String name;

	@Schema(description = "이메일", example = "hong@gmail.com")
	private String email;
}
