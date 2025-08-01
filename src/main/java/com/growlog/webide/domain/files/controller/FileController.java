package com.growlog.webide.domain.files.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.files.dto.CreateFileRequest;
import com.growlog.webide.domain.files.dto.FileOpenResponseDto;
import com.growlog.webide.domain.files.dto.FileResponse;
import com.growlog.webide.domain.files.dto.FileSaveRequestDto;
import com.growlog.webide.domain.files.dto.FileSearchResponseDto;
import com.growlog.webide.domain.files.dto.MoveFileRequest;
import com.growlog.webide.domain.files.service.FileService;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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

	/**
	 * 파일 열기
	 *
	 * @param projectId 프로젝트 ID
	 * @param path      파일 경로
	 *                  - 루트 작업 디렉토리(`/workspace`) 기준의 상대 경로입니다.
	 *  	            - 예: "src/Main.java" → 실제 경로: "/workspace/src/Main.java"
	 * @param user      인증된 사용자 정보
	 * @return 파일 내용
	 */
	@Operation(summary = "파일열기", description = "프로젝트 내 파일을 열어 내용을 반환한다.(경로는 컨테이너 작업 디렉토리 기준)")
	@GetMapping
	public ResponseEntity<ApiResponse<FileOpenResponseDto>> openFile(
		@Parameter(description = "프로젝트 ID", example = "1") @PathVariable Long projectId,

		@Parameter(description = """
			파일 경로는 컨테이너의 루트 작업 디렉토리(`/app`) 기준.
			예: "src/Main.java" → 실제 경로: "/app/src/Main.java"
			""", example = "src/Main.java") @RequestParam String path, @AuthenticationPrincipal UserPrincipal user) {

		FileOpenResponseDto response = fileService.openFile(projectId, path, user.getUserId());
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	/**
	 * 파일 저장 API
	 *
	 * @param projectId 프로젝트 ID
	 * @param requestDto 저장할 파일 경로 및 내용
	 *             - path 값은 컨테이너 루트 디렉토리(`/workspace`)를 기준으로 한 상대 경로입니다.
	 *             - 예: "src/Main.java"
	 * @param user 인증된 사용자 정보
	 * @return 저장 성공 메시지
	 */
	@Operation(summary = "파일 저장", description = "프로젝트 내 파일을 수정 및 저장한다. (경로는 컨테이너 작업 디렉토리 기준)")
	@PutMapping
	public ResponseEntity<ApiResponse<Map<String, String>>> saveFile(
		@Parameter(description = "프로젝트 ID", example = "1") @PathVariable Long projectId,

		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "저장할 파일 경로와 파일 내용",
			required = true,
			content = @io.swagger.v3.oas.annotations.media.Content(
				schema = @Schema(example = "{\n"
					+ "  \"path\": \"src/Main.java\",\n"
					+ "  \"content\": \"public class Main {\\n"
					+ "    public static void main(String[] args) {\\n"
					+ "      System.out.println(\\\"Updated!\\\");\\n"
					+ "    }\\n"
					+ "  }\"\n"
					+ "}")
			)
		)
		@RequestBody FileSaveRequestDto requestDto,
		@AuthenticationPrincipal UserPrincipal user) {
		try {
			fileService.saveFile(projectId, requestDto.getPath(), requestDto.getContent(), user.getUserId());
			return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "저장되었습니다.")));
		} catch (CustomException e) {
			HttpStatus status = switch (e.getErrorCode()) {
				case NO_WRITE_PERMISSION, NOT_A_MEMBER -> HttpStatus.FORBIDDEN;
				case PROJECT_NOT_FOUND -> HttpStatus.NOT_FOUND;
				default -> HttpStatus.INTERNAL_SERVER_ERROR;
			};
			return ResponseEntity.status(status).body(ApiResponse.error(e.getErrorCode()));
		}

	}

	@Operation(summary = "파일/폴더 이름 검색", description = "프로젝트 내 파일 또는 폴더의 이름을 기준으로 검색합니다.")
	@GetMapping("/search")
	public ApiResponse<List<FileSearchResponseDto>> searchFiles(
		@Parameter(description = "프로젝트 ID", example = "1") @PathVariable Long projectId,
		@Parameter(description = "검색 키워드", example = "code") @RequestParam String query
	) {
		List<FileSearchResponseDto> results = fileService.searchFilesByName(projectId, query);
		return ApiResponse.ok(results);
	}

}
