package com.growlog.webide.workers.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LogMessage {
	/**
	 * 로그를 수신할 대상(클라이언트)을 식별하는 ID (예: containerId, executionLogId)
	 */
	private String targetId;

	/**
	 * 로그를 수신할 사용자의 ID
	 */
	private Long userId;

	/** 로그의 종류 (stdout, stderr) */
	private String streamType;
	/** 실제 로그 내용 */
	private String content;
}
