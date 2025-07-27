package com.growlog.webide.domain.projects.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.projects.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
