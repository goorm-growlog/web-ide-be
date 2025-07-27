package com.growlog.webide.domain.chats.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.service.ChatService;
import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
	private final UserRepository userRepository;
	private final ProjectMemberRepository projectMemberRepository;

	@MessageMapping("/projects/{projectId}/chat/enter")
	@SendTo("/topic/projects/{projectId}/chat")
	public ChattingResponseDto enter(
		@DestinationVariable Long projectId,
		StompHeaderAccessor accessor
	) {
		Authentication userToken = (Authentication)accessor.getUser();

		final Long userId = Long.parseLong(accessor.getSessionAttributes().get("AUTHENTICATED").toString());

		final Users user = userRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

		projectMemberRepository.findByProject_IdAndUser_UserId(projectId, userId)
			.orElseThrow(() -> new IllegalArgumentException("이 프로젝트에 참여할 권한이 없습니다."));

		final String username = user.getName();

		return chatService.enter(projectId, username);
	}
}
