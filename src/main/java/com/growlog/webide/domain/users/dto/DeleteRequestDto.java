package com.growlog.webide.domain.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class DeleteRequestDto {

	@Schema(description = "회원 탈퇴를 위한 비밀번호", example = "securePassword123")
	private String password;
}
