package com.growlog.webide.domain.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class NameUpdateRequestDto {
	@Schema(description = "사용자 이름", example = "홍길동")
	private String name;
}
