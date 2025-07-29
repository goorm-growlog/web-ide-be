package com.growlog.webide.domain.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class PasswordUpdateRequestDto {

	@Schema(description = "현재 비밀번호", example = "currentPassword123")
	private String currentPassword;

	@Schema(description = "새로운 비밀번호", example = "newSecurePassword456")
	private String newPassword;
}
