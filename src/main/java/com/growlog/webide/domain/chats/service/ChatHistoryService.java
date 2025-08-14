package com.growlog.webide.domain.chats.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.chats.dto.ChatPagingRequestDto;
import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
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
