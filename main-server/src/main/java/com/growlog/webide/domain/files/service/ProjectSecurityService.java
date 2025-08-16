package com.growlog.webide.domain.files.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.projects.entity.MemberRole;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.entity.ProjectMembers;
import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectSecurityService {
	private final ProjectRepository projectRepo;
	private final UserRepository userRepo;
	private final ProjectMemberRepository memberRepo;

	public boolean hasReadPermission(Long projectId) {
		MemberRole role = getCurrentMemberRole(projectId);
		return role == MemberRole.OWNER || role == MemberRole.WRITE || role == MemberRole.READ;
	}

	public boolean hasWritePermission(Long projectId) {
		MemberRole userRole = getCurrentMemberRole(projectId);
		return userRole == MemberRole.OWNER || userRole == MemberRole.WRITE;
	}

	public boolean hasOwnerPermission(Long projectId) {
		MemberRole userRole = getCurrentMemberRole(projectId);
		return userRole == MemberRole.OWNER;
	}

	private MemberRole getCurrentMemberRole(Long projectId) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return null;
		}

		// principal 에 UserPrincipal 이 들어있는지 확인
		Object prin = auth.getPrincipal();
		if (!(prin instanceof UserPrincipal)) {
			return null;
		}
		UserPrincipal userPrincipal = (UserPrincipal)prin;

		// 토큰에서 꺼낸 userId 사용
		Long userId = userPrincipal.getUserId();

		// 사용자 조회
		Users user = userRepo.findById(userId).orElse(null);
		if (user == null) {
			return null;
		}

		// 프로젝트 조회
		Project project = projectRepo.findById(projectId).orElse(null);
		if (project == null) {
			return null;
		}

		// 멤버 조회
		ProjectMembers member = memberRepo
			.findByUserAndProject(user, project)
			.orElse(null);
		if (member == null) {
			return null;
		}

		return member.getRole();
	}
}
