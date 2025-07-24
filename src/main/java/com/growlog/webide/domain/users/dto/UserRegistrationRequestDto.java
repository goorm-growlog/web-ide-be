package com.growlog.webide.domain.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationRequestDto {

	@Schema(description = "이메일 주소", example = "hong@gmail.com")
	private String email;

	@Schema(description = "비밀번호", example = "securePassword123")
	private String password;

	@Schema(description = "사용자 이름", example = "홍길동")
	private String username;
}
