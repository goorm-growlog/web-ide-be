package com.growlog.webide.domain.files.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.files.dto.FileOpenResponseDto;
import com.growlog.webide.domain.files.service.FileService;
import com.growlog.webide.global.common.ApiResponse;
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
	public ResponseEntity<ApiResponse<FileOpenResponseDto>> openFile(
		@PathVariable Long projectId,
		@RequestParam String path,
		@AuthenticationPrincipal UserPrincipal user
	) {
		FileOpenResponseDto response = fileService.openFile(projectId, path, user.getUserId());
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
