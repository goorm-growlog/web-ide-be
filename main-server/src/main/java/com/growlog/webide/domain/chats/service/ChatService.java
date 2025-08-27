package com.growlog.webide.domain.chats.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growlog.webide.domain.chats.dto.ChatType;
import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.entity.Chats;
import com.growlog.webide.domain.chats.repository.ChatRepository;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

	private final ChatRepository chatRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ObjectMapper objectMapper;
	private final RedisTemplate<String, Object> redisTemplate;

	@Transactional(readOnly = true)
	public ChattingResponseDto enter(Long projectId, Long userId) {
		final Users user = getUsers(userId);

		projectMemberRepository.findByProject_IdAndUser_UserId(projectId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_A_MEMBER));

		final String username = user.getName();

		final String enterMessage = username + " joined.";

		log.info("{} Entering project {}", username, projectId);
		return new ChattingResponseDto(ChatType.ENTER, projectId, userId, username, user.getProfileImageUrl(),
			enterMessage);
	}

	@Transactional
	public ChattingResponseDto talk(Long projectId, Long userId, String content) {
		final Project projectRef = projectRepository.getReferenceById(projectId);
		final Users user = getUsers(userId);
		final Chats chat = new Chats(projectRef, user, content);

		// chatRepository.save(chat);

		ChattingResponseDto response = new ChattingResponseDto(
			ChatType.TALK, projectId, userId, user.getName(), user.getProfileImageUrl(), content
		);

		String key = "chat:" + projectId;
		String messageJson = null;
		try {
			messageJson = objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize chat message to JSON", e);
		}
		redisTemplate.opsForList().rightPush(key, messageJson);
		redisTemplate.expire(key, Duration.ofDays(10));

		log.info("{} Talking Project {}", user.getName(), projectId);
		return response;
	}

	@Transactional(readOnly = true)
	public ChattingResponseDto leave(Long projectId, Long userId) {
		final Users user = getUsers(userId);
		final String username = user.getName();
		final String leaveMessage = username + " left.";

		log.info("{} Leaving Project {}", user.getName(), projectId);
		return new ChattingResponseDto(ChatType.LEAVE, projectId, userId, username, null, leaveMessage);
	}

	private Users getUsers(Long userId) {
		final Users user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		return user;
	}
}
