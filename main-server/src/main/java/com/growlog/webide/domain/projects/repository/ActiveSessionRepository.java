package com.growlog.webide.domain.projects.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.projects.entity.ActiveSession;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.users.entity.Users;

public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {

	List<ActiveSession> findAllByUser_UserIdAndProject_Id(Long userId, Long projectId);

	List<ActiveSession> findAllByProject_Id(Long projectId);

	Long countAllByProjectId(Long projectId);
}
