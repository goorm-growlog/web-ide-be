package com.growlog.webide.domain.chats.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.chats.dto.ChatType;
import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.entity.Chats;
import com.growlog.webide.domain.chats.repository.ChatRepository;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ChatService {

	private final ChatRepository chatRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;

	@Transactional(readOnly = true)
	public ChattingResponseDto enter(Long projectId, String username) {
		String enterMessage = username + "님이 입장했습니다.";

		return new ChattingResponseDto(ChatType.ENTER, projectId, username, enterMessage);
	}

	@Transactional
	public ChattingResponseDto talk(Long projectId, Long userId, String username, String content) {
		Project projectRef = projectRepository.getReferenceById(projectId);
		Users userRef = userRepository.getReferenceById(userId);
		Chats chat = new Chats(projectRef, userRef, content);

		chatRepository.save(chat);

		return new ChattingResponseDto(ChatType.TALK, projectId, username, content);
	}
}
