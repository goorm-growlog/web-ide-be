package com.growlog.webide.workers.container.dto;

public record ContainerCleanupRequest(
	Long sessionId,
	Long projectId,
	String containerId
) {
}
