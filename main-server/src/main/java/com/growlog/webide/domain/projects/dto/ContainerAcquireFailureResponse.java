package com.growlog.webide.domain.projects.dto;

public record ContainerAcquireFailureResponse(
	Long sessionId,
	String reason
) {
}
