package com.growlog.webide.domain.files.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.files.dto.tree.TreeNodeDto;
import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
import com.growlog.webide.domain.files.service.TreeService;
import com.growlog.webide.domain.permissions.service.ProjectPermissionService;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProjectTreeController {
	private final TreeService treeService;
	private final ProjectPermissionService permissionService;
	private final ProjectRepository projectRepository;

	@GetMapping("/projects/{projectId}/tree")
	public WebSocketMessage getProjectTree(
		@PathVariable Long projectId,
		@AuthenticationPrincipal UserPrincipal userPrincipal
	) {
		Long userId = userPrincipal.getUserId();

		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		permissionService.checkReadAccess(project, userId); // 읽기 권한 확인

		TreeNodeDto rootNode = treeService.getInitialTree(projectId);

		List<TreeNodeDto> payload = Collections.singletonList(rootNode);

		// TreeService의 기존 메서드를 재활용하여 최신 트리 구조를 반환
		return new WebSocketMessage("tree:init", payload);
	}
}
