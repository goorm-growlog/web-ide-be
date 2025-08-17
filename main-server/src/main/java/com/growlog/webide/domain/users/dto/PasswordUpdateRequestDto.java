package com.growlog.webide.domain.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordUpdateRequestDto {

	@Schema(description = "현재 비밀번호", example = "currentPassword123")
	@NotBlank(message = "Current password is required.")
	private String currentPassword;

	@Schema(description = "새로운 비밀번호", example = "newSecurePassword456")
	@NotBlank(message = "New password is required.")
	private String newPassword;
}
