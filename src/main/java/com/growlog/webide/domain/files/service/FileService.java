package com.growlog.webide.domain.files.service;

import org.springframework.stereotype.Service;

import com.growlog.webide.domain.files.dto.FileOpenResponseDto;
import com.growlog.webide.domain.permissions.service.ProjectPermissionService;
import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.docker.DockerCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

	private final ProjectRepository projectRepository;
	private final DockerCommandService dockerCommandService;
	private final ProjectPermissionService permissionService;
	private final ActiveInstanceRepository activeInstanceRepository;

	public FileOpenResponseDto openFile(Long projectId, String relativePath, Long userId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		// 권한 확인
		permissionService.checkReadAccess(project, userId);

		ActiveInstance instance = activeInstanceRepository.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		String containerId = instance.getContainerId();

		// 👉 로그 추가 (디버깅용)
		log.info("📂 파일 열기 - containerId: {}, path: {}", containerId, relativePath);

		String fileContent = dockerCommandService.readFileContent(containerId, relativePath);

		return FileOpenResponseDto.of(projectId, relativePath, fileContent, true); // editable은 write 권한 체크 결과로 설정 가능
	}

	public void saveFile(Long projectId, String relativePath, String content, Long userId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		permissionService.checkWriteAccess(project, userId);

		ActiveInstance instance = activeInstanceRepository.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		String containerId = instance.getContainerId();
		dockerCommandService.writeFileContent(containerId, relativePath, content);

		log.info("✅ 파일 저장 완료 - containerId: {}, path: {}", containerId, relativePath);
	}
}
