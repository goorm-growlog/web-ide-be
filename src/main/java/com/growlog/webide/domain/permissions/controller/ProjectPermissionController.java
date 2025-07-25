package com.growlog.webide.domain.permissions.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.permissions.dto.RoleResponseDto;
import com.growlog.webide.domain.permissions.service.ProjectPermissionService;
import com.growlog.webide.domain.users.entity.MemberRole;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/permissions")
@RequiredArgsConstructor
public class ProjectPermissionController {

	private final ProjectPermissionService projectPermissionService;

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<RoleResponseDto>> getMyRole(
		@PathVariable Long projectId,
		@AuthenticationPrincipal UserPrincipal userPrincipal) {
		MemberRole role = projectPermissionService.getMyRole(projectId, userPrincipal);

		return ResponseEntity.ok(ApiResponse.ok(new RoleResponseDto(role.name())));
	}

}
