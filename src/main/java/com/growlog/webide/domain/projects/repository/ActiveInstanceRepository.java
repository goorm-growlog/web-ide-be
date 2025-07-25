package com.growlog.webide.domain.projects.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.users.entity.Users;

public interface ActiveInstanceRepository extends JpaRepository<ActiveInstance, Long> {
	Optional<ActiveInstance> findByUser_UserIdAndProject_Id(Long userId, Long projectId);

	//객체 기반도 생성
	Optional<ActiveInstance> findByUserAndProject(Users user, Project project);

}
