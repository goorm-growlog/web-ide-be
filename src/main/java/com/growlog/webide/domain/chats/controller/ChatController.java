package com.growlog.webide.domain.chats.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.service.ChatService;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
	private final UserRepository userRepository;

	@MessageMapping("/projects/{projectId}/chat/enter")
	@SendTo("/topic/projects/{projectId}/chat")
	public ChattingResponseDto enter(
		@DestinationVariable Long projectId,
		StompHeaderAccessor accessor
	) {
		Authentication userToken = (Authentication)accessor.getUser();

		final UserPrincipal principal = (UserPrincipal)userToken.getPrincipal();
		final Long userId = principal.getUserId();

		final Users user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

		final String username = user.getName();

		return chatService.enter(projectId, username);
	}
}
