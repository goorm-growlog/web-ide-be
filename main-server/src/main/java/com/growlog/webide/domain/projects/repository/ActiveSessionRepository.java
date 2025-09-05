package com.growlog.webide.domain.projects.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.projects.entity.ActiveSession;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.users.entity.Users;

public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {

	Optional<ActiveSession> findByUser_UserIdAndProject_Id(Long userId, Long projectId);

	// 특정 사용자 활성 세션 찾기
	Optional<ActiveSession> findByUserAndProject(Users user, Project project);

	// 특정 프로젝트에 활성 세션이 몇 개 있는지
	long countByProject(Project project);

	// 특정 프로젝트에 특정 상태의 세션이 몇 개 있는지
	// long countByProjectAndStatus(Project project, InstanceStatus status);

	// 컨테이너 ID로 활성 세션 찾기
	Optional<ActiveSession> findByContainerId(String containerId);

	// 프로젝트 아이디로 ActiveInstance 찾기
	Optional<ActiveSession> findByProject_Id(Long projectId);

	List<ActiveSession> findAllByProject_Id(Long projectId);
}
