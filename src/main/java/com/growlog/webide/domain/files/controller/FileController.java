package com.growlog.webide.domain.files.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.files.dto.CreateFileRequest;
import com.growlog.webide.domain.files.dto.FileResponse;
import com.growlog.webide.domain.files.dto.MoveFileRequest;
import com.growlog.webide.domain.files.service.FileService;
import com.growlog.webide.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "file-system", description = "파일 시스템 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/files")
public class FileController {

	private final FileService fileService;

	@Operation(summary = "파일/폴더 생성", description = "새로운 파일이나 폴더를 생성합니다.")
	@PostMapping
	@PreAuthorize("@projectSecurityService.hasWritePermission(#projectId)")
	public ApiResponse<FileResponse> createFile(
		@PathVariable Long projectId,
		@RequestBody CreateFileRequest request
	) {
		fileService.createFileorDirectory(projectId, request);
		return ApiResponse.ok(new FileResponse("파일이 생성되었습니다."));
	}

	@Operation(summary = "파일/폴더 삭제", description = "파일/폴더를 삭제합니다.")
	@DeleteMapping("/**")
	@PreAuthorize("@projectSecurityService.hasWritePermission(#projectId)")
	public ApiResponse<FileResponse> deleteFile(
		@PathVariable Long projectId,
		@RequestParam("path") String filePath
	) {
		fileService.deleteFileorDirectory(projectId, filePath);
		return ApiResponse.ok(new FileResponse("삭제되었습니다."));
	}

	@Operation(summary = "파일/폴더 이름 변경 및 이동", description = "파일/폴더의 이름을 변경하거나 위치를 변경합니다.")
	@PatchMapping("/{filePath:.+}")
	@PreAuthorize("@projectSecurityService.hasWritePermission(#projectId)")
	public ApiResponse<FileResponse> moveFile(
		@PathVariable Long projectId,
		@RequestBody MoveFileRequest request
	) {
		fileService.moveFileorDirectory(projectId, request);
		return ApiResponse.ok(new FileResponse("파일이 이동되었습니다."));
	}
}
