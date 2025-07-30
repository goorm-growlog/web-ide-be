package com.growlog.webide.domain.files.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.files.dto.InviteMemberRequestDto;
import com.growlog.webide.domain.files.dto.MemberDto;
import com.growlog.webide.domain.files.dto.UpdateMemberRoleRequestDto;
import com.growlog.webide.domain.files.service.ProjectMemberService;
import com.growlog.webide.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectMemberController {

	private final ProjectMemberService memberService;

	@PostMapping("/{projectId}/invite")
	@PreAuthorize("@projectSecurityService.hasOwnerPermission(#projectId)")
	public ApiResponse<List<String>> invite(
		@PathVariable Long projectId,
		@Valid @RequestBody InviteMemberRequestDto dto
	) {
		return memberService.inviteMembers(projectId, dto);
	}

	@GetMapping("/{projectId}/members")
	public ApiResponse<List<MemberDto>> listMembers(
		@PathVariable Long projectId
	) {
		return memberService.listMembers(projectId);
	}

	@PatchMapping("/{projectId}/members/{userId}")
	@PreAuthorize("@projectSecurityService.hasOwnerPermission(#projectId)")
	public ApiResponse<String> updateRole(
		@PathVariable Long projectId,
		@PathVariable Long userId,
		@Valid @RequestBody UpdateMemberRoleRequestDto dto
	) {
		return memberService.updateMemberRole(projectId, userId, dto);
	}

	@DeleteMapping("/{projectId}/members/{userId}")
	@PreAuthorize("@projectSecurityService.hasOwnerPermission(#projectId)")
	public ApiResponse<String> remove(
		@PathVariable Long projectId,
		@PathVariable Long userId
	) {
		return memberService.removeMember(projectId, userId);
	}
}
