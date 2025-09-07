package com.growlog.webide.domain.terminal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalCommandRequestDto {
	private Long projectId;
	private Long userId;        // 요청한 사용자를 식별
	private String containerId; // 명령어를 실행할 대상 컨테이너 ID
	private String command;     // 실행할 명령어 (e.g., "ls -al", "git status")
	private String dockerImage;
}
