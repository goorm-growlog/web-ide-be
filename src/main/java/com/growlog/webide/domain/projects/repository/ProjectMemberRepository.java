package com.growlog.webide.domain.projects.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.growlog.webide.domain.projects.entity.ProjectMembers;

public interface ProjectMemberRepository extends JpaRepository<ProjectMembers, Long> {
	@Query("SELECT pm FROM ProjectMembers pm JOIN FETCH pm.user WHERE "
		+ "pm.project.id = :projectId AND pm.user.userId = :userId")
	Optional<ProjectMembers> findByProject_IdAndUser_UserId(@Param("projectId") Long projectId,
		@Param("userId") Long userId);
}
