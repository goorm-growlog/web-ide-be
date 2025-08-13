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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

	private final ChatRepository chatRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;

	@Transactional(readOnly = true)
	public ChattingResponseDto enter(Long projectId, Long userId, String username, String profileImageUrl) {
		String enterMessage = username + " joined.";

		log.info("{} Entering project {}", username, projectId);
		return new ChattingResponseDto(ChatType.ENTER, projectId, userId, username, profileImageUrl, enterMessage);

	}

	@Transactional
	public ChattingResponseDto talk(Long projectId, Long userId, String content) {
		Project projectRef = projectRepository.getReferenceById(projectId);
		Users userRef = userRepository.getReferenceById(userId);
		String username = userRef.getName();
		Chats chat = new Chats(projectRef, userRef, content);

		chatRepository.save(chat);

		log.info("{} Talking Project {}", username, projectId);
		return new ChattingResponseDto(ChatType.TALK, projectId, userId, username, null, content);
	}

	@Transactional(readOnly = true)
	public ChattingResponseDto leave(Long projectId, Long userId) {
		Users userRef = userRepository.getReferenceById(userId);
		String username = userRef.getName();
		String leaveMessage = username + " left.";

		log.info("{} Leaving Project {}", username, projectId);
		return new ChattingResponseDto(ChatType.LEAVE, projectId, userId, username, null, leaveMessage);
	}
}
