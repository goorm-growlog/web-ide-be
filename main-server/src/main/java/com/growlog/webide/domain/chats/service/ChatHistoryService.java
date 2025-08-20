package com.growlog.webide.domain.chats.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growlog.webide.domain.chats.dto.ChatPagingRequestDto;
import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.dto.PageResponse;
import com.growlog.webide.domain.chats.entity.Chats;
import com.growlog.webide.domain.chats.repository.ChatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

	private final ChatRepository chatRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public PageResponse<ChattingResponseDto> getHistory(Long projectId, ChatPagingRequestDto pagingDto) {
		String key = "chat:" + projectId;
		int page = pagingDto.page(); // default 0
		int size = pagingDto.size(); // default 20

		List<ChattingResponseDto> messages;

		// 첫 페이지 조회 (redis에 남아있는 메시지 최신 20개 -> 없으면 db 조회로 fallback
		if (page == 0) {
			Long listSize = redisTemplate.opsForList().size(key);
			if (listSize != null && listSize > 0) {
				long start = Math.max(listSize - size, 0);
				long end = listSize - 1;
				List<Object> cachedMessages = redisTemplate.opsForList().range(key, start, end);
				Collections.reverse(cachedMessages); // 최신 -> 오래된 순 정렬
				if (cachedMessages != null && !cachedMessages.isEmpty()) {
					// JSON -> DTO 역직렬화
					messages = cachedMessages.stream()
						.map(json -> {
							try {
								return objectMapper.readValue(json.toString(), ChattingResponseDto.class);
							} catch (JsonProcessingException e) {
								log.warn("Failed to deserialize chat Json", e);
								return null;
							}
						})
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
					return PageResponse.from(messages, 0, size, listSize.intValue());
				}
			}
		}

		// Redis 메시지 없거나 이후 페이지 조회 (db)
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Chats> chats = chatRepository.findByProjectIdWithUser(projectId, pageable);

		Page<ChattingResponseDto> chatResponses = chats.map(ChattingResponseDto::from);
		return PageResponse.from(chatResponses);
	}

	@Transactional(readOnly = true)
	public PageResponse<ChattingResponseDto> searchChatHistory(Long projectId, String keyword,
		ChatPagingRequestDto pagingDto) {
		Pageable pageable = PageRequest.of(pagingDto.page(), pagingDto.size());
		Page<Chats> chats = chatRepository.findByProjectIdAndKeywordWithUser(projectId, keyword, pageable);

		Page<ChattingResponseDto> chatResponses = chats.map(ChattingResponseDto::from);
		return PageResponse.from(chatResponses);
	}

}
