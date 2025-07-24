package com.growlog.webide.domain.files.controller;

import com.growlog.webide.domain.files.dto.CreateFileRequest;
import com.growlog.webide.domain.files.dto.FileResponse;
import com.growlog.webide.domain.files.dto.MoveFileRequest;
import com.growlog.webide.domain.files.service.FileService;
import com.growlog.webide.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/files")
public class FileController {

	private final FileService fileService;

	@PostMapping
	public ApiResponse<FileResponse> createFile(
		@PathVariable Long projectId,
		@RequestBody CreateFileRequest request
	) {
		fileService.createFileorDirectory(projectId, request);
		return ApiResponse.ok(new FileResponse("파일이 생성되었습니다."));
	}

	@DeleteMapping("/**")
	public ApiResponse<FileResponse> deleteFile(
		@PathVariable Long projectId,
		@RequestParam("path") String filePath
	) {
		fileService.deleteFileorDirectory(projectId, filePath);
		return ApiResponse.ok(new FileResponse("삭제되었습니다."));
	}

	@PatchMapping("/{filePath:.+}")
	public ApiResponse<FileResponse> moveFile(
		@PathVariable Long projectId,
		@RequestBody MoveFileRequest request
	) {
		fileService.moveFileorDirectory(projectId, request);
		return ApiResponse.ok(new FileResponse("파일이 이동되었습니다."));
	}
}
