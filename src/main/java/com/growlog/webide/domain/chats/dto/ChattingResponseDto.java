package com.growlog.webide.domain.chats.dto;

import java.time.LocalDateTime;

public record ChattingResponseDto(
	ChatType messageType,
	Long projectId,
	String username,
	String content,
	LocalDateTime sendAt
) {
}
