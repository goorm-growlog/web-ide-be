package com.growlog.webide.workers.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContainerCreationRequest {
	private Long projectId;
	private Long userId;
	private String dockerImage;
}
