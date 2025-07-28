package com.growlog.webide.domain.files.controller;

import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
import com.growlog.webide.domain.files.service.TreeService;
import com.growlog.webide.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "file-system", description = "파일 시스템 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}")
public class TreeRestController {

	private final TreeService treeService;

	@Operation(
		summary = "프로젝트 트리 조회 (REST)",
		description = "전체 파일/폴더 트리를 `tree:init` 메시지 형태로 반환합니다."
	)
	@GetMapping("/tree")
	public ApiResponse<WebSocketMessage> getTree(@PathVariable Long projectId) {
		WebSocketMessage msg = new WebSocketMessage(
			"tree:init",
			treeService.buildTree(projectId)
		);
		return ApiResponse.ok(msg);
	}
}
