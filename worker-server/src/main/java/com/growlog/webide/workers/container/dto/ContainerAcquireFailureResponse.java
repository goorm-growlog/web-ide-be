package com.growlog.webide.workers.container.dto;

public record ContainerAcquireFailureResponse(
	Long sessionId,
	String reason
) {
}
