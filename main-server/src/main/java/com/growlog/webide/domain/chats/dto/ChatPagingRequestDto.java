package com.growlog.webide.domain.chats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatPagingRequestDto(
	@Schema(description = "페이지 번호 (0부터 시작)", defaultValue = "0")
	int page,

	@Schema(description = "한 페이지의 데이터 개수", defaultValue = "20")
	int size
) {
}
