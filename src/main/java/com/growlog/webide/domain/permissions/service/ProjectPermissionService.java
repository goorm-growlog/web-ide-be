package com.growlog.webide.domain.permissions.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.domain.users.entity.MemberRole;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectPermissionService {
	private final ProjectMemberRepository projectMemberRepository;

	@Transactional(readOnly = true)
	public MemberRole getMyRole(Long projectId, UserPrincipal userPrincipal) {
		return projectMemberRepository
			.findByProject_IdAndUser_UserId(projectId, userPrincipal.getUserId())
			.map(pm -> pm.getRole())
			.orElseThrow(()-> new CustomException(ErrorCode.NOT_A_MEMBER));
	}
}
