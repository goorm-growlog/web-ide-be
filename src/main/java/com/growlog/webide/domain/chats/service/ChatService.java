package com.growlog.webide.domain.chats.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.chats.dto.ChatType;
import com.growlog.webide.domain.chats.dto.ChattingResponseDto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ChatService {

	@Transactional(readOnly = true)
	public ChattingResponseDto enter(Long projectId, String username) {
		String enterMessage = username + "님이 입장했습니다.";

		return new ChattingResponseDto(ChatType.ENTER, projectId, username, enterMessage, LocalDateTime.now());
	}

}
