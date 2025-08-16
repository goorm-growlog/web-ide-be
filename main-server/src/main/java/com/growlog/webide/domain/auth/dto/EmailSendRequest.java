package com.growlog.webide.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "이메일 전송 요청 DTO")
public class EmailSendRequest {
	@Schema(description = "이메일", example = "hong@gmail.com")
	private String email;
}
