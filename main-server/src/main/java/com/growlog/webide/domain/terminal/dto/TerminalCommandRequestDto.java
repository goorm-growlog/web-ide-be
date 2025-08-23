package com.growlog.webide.domain.terminal.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TerminalCommandRequestDto {
	private Long userId;        // 요청한 사용자를 식별
	private String command;     // 실행할 명령어 (e.g., "ls -al", "git status")

	public void setProjectId(Long projectId) {

	}
}
