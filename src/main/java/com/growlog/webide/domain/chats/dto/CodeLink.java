package com.growlog.webide.domain.chats.dto;

public record CodeLink(
	String text,
	String path,
	int line
) {
}
