package com.growlog.webide.domain.terminal.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.projects.entity.InstanceStatus;
import com.growlog.webide.domain.terminal.entity.ActiveInstance;

public interface ActiveInstanceRepository extends JpaRepository<ActiveInstance, Long> {

	List<ActiveInstance> findAllByProject_IdAndStatus(Long projectId, InstanceStatus status);

	Optional<ActiveInstance> findByUser_UserIdAndProject_IdAndStatus(Long userId, Long projectId,
		InstanceStatus status);

	List<ActiveInstance> findAllByStatusAndLastActivityAtBefore(InstanceStatus status, LocalDateTime threshold);

	List<ActiveInstance> findAllByUser_UserIdAndProject_Id(Long userId, Long projectId);

	Optional<ActiveInstance> findByContainerIdAndStatus(String containerId, InstanceStatus instanceStatus);

	List<ActiveInstance> findAllByProject_Id(Long projectId);
}

