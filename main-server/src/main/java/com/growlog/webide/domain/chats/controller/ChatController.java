package com.growlog.webide.domain.chats.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@MessageMapping("/projects/{projectId}/chat/enter")
	@SendTo("/topic/projects/{projectId}/chat")
	public ChattingResponseDto enter(
		@DestinationVariable Long projectId,
		StompHeaderAccessor accessor
	) {
		final Long userId = Long.parseLong(accessor.getSessionAttributes().get("userId").toString());

		accessor.getSessionAttributes().put("projectId", projectId);
		log.info("accessor: {}", accessor);

		return chatService.enter(projectId, userId);
	}

	@MessageMapping("/projects/{projectId}/chat/talk")
	@SendTo("/topic/projects/{projectId}/chat")
	public ChattingResponseDto talk(
		@DestinationVariable Long projectId,
		StompHeaderAccessor accessor,
		@Payload String content
	) {
		final Long userId = Long.parseLong(accessor.getSessionAttributes().get("userId").toString());

		return chatService.talk(projectId, userId, content);
	}
}
