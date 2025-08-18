package com.growlog.webide.domain.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExecutionResult {

	private String stdout;
	private String stderr;
	private int exitCode;
}
