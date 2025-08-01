package com.growlog.webide.domain.chats.dto;

import java.time.Instant;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ChattingResponseDto(
	ChatType messageType,
	Long projectId,
	String username,
	String content,
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	Instant sentAt,
	CodeLink codeLink
) {
	public ChattingResponseDto(ChatType messageType, Long projectId, String username, String content) {
		this(messageType, projectId, username, content, Instant.now(), null);
	}

	public ChattingResponseDto(ChatType messageType, Long projectId, String username, String content,
		CodeLink codeLink) {
		this(messageType, projectId, username, content, Instant.now(), codeLink);
	}
}
