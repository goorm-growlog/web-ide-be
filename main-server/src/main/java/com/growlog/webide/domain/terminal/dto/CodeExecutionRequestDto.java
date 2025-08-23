package com.growlog.webide.domain.terminal.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CodeExecutionRequestDto {
	private String filePath;
	private Long userId;
	private String language;

	public void setProjectId(Long projectId) {

	}
}
