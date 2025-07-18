package com.growlog.web_ide.global.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.growlog.web_ide.global.common.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<String>> handleCustomException(CustomException ex) {
		log.error(ex.toString());
		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(ex.getErrorCode()));
	}

	// CustomException에서 지정하지 않은 모든 예외 : 서버 오류로 간주
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex) {
		log.error("Unhandled Exception: ", ex);
		return ResponseEntity
			.internalServerError()
			.body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
