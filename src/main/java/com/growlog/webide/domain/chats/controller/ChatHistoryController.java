package com.growlog.webide.domain.chats.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.chats.dto.ChatPagingRequestDto;
import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.dto.PageResponse;
import com.growlog.webide.domain.chats.service.ChatHistoryService;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/chat")
public class ChatHistoryController {

	private final ChatHistoryService chatHistoryService;

	@GetMapping("/history")
	public ResponseEntity<PageResponse<ChattingResponseDto>> getChatHistory(
		@PathVariable Long projectId,
		@ParameterObject ChatPagingRequestDto pagingDto
	) {
		final PageResponse<ChattingResponseDto> history = chatHistoryService.getHistory(projectId, pagingDto);

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(history);
	}

	@GetMapping("/search")
	public ResponseEntity<PageResponse<ChattingResponseDto>> searchChatHistory(
		@PathVariable Long projectId,
		@RequestParam(required = false) String keyword,
		@ParameterObject ChatPagingRequestDto pagingDto
	) {
		if (!StringUtils.hasText(keyword)) {
			throw new CustomException(ErrorCode.KEYWORD_NOT_FOUND);
		}

		final PageResponse<ChattingResponseDto> results =
			chatHistoryService.searchChatHistory(projectId, keyword, pagingDto);

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(results);
	}

}
