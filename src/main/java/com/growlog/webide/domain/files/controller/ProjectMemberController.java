package com.growlog.webide.domain.files.controller;

import com.growlog.webide.domain.files.dto.InviteMemberRequestDto;
import com.growlog.webide.domain.files.dto.MemberDto;
import com.growlog.webide.domain.files.dto.UpdateMemberRoleRequestDto;
import com.growlog.webide.domain.files.service.ProjectMemberService;
import com.growlog.webide.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectMemberController {

	private final ProjectMemberService memberService;

	@PostMapping("/{projectId}/invite")
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

	// 9) 권한 수정
	@PatchMapping("/{projectId}/members/{userId}")
	public ApiResponse<String> updateRole(
		@PathVariable Long projectId,
		@PathVariable Long userId,
		@Valid @RequestBody UpdateMemberRoleRequestDto dto
	) {
		return memberService.updateMemberRole(projectId, userId, dto);
	}

	// 10) 멤버 삭제
	@DeleteMapping("/{projectId}/members/{userId}")
	public ApiResponse<String> remove(
		@PathVariable Long projectId,
		@PathVariable Long userId
	) {
		return memberService.removeMember(projectId, userId);
	}
}
