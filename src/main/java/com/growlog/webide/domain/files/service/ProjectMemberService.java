package com.growlog.webide.domain.files.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.files.dto.InviteMemberRequestDto;
import com.growlog.webide.domain.files.dto.MemberDto;
import com.growlog.webide.domain.files.dto.UpdateMemberRoleRequestDto;
import com.growlog.webide.domain.projects.entity.MemberRole;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.ProjectMembers;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectMemberService {

	private final ProjectRepository projectRepo;
	private final ProjectMemberRepository memberRepo;
	private final UserRepository userRepo;

	public ApiResponse<List<String>> inviteMembers(Long projectId, InviteMemberRequestDto dto) {

		Project project = projectRepo.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		List<String> invited = new ArrayList<>();

		for (String email : dto.getEmails()) {
			Users user = userRepo.findByEmail(email)
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

			if (memberRepo.findByUserAndProject(user, project).isPresent()) {
				throw new CustomException(ErrorCode.MEMBER_ALREADY_EXISTS);
			}

			ProjectMembers pm = ProjectMembers.builder()
				.project(project)
				.user(user)
				.role(MemberRole.READ)   // 기본 권한 READ
				.build();

			memberRepo.save(pm);
			invited.add(email);
		}

		return ApiResponse.ok(invited);
	}

	public ApiResponse<List<MemberDto>> listMembers(Long projectId) {
		Project project = projectRepo.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		List<MemberDto> members = memberRepo.findByProject(project).stream()
			.map(pm -> new MemberDto(
				pm.getUser().getUserId(),
				pm.getUser().getName(),
				pm.getUser().getEmail(),
				pm.getUser().getProfileImageUrl(),
				pm.getRole()
			))
			.collect(Collectors.toList());

		return ApiResponse.ok(members);
	}

	public ApiResponse<String> updateMemberRole(Long projectId, Long userId, UpdateMemberRoleRequestDto dto) {
		Project project = projectRepo.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
		Users user = userRepo.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		ProjectMembers pm = memberRepo.findByUserAndProject(user, project)
			.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

		pm.setRole(dto.getRole());
		memberRepo.save(pm);

		return ApiResponse.ok("권한이 변경되었습니다.");
	}

	public ApiResponse<String> removeMember(Long projectId, Long userId) {
		Project project = projectRepo.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
		Users user = userRepo.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		ProjectMembers pm = memberRepo.findByUserAndProject(user, project)
			.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

		memberRepo.delete(pm);
		return ApiResponse.ok("사용자가 프로젝트에서 제거되었습니다.");
	}
}
