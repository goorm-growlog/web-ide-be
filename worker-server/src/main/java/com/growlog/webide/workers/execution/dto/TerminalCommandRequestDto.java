package com.growlog.webide.workers.execution.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TerminalCommandRequestDto {
	private Long projectId;     // 프로젝트 식별자
	private Long userId;        // 요청한 사용자를 식별
	private String containerId;
	private String command;     // 실행할 명령어 (e.g., "ls -al", "git status")
	private String dockerImage;
}
