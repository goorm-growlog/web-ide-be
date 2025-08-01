package com.growlog.webide.domain.chats.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ChattingResponseDto(
	ChatType messageType,
	Long projectId,
	String username,
	String content,
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	LocalDateTime sentAt
) {
	public ChattingResponseDto(ChatType messageType, Long projectId, String username, String content) {
		this(messageType, projectId, username, content, LocalDateTime.now());
	}
}
