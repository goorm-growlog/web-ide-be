package com.growlog.webide.domain.chats.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.growlog.webide.domain.chats.entity.Chats;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.users.entity.Users;

public record ChattingResponseDto(
	Long chatId,
	ChatType messageType,
	Long projectId,
	Long userId,
	String username,
	String profileImageUrl,
	String content,
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	Instant sentAt
) {
	public ChattingResponseDto(Long chatId, ChatType messageType, Long projectId, Long userId, String username,
		String profileImageUrl, String content) {
		this(chatId, messageType, projectId, userId, username, profileImageUrl, content, Instant.now());
	}

	public static ChattingResponseDto from(Chats chat) {
		return new ChattingResponseDto(
			chat.getChatId(),
			com.growlog.webide.domain.chats.dto.ChatType.TALK,
			chat.getProject().getId(),
			chat.getUser().getUserId(),
			chat.getUser().getName(),
			null,
			chat.getContent(),
			chat.getSentAt()
		);
	}

	public Chats toEntity(Project project, Users user) {

		return new Chats(
			project,
			user,
			this.content,
			this.sentAt
		);
	}
}
