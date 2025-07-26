package com.growlog.webide.domain.files.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.files.dto.FileOpenResponseDto;
import com.growlog.webide.domain.files.dto.FileSaveRequestDto;
import com.growlog.webide.domain.files.service.FileService;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class FileController {
	private final FileService fileService;

	/**
	 * 파일 열기
	 *
	 * @param projectId 프로젝트 ID
	 * @param path      파일 경로
	 * @param user      인증된 사용자 정보
	 * @return 파일 내용
	 */
	@GetMapping("/{projectId}/file")
	public ResponseEntity<ApiResponse<FileOpenResponseDto>> openFile(@PathVariable Long projectId,
		@RequestParam String path, @AuthenticationPrincipal UserPrincipal user) {
		FileOpenResponseDto response = fileService.openFile(projectId, path, user.getUserId());
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@PutMapping("/{projectId}/file")
	public ResponseEntity<ApiResponse<Map<String, String>>> saveFile(@PathVariable Long projectId,
		@RequestBody FileSaveRequestDto requestDto, @AuthenticationPrincipal UserPrincipal user) {
		try {
			fileService.saveFile(projectId, requestDto.getPath(), requestDto.getContent(), user.getUserId());
			return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "저장되었습니다.")));
		} catch (CustomException e) {
			HttpStatus status = switch (e.getErrorCode()) {
				case NO_WRITE_PERMISSION, NOT_A_MEMBER -> HttpStatus.FORBIDDEN;
				case PROJECT_NOT_FOUND -> HttpStatus.NOT_FOUND;
				default -> HttpStatus.INTERNAL_SERVER_ERROR;
			};
			return ResponseEntity.status(status)
				.body(ApiResponse.error(e.getErrorCode()));
		}

	}
}
