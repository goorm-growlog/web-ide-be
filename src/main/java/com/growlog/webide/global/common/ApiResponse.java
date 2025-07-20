package com.growlog.webide.global.common;

import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class ApiResponse<T> {

	private final boolean success;
	private final T data;
	private final Error error;

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static <T> ApiResponse<T> error(ErrorCode errorCode) {
		return new ApiResponse<>(false, null, new Error(errorCode));
	}

	@Getter
	@RequiredArgsConstructor
	public static class Error {
		private final String code;
		private final String message;

		public Error(ErrorCode errorCode) {
			this.code = errorCode.getCode();
			this.message = errorCode.getMessage();
		}
	}
}
