package com.growlog.webide.domain.projects.dto;

public record ContainerAcquireSuccessResponse(
	Long sessionId,
	Long projectId,
	String containerId
) {
}
