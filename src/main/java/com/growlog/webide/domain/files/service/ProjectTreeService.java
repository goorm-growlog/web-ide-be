package com.growlog.webide.domain.files.service;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.files.dto.tree.TreeNodeDto;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectTreeService {

	private final ProjectRepository projectRepository;
	private final SimpMessagingTemplate messagingTemplate;

	@Value("${docker.volume.host-path:/var/lib/docker/volumes}")
	private String volumeHostPath;

	public List<TreeNodeDto> projectTree(Long projectId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		String basePath = Paths.get(volumeHostPath, project.getStorageVolumeName(), "_data").toString();
		File baseDir = new File(basePath);

		return null;
	}
}
