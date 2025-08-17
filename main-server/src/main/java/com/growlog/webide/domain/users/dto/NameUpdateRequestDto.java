package com.growlog.webide.domain.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class NameUpdateRequestDto {
	@Schema(description = "사용자 이름", example = "홍길동")
	@NotBlank(message = "Username is required.")
	private String name;
}
