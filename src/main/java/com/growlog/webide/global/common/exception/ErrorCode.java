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

	// Sign in, Log in 관련
	LOGIN_FAILED("LOGIN_FAILED", "아이디 혹은 비밀번호가 올바르지 않습니다."),
	EMAIL_CONFLICT("EMAIL_CONFLICT", "이 이메일로 가입된 계정이 있습니다."),
	EMAIL_NOT_FOUND("EMAIL_NOT_FOUND", "이메일을 찾을 수 없습니다."),

	// 인증 관련
	EMAIL_VERIFICATION_FAILED("EMAIL_VERIFICATION_FAILED", "이메일 인증에 실패했습니다."),
	EMAIL_NOT_VERIFIED("EMAIL_NOT_VERIFIED", "이메일이 인증되지 않았습니다."),
	AUTH_CODE_NOT_FOUND("AUTH_404", "인증번호를 먼저 요청해주세요."),
	INVALID_AUTH_CODE("AUTH_401", "인증번호가 일치하지 않습니다."),
	DELETED_USER("DELETED_USER", "삭제된 회원입니다."),
	AUTH_INFO_NOT_FOUND("AUTH_INFO_NOT_FOUND", "인증 정보 조회에 실패했습니다."),

	// User 관련
	USER_NOT_FOUND("USER_NOT_FOUND", "존재하지 않는 회원입니다."),

	// 회원 탈퇴 관련
	INVALID_PASSWORD("INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),
	UNAUTHORIZED_ACCESS("UNAUTHORIZED_ACCESS", "접근 권한이 없습니다."),

	// 프로젝트/파일용 추가 에러
	PROJECT_NOT_FOUND("PROJECT_NOT_FOUND", "해당 프로젝트를 찾을 수 없습니다."),
	FILE_ALREADY_EXISTS("FILE_ALREADY_EXISTS", "해당 경로에 이미 파일 또는 폴더가 존재합니다."),
	FILE_NOT_FOUND("FILE_NOT_FOUND", "파일 또는 폴더를 찾을 수 없습니다."),
	FILE_OPERATION_FAILED("FILE_OPERATION_FAILED", "파일 처리 중 오류가 발생했습니다."),
	INSTANCE_NOT_FOUND("INSTANCE_NOT_FOUND", "인스턴스를 찾을 수 없습니다."),
	MEMBER_ALREADY_EXISTS("MEMBER_ALREADY_EXISTS", "이미 프로젝트에 참여중인 사용자입니다."),
	MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "프로젝트 멤버가 아닙니다.");

	private final String code;
	private final String message;

}
