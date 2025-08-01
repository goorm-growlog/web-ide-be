package com.growlog.webide.domain.chats.service;

import java.util.regex.Matcher;
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

	private final ChatRepository chatRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;

	private static final Pattern CODE_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(ide://([^:]+):(\\d+)\\)");

	@Transactional(readOnly = true)
	public ChattingResponseDto enter(Long projectId, String username) {
		String enterMessage = username + "님이 입장했습니다.";

		return new ChattingResponseDto(ChatType.ENTER, projectId, username, enterMessage);
	}

	@Transactional
	public ChattingResponseDto talk(Long projectId, Long userId, String content) {
		Project projectRef = projectRepository.getReferenceById(projectId);
		Users userRef = userRepository.getReferenceById(userId);
		String username = userRef.getName();
		Chats chat = new Chats(projectRef, userRef, content);
		CodeLink codeLink = parseForCodeLink(content);

		chatRepository.save(chat);

		return new ChattingResponseDto(ChatType.TALK, projectId, username, content, codeLink);
	}

	@Transactional(readOnly = true)
	public ChattingResponseDto leave(Long projectId, Long userId) {
		Users userRef = userRepository.getReferenceById(userId);
		String username = userRef.getName();
		String leaveMessage = username + "님이 퇴장했습니다.";

		return new ChattingResponseDto(ChatType.LEAVE, projectId, username, leaveMessage);
	}

	// 문자열에서 [텍스트](ide://경로:줄번호) 패턴을 찾아 CodeLink 객체로 변환
	private CodeLink parseForCodeLink(String content) {
		Matcher matcher = CODE_LINK_PATTERN.matcher(content);

		if (matcher.find()) {
			String text = matcher.group(1);
			String path = matcher.group(2);
			int line = Integer.parseInt(matcher.group(3));
			return new CodeLink(text, path, line);
		}
		return null;
	}
}
