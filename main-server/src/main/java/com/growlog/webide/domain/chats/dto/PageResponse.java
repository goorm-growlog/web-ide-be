package com.growlog.webide.domain.chats.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<ChattingResponseDto>(
	List<ChattingResponseDto> content,
	int pageNumber,
	long totalElements,
	int totalPages
) {
	public static <ChattingResponseDto> PageResponse<ChattingResponseDto> from(Page<ChattingResponseDto> page) {
		return new PageResponse<>(
			page.getContent(),
			page.getNumber(),
			page.getTotalElements(),
			page.getTotalPages()
		);
	}
}
