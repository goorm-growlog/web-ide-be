package com.growlog.webide.domain.chats.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.chats.dto.ChatType;
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
	public PageResponse<ChattingResponseDto> getHistory(Long projectId, Pageable pageable) {
		Page<Chats> chats = chatRepository.findByProjectIdWithUser(projectId, pageable);

		Page<ChattingResponseDto> chatResponses = chats.map(
			chat -> {
				final String username = chat.getUser().getName();
				final String content = chat.getContent();
				return new ChattingResponseDto(
					ChatType.TALK, projectId, username, content
				);
			});
		return PageResponse.from(chatResponses);
	}

}
