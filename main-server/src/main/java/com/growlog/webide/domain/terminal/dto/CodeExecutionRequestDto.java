package com.growlog.webide.domain.terminal.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CodeExecutionRequestDto {
	private Long projectId;
	private String executionLogId; // 일회성 실행(코드 실행)을 식별하기 위한 ID
	private Long userId;
	private String language;
	private String filePath;
	private String dockerImage;
	private String buildCommand;
	private String runCommand;
}
