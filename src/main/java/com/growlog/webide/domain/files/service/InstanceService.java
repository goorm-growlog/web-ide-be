package com.growlog.webide.domain.files.service;

import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstanceService {
	private final ActiveInstanceRepository instanceRepository;

	public ActiveInstance getActiveInstance(Long instanceId) {
		return instanceRepository.findById(instanceId)
			.orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
	}
}
