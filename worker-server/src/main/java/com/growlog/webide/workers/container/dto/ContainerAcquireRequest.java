package com.growlog.webide.workers.container.dto;

public record ContainerAcquireRequest(
	Long sessionId,
	Long projectId
) {
}
