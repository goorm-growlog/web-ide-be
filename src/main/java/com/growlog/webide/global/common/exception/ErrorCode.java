package com.growlog.webide.global.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 공통 에러
	BAD_REQUEST("BAD_REQUEST", "잘못된 요청입니다."),
	INVALID_ACCESSTOKEN("INVALID_ACCESSTOKEN", "유효하지 않은 접근입니다."),
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

	USER_NOT_FOUND("ENTITY_NOT_FOUND", "사용자를 찾을 수 없습니다."),
	ACCESS_DENIED("ACCESS_DENIED", "프로젝트 참여 권한이 없습니다.");

	private final String code;
	private final String message;

}
