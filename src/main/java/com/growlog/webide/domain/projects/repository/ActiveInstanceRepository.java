package com.growlog.webide.domain.projects.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.users.entity.User;

public interface ActiveInstanceRepository extends JpaRepository<ActiveInstance, Long> {

	// 특정 사용자 활성 세션 찾기
	Optional<ActiveInstance> findByUserAndProject(User user, Project project);

	// 특정 프로젝트에 활성 세션이 몇 개 있는지
	long countByProject(Project project);

	// 컨테이너 ID로 활성 세션 찾기
	Optional<ActiveInstance> findByContainerId(String containerId);
}
