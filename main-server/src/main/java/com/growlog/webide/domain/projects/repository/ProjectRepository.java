package com.growlog.webide.domain.projects.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.entity.ProjectStatus;

import jakarta.persistence.LockModeType;

public interface ProjectRepository extends JpaRepository<Project, Long> {
	Optional<Project> findByIdAndStatusNot(Long projectId, ProjectStatus projectStatus);

	Optional<Project> findByOwner_UserIdAndIdAndStatusNot(Long userId, Long projectId, ProjectStatus projectStatus);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Project> findWithLockById(Long projectId);
}
