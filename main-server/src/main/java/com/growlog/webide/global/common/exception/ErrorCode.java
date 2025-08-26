package com.growlog.webide.global.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 공통 에러
	BAD_REQUEST("BAD_REQUEST", "Bad request."),
	INVALID_ACCESSTOKEN("INVALID_ACCESSTOKEN", "Invalid access token."),
	INVALID_REFRESH_TOKEN("INVALID_REFRESH_TOKEN", "Invalid refresh token."),
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "An internal server error has occurred."),
	REFRESH_TOKEN_NOT_FOUND("REFRESH_TOKEN_NOT_FOUND", "Refresh token not found."),
	REFRESH_TOKEN_REUSED("REFRESH_TOKEN_REUSED", "Refresh token has been reused."),
	PARSING_ERROR("PARSING_ERROR", "Parsing error."),

	// Sign in, Log in 관련
	LOGIN_FAILED("LOGIN_FAILED", "Incorrect username or password."),
	EMAIL_CONFLICT("EMAIL_CONFLICT", "An account with this email already exists."),
	EMAIL_NOT_FOUND("EMAIL_NOT_FOUND", "Email not found."),

	// 인증 관련
	EMAIL_VERIFICATION_FAILED("EMAIL_VERIFICATION_FAILED", "Email verification failed."),
	EMAIL_NOT_VERIFIED("EMAIL_NOT_VERIFIED", "Email not verified."),
	AUTH_CODE_NOT_FOUND("AUTH_404", "Please request an auth code first."),
	INVALID_AUTH_CODE("AUTH_401", "The auth code does not match."),
	DELETED_USER("DELETED_USER", "This account has been deleted."),
	AUTH_INFO_NOT_FOUND("AUTH_INFO_NOT_FOUND", "Failed to retrieve auth information."),
	SHA256_NOT_CREATED("SHA256_NOT_CREATED", "SHA-256 Algorithm not available"),

	// User 관련
	USER_NOT_FOUND("USER_NOT_FOUND", "User not found."),

	// 회원 탈퇴 관련
	INVALID_PASSWORD("INVALID_PASSWORD", "Incorrect password."),
	UNAUTHORIZED_ACCESS("UNAUTHORIZED_ACCESS", "Unauthorized access."),

	//권한 관련
	NOT_A_MEMBER("NOT_A_MEMBER", "Not a member of this project."),
	NO_READ_PERMISSION("NO_READ_PERMISSION", "Read permission required."),
	NO_WRITE_PERMISSION("NO_WRITE_PERMISSION", "Write permission required."),
	NO_OWNER_PERMISSION("NO_OWNER_PERMISSION", "Owner permission required."),

	//도커 관련
	DOCKER_COMMAND_FAILED("DOCKER_COMMAND_FAILED", "Failed to execute Docker command."),

	//프로젝트/파일 관련
	CONTAINER_NOT_FOUND("CONTAINER_NOT_FOUND", "Container not found."),
	ACTIVE_CONTAINER_NOT_FOUND("ACTIVE_CONTAINER_NOT_FOUND", "Active container not found."),
	NOT_SUPPORT_LANGUAGE("NOT_SUPPORT_LANGUAGE", "Unsupported language."),
	PROJECT_NOT_FOUND("PROJECT_NOT_FOUND", "Project not found."),
	FILE_ALREADY_EXISTS("FILE_ALREADY_EXISTS", "A file or directory already exists at this path."),
	FILE_NOT_FOUND("FILE_NOT_FOUND", "File or directory not found."),
	FILE_OPERATION_FAILED("FILE_OPERATION_FAILED", "An error occurred during file operation."),
	INSTANCE_NOT_FOUND("INSTANCE_NOT_FOUND", "Instance not found."),
	IMAGE_NOT_FOUND("IMAGE_NOT_FOUND", "Image not found."),
	INVALID_FILE_PATH("INVALID_FILE_PATH", "Invalid file path."),
	CANNOT_MOVE_TO_SUBFOLDER("CANNOT_MOVE_TO_SUBFOLDER", "Unable to move to the folder below"),

	CONTAINER_STILL_RUNNING("CONTAINER_STILL_RUNNING", "Active container still running."),

	MEMBER_ALREADY_EXISTS("MEMBER_ALREADY_EXISTS", "This user is already a member of the project."),
	MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "Project member not found."),
	PATH_NOT_ALLOWED("PATH_NOT_ALLOWED", "Path is not allowed."),

	// 채팅 관련
	KEYWORD_NOT_FOUND("KEYWORD_NOT_FOUND", "Search keyword cannot be empty.");

	private final String code;
	private final String message;

}
