package com.growlog.webide.domain.files.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.files.dto.FileOpenResponseDto;
import com.growlog.webide.domain.files.dto.CreateFileRequest;
import com.growlog.webide.domain.files.dto.MoveFileRequest;
import com.growlog.webide.domain.files.dto.tree.TreeAddEventDto;
import com.growlog.webide.domain.files.dto.tree.TreeMoveEventDto;
import com.growlog.webide.domain.files.dto.tree.TreeRemoveEventDto;
import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

	private final InstanceService instanceService;
	private final SimpMessagingTemplate messagingTemplate;
  private final ProjectRepository projectRepository;
	private final DockerCommandService dockerCommandService;
	private final ProjectPermissionService permissionService;
	private final ActiveInstanceRepository activeInstanceRepository;

	@Value("${docker.volume.host-path:/var/lib/docker/volumes}")
	private String volumeHostPath;

	public void createFileorDirectory(Long projectId, CreateFileRequest request) {
		log.info("=== FILE CREATE DEBUG START ===");
		log.info("instanceId: {}, request: {}", projectId, request);

		ActiveInstance inst = instanceService.getActiveInstanceByProjectId(projectId);
		log.info("ActiveInstance found: {}", inst);
		log.info("Project: {}, StorageVolumeName: {}",
			inst.getProject().getId(),
			inst.getProject().getStorageVolumeName());

		String base = Paths.get(volumeHostPath, inst.getProject().getStorageVolumeName(), "_data").toString();
		log.info("Base path: {}", base);
		String rel = request.getPath().startsWith("/") ? request.getPath().substring(1) : request.getPath();
		File target = new File(base, rel);
		log.info("Target path: {}, exists: {}", target.getAbsolutePath(), target.exists());

		// ✅ base 디렉토리 존재 확인 및 생성
		File baseDir = new File(base);
		log.info("Base directory exists: {}", baseDir.exists());
		if (!baseDir.exists()) {
			if (!baseDir.mkdirs()) {
				throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
			}
		}

		log.info("[DEBUG] base='{}', rel='{}', target='{}'",
			base, rel, target.getAbsolutePath());

		if (target.exists()) {
			throw new CustomException(ErrorCode.FILE_ALREADY_EXISTS);
		}

		try {
			if ("file".equalsIgnoreCase(request.getType())) {
				File parent = target.getParentFile();
				if (!parent.exists() && !parent.mkdirs()) {
					throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
				}
				if (!target.createNewFile()) {
					throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
				}

			} else if ("folder".equalsIgnoreCase(request.getType())) {
				if (!target.mkdirs()) {
					throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
				}
			} else {
				throw new CustomException(ErrorCode.BAD_REQUEST);
			}

		} catch (IOException e) {
			log.error("파일 생성 중 IO 예외 발생: {}", e.getMessage(), e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		// ✅ WebSocket 이벤트 푸시
		WebSocketMessage msg = new WebSocketMessage(
			"tree:add",
			new TreeAddEventDto(request.getPath(), request.getType())
		);
		log.info("[WS ▶ add] sending tree:add → instanceId={}", inst.getId());
		messagingTemplate.convertAndSend(
			"/topic/instances/" + inst.getId() + "/tree",
			msg
		);
	}

	public void deleteFileorDirectory(Long projectId, String path) {
		ActiveInstance inst = instanceService.getActiveInstanceByProjectId(projectId);
		String base = Paths.get(volumeHostPath, inst.getProject().getStorageVolumeName(), "_data").toString();
		String rel = path.startsWith("/") ? path.substring(1) : path;
		File target = new File(base, rel);

		if (!target.exists()) {
			throw new CustomException(ErrorCode.FILE_NOT_FOUND);
		}

		boolean deleted = deleteRecursively(target);
		if (!deleted) {
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		// ✅ WebSocket 이벤트 푸시
		WebSocketMessage msg = new WebSocketMessage(
			"tree:remove",
			new TreeRemoveEventDto(path)
		);
		messagingTemplate.convertAndSend(
			"/topic/instances/" + inst.getId() + "/tree",
			msg
		);

	}

	public void moveFileorDirectory(Long projectId, MoveFileRequest request) {
		ActiveInstance inst = instanceService.getActiveInstanceByProjectId(projectId);
		String base = Paths.get(volumeHostPath, inst.getProject().getStorageVolumeName(), "_data")
			.toString();
		String from = request.getFromPath().startsWith("/")
			? request.getFromPath().substring(1)
			: request.getFromPath();
		String to = request.getToPath().startsWith("/")
			? request.getToPath().substring(1)
			: request.getToPath();

		File src = new File(base, from);

		if (!src.exists()) {
			throw new CustomException(ErrorCode.FILE_NOT_FOUND);
		}

		File dst = new File(base, to);
		if (dst.exists()) {
			throw new CustomException(ErrorCode.FILE_ALREADY_EXISTS);
		}

		try {
			Files.createDirectories(dst.getParentFile().toPath());
			Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		// ✅ WebSocket 이벤트 푸시
		WebSocketMessage msg = new WebSocketMessage(
			"tree:move",
			new TreeMoveEventDto(request.getFromPath(), request.getToPath())
		);
		messagingTemplate.convertAndSend(
			"/topic/instances/" + inst.getId() + "/tree",
			msg
		);
	}

	private boolean deleteRecursively(File file) {
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children == null) {
				return false;
			} // ← 보호 코드 추가

			for (File c : children) {
				if (!deleteRecursively(c)) {
					return false;
				}
			}
		}
		return file.delete();
	}
  
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
