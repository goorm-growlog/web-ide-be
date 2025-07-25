package com.growlog.webide.domain.projects.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.projects.entity.ProjectMembers;

public interface ProjectMemberRepository extends JpaRepository<ProjectMembers, Long> {

	Optional<ProjectMembers> findByProject_IdAndUser_UserId(Long projectId, Long userId);

}
