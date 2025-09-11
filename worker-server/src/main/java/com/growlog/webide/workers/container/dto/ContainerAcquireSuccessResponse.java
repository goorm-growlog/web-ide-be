package com.growlog.webide.workers.container.dto;

public record ContainerAcquireSuccessResponse(
	Long sessionId,
	Long projectId,
	String containerId
) {
}
