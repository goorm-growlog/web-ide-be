package com.growlog.webide.domain.projects.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.growlog.webide.domain.projects.entity.ProjectMembers;

import io.lettuce.core.dynamic.annotation.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMembers, Long> {
	/*@Query("SELECT pm FROM ProjectMembers pm WHERE pm.project.id = :projectId AND pm.user.userId = :userId")
	Optional<ProjectMembers> findByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);*/

	Optional<ProjectMembers> findByProject_IdAndUser_UserId(Long projectId, Long userId);


}
