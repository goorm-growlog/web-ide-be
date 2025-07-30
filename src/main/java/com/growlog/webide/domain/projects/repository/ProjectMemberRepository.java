package com.growlog.webide.domain.projects.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.projects.entity.ProjectMembers;
import com.growlog.webide.domain.projects.entity.MemberRole;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.users.entity.ProjectMembers;
import com.growlog.webide.domain.users.entity.Users;

public interface ProjectMemberRepository extends JpaRepository<ProjectMembers, Long> {

	Optional<ProjectMembers> findByProject_IdAndUser_UserId(Long projectId, Long userId);

	Optional<ProjectMembers> findByUserAndProject(Users user, Project project);

	List<ProjectMembers> findByUserAndRole(Users user, MemberRole role);

	List<ProjectMembers> findByUser(Users user);
}
