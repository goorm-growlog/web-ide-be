package com.growlog.webide.domain.chats.service;

import java.time.Instant;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.chats.dto.ChatPagingRequestDto;
import com.growlog.webide.domain.chats.dto.ChatType;
import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.dto.CodeLink;
import com.growlog.webide.domain.chats.dto.PageResponse;
import com.growlog.webide.domain.chats.entity.Chats;
import com.growlog.webide.domain.chats.repository.ChatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

	private final ChatRepository chatRepository;

	@Transactional(readOnly = true)
	public PageResponse<ChattingResponseDto> getHistory(Long projectId, ChatPagingRequestDto pagingDto) {
		Pageable pageable = PageRequest.of(pagingDto.page(), pagingDto.size());

		Page<Chats> chats = chatRepository.findByProjectIdWithUser(projectId, pageable);

		Page<ChattingResponseDto> chatResponses = getChattingResponseDtos(
			projectId, chats);
		return PageResponse.from(chatResponses);
	}

	@Transactional(readOnly = true)
	public PageResponse<ChattingResponseDto> searchChatHistory(Long projectId, String keyword,
		ChatPagingRequestDto pagingDto) {
		Pageable pageable = PageRequest.of(pagingDto.page(), pagingDto.size());

		Page<Chats> chats = chatRepository.findByProjectIdAndKeywordWithUser(projectId, keyword, pageable);

		Page<ChattingResponseDto> chatResponses = getChattingResponseDtos(
			projectId, chats);
		return PageResponse.from(chatResponses);
	}

	@NotNull
	private static Page<ChattingResponseDto> getChattingResponseDtos(Long projectId, Page<Chats> chats) {
		Page<ChattingResponseDto> chatResponses = chats.map(
			chat -> {
				final Long userId = chat.getUser().getUserId();
				final String username = chat.getUser().getName();
				final String content = chat.getContent();
				final Instant sentAt = chat.getSentAt();
				final CodeLink codeLink = CodeLinkParser.parse(chat.getContent());
				return new ChattingResponseDto(
					ChatType.TALK, projectId, userId, username, null, content, sentAt, codeLink
				);
			});
		return chatResponses;
	}

}
