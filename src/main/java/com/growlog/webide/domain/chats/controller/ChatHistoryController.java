package com.growlog.webide.domain.chats.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.chats.dto.ChattingResponseDto;
import com.growlog.webide.domain.chats.dto.PageResponse;
import com.growlog.webide.domain.chats.service.ChatHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/chat")
public class ChatHistoryController {

	private final ChatHistoryService chatHistoryService;

	@GetMapping("/history")
	public ResponseEntity<PageResponse<ChattingResponseDto>> getChatHistory(
		@PathVariable Long projectId,
		Pageable pageable
	) {
		final PageResponse<ChattingResponseDto> history = chatHistoryService.getHistory(projectId, pageable);

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(history);
	}

}
