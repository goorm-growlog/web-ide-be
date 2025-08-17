package com.growlog.webide.domain.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CodeExecutionResponse {
	private boolean success;    // 컴파일 및 실행 성공 여부
	private String output;        // 표준 출력
	private String error;        // 표준 에러
	private int exitCode;        // 프로세스 종료 코드 (0: 정상)
}
