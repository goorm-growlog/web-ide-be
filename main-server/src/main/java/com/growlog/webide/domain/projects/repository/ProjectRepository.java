package com.growlog.webide.domain.projects.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.entity.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, Long> {
	Optional<Object> findByProjectName(String projectName);

	Optional<Project> findByIdAndStatusNot(Long projectId, ProjectStatus projectStatus);
}
