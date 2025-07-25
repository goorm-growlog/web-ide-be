package com.growlog.webide.domain.projects.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.growlog.webide.domain.projects.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

}
