package com.growlog.webide.domain.chats.dto;

public record ChattingResponseDto(
	ChatType messageType,
	Long projectId,
	String username,
	String content
) {
}
