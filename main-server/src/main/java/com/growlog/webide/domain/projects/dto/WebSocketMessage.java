package com.growlog.webide.domain.projects.dto;

public record WebSocketMessage(
	String type,
	String message
) {
}
