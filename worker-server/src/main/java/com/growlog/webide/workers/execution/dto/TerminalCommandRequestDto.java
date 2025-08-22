package com.growlog.webide.workers.execution.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TerminalCommandRequestDto {
	private Long projectId;     // 어떤 프로젝트의 터미널인지 식별
	private Long userId;        // 요청한 사용자를 식별
	private String command;     // 실행할 명령어 (e.g., "ls -al", "git status")
}
