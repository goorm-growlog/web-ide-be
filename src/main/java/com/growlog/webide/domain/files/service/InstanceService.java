package com.growlog.webide.domain.files.service;

import org.springframework.stereotype.Service;

import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceService {
	private final ActiveInstanceRepository instanceRepository;

	public ActiveInstance getActiveInstance(Long projectId, Long userId) {
		return instanceRepository.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));
	}
}
