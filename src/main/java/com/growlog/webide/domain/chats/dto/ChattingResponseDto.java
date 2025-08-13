package com.growlog.webide.domain.chats.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ChattingResponseDto(
	ChatType messageType,
	Long projectId,
	Long userId,
	String username,
	String profileImageUrl,
	String content,
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	Instant sentAt
) {
	public ChattingResponseDto(ChatType messageType, Long projectId, Long userId, String username,
		String profileImageUrl, String content) {
		this(messageType, projectId, userId, username, profileImageUrl, content, Instant.now());
	}
}
