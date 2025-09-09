package com.growlog.webide.domain.terminal.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.growlog.webide.domain.projects.repository.ActiveSessionRepository;
import com.growlog.webide.domain.terminal.config.ContainerTaskProducer;
import com.growlog.webide.domain.terminal.entity.ActiveInstance;
import com.growlog.webide.domain.terminal.repository.ActiveInstanceRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ActiveInstanceService {

	private final ActiveInstanceRepository activeInstanceRepository;
	private final ContainerTaskProducer containerTaskProducer;

	public ActiveInstanceService(ActiveInstanceRepository activeInstanceRepository,
		ContainerTaskProducer containerTaskProducer) {
		this.activeInstanceRepository = activeInstanceRepository;
		this.containerTaskProducer = containerTaskProducer;
	}

	private void requestContainerDeletion(ActiveInstance activeInstance) {
		if (StringUtils.hasText(activeInstance.getContainerId())) {
			containerTaskProducer.requestContainerDeletion(activeInstance.getContainerId());
		}
		if (!StringUtils.hasText(activeInstance.getContainerId())) {
			Long instanceId = activeInstance.getId();
			activeInstanceRepository.deleteById(instanceId);
		}
	}

}
