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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "project-members", description = "프로젝트 멤버 관련 API 입니다.")
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectMemberController {

	private final ProjectMemberService memberService;

	@Operation(summary = "프로젝트 멤버 초대", description = "이메일로 프로젝트 멤버 초대(OWNER 전용)")
	@PostMapping("/{projectId}/invite")
	@PreAuthorize("@projectSecurityService.hasOwnerPermission(#projectId)")
	public ApiResponse<List<String>> invite(
		@PathVariable Long projectId,
		@Valid @RequestBody InviteMemberRequestDto dto
	) {
		return memberService.inviteMembers(projectId, dto);
	}

	@Operation(summary = "프로젝트 멤버 조회", description = "프로젝트 내의 모든 유저 전체 조회")
	@GetMapping("/{projectId}/members")
	@PreAuthorize("@projectSecurityService.hasReadPermission(#projectId)")
	public ApiResponse<List<MemberDto>> listMembers(
		@PathVariable Long projectId
	) {
		return memberService.listMembers(projectId);
	}

	@Operation(summary = "프로젝트 멤버 권한 수정", description = "프로젝트 참여 멤버의 권한 수정(OWNER 전용)")
	@PatchMapping("/{projectId}/members/{userId}")
	@PreAuthorize("@projectSecurityService.hasOwnerPermission(#projectId)")
	public ApiResponse<String> updateRole(
		@PathVariable Long projectId,
		@PathVariable Long userId,
		@Valid @RequestBody UpdateMemberRoleRequestDto dto
	) {
		return memberService.updateMemberRole(projectId, userId, dto);
	}

	@Operation(summary = "프로젝트 멤버 삭제", description = "프로젝트 멤버 삭제(OWNER 전용)")
	@DeleteMapping("/{projectId}/members/{userId}")
	@PreAuthorize("@projectSecurityService.hasOwnerPermission(#projectId)")
	public ApiResponse<String> remove(
		@PathVariable Long projectId,
		@PathVariable Long userId
	) {
		return memberService.removeMember(projectId, userId);
	}
}
