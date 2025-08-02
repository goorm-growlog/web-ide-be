package com.growlog.webide.domain.chats.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.chats.dto.ChatType;
import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.dto.CodeLink;
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

	private static final Pattern CODE_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(ide://([^:]+):(\\d+)\\)");
	private final ChatRepository chatRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;

	@Transactional(readOnly = true)
	public ChattingResponseDto enter(Long projectId, String username) {
		String enterMessage = username + " joined.";

		return new ChattingResponseDto(ChatType.ENTER, projectId, username, enterMessage);
	}

	@Transactional
	public ChattingResponseDto talk(Long projectId, Long userId, String content) {
		Project projectRef = projectRepository.getReferenceById(projectId);
		Users userRef = userRepository.getReferenceById(userId);
		String username = userRef.getName();
		Chats chat = new Chats(projectRef, userRef, content);
		CodeLink codeLink = CodeLinkParser.parse(content);

		chatRepository.save(chat);

		return new ChattingResponseDto(ChatType.TALK, projectId, username, content, codeLink);
	}

	@Transactional(readOnly = true)
	public ChattingResponseDto leave(Long projectId, Long userId) {
		Users userRef = userRepository.getReferenceById(userId);
		String username = userRef.getName();
		String leaveMessage = username + " left.";

		return new ChattingResponseDto(ChatType.LEAVE, projectId, username, leaveMessage);
	}
}
