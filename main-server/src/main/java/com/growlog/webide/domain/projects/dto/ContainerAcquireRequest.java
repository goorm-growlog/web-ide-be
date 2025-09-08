package com.growlog.webide.domain.projects.dto;

public record ContainerAcquireRequest(
	Long sessionId,
	Long projectId
) {
}
