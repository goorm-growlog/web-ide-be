package com.growlog.webide.domain.terminal.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CodeExecutionApiRequest {
	private String filePath;
	private String language;
}
