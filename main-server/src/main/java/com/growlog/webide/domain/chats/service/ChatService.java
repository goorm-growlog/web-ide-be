package com.growlog.webide.domain.chats.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
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
		return new ChattingResponseDto(null, ChatType.ENTER, projectId, userId, username, user.getProfileImageUrl(),
			enterMessage);
	}

	@Transactional
	public ChattingResponseDto talk(Long projectId, Long userId, String content) {
		final Project projectRef = projectRepository.getReferenceById(projectId);
		final Users user = getUsers(userId);
		final Chats chat = new Chats(projectRef, user, content);

		// chatRepository.save(chat);

		ChattingResponseDto response = new ChattingResponseDto(
			null, ChatType.TALK, projectId, userId, user.getName(), user.getProfileImageUrl(), content
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
		return new ChattingResponseDto(null, ChatType.LEAVE, projectId, userId, username, null, leaveMessage);
	}

	private Users getUsers(Long userId) {
		final Users user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		return user;
	}

	// redis cache -> 5분 주기로 db 저장 및 캐시 삭제
	@Scheduled(fixedRate = 5 * 60 * 1000)
	@Transactional
	public void persistChatsToDb() {
		log.info("try to persist chats to db");
		// Set<String> keys = redisTemplate.keys("chat:*");
		ScanOptions scanOptions = ScanOptions.scanOptions().match("chat:*").count(100).build();
		try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection().scan(scanOptions)) {

			// for (String key : keys) {
			while (cursor.hasNext()) {
				String key = new String(cursor.next(), StandardCharsets.UTF_8);
				// 1. 10일 이상 비활성 방은 삭제 (마지막 채팅 기준)
				Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
				if (ttl != null && ttl < 0) {
					redisTemplate.delete(key);
					log.info("[delete] delete chats from inactive rooms: {}", key);
					continue;
				}

				// 2. Redis에서 전체 메시지 조회
				List<Object> all = redisTemplate.opsForList().range(key, 0, -1);
				if (all == null || all.isEmpty()) {
					continue;
				}

				for (int i = 0; i < all.size(); i++) {
					Object obj = all.get(i);
					try {
						String json = obj.toString();
						ChattingResponseDto dto = objectMapper.readValue(json, ChattingResponseDto.class);

						// 3. chatId가 없는 메시지 -> DB 저장
						if (dto.chatId() == null) {
							// Project, Users reference 조회
							Project project = projectRepository.getReferenceById(dto.projectId());
							Users user = userRepository.getReferenceById(dto.userId());

							// Redis -> DTO -> DB 저장용 엔티티 변환 및  DB 저장
							Chats saved = chatRepository.save(dto.toEntity(project, user));

							// chatId 포함된 DTO로 redis에 다시 저장
							ChattingResponseDto updated = ChattingResponseDto.from(saved);
							String updatedJson = objectMapper.writeValueAsString(updated);
							redisTemplate.opsForList().set(key, i, updatedJson);
						}
					} catch (Exception e) {
						log.warn("Failed to parse chat message in {}: {}", key, e.getMessage());
					}
				}

				// 4. redis 정리: 최신 20개만 유지
				Long size = redisTemplate.opsForList().size(key);
				if (size != null && size > 20) {
					redisTemplate.opsForList().trim(key, -20, -1);
				}
			}
		}
	}
}
