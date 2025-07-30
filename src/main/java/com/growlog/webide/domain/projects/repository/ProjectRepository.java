package com.growlog.webide.domain.projects.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.growlog.webide.domain.projects.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
	Optional<Object> findByProjectName(String projectName);
}
