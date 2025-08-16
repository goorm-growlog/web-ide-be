package com.growlog.webide.domain.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordVerifyRequestDto {

	@Schema(description = "현재비밀번호", example = "securePassword123")
	@NotBlank(message = "Password is required.")
	private String password;
}
