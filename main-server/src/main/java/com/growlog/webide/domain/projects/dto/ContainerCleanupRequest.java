package com.growlog.webide.domain.projects.dto;

public record ContainerCleanupRequest(
	Long sessionId,
	Long projectId,
	String containerId
) {
}
