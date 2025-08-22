package com.growlog.webide.workers.work;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExecutionResultDto {
	private Long projectId;
	private Long userId;
	private String status; // 예: "SUCCESS", "FAILURE"
	private String output; // 실행 결과(표준 출력)
	private String errorMessage; // 에러 발생 시 메시지
}
