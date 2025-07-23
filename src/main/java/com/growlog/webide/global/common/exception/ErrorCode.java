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

	// 프로젝트/파일용 추가 에러
	PROJECT_NOT_FOUND("PROJECT_NOT_FOUND", "해당 프로젝트를 찾을 수 없습니다."),
	FILE_ALREADY_EXISTS("FILE_ALREADY_EXISTS", "해당 경로에 이미 파일 또는 폴더가 존재합니다."),
	FILE_NOT_FOUND("FILE_NOT_FOUND", "삭제할 파일 또는 폴더를 찾을 수 없습니다.");

	private final String code;
	private final String message;

}
