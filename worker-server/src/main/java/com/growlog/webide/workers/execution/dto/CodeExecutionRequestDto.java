package com.growlog.webide.workers.execution.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CodeExecutionRequestDto {
	private Long projectId;
	private String filePath;
	private Long userId;
	private String language;
}
