package com.growlog.webide.domain.files.service;

import java.io.File;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.files.dto.CreateFileRequest;
import com.growlog.webide.domain.files.dto.FileOpenResponseDto;
import com.growlog.webide.domain.files.dto.FileSearchResponseDto;
import com.growlog.webide.domain.files.dto.tree.TreeAddEventDto;
import com.growlog.webide.domain.files.dto.tree.TreeMoveEventDto;
import com.growlog.webide.domain.files.dto.tree.TreeRemoveEventDto;
import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
import com.growlog.webide.domain.files.entity.FileMeta;
import com.growlog.webide.domain.files.repository.FileMetaRepository;
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

	private static final String CONTAINER_BASE = "/app";
	private final InstanceService instanceService;
	private final SimpMessagingTemplate messagingTemplate;
	private final ProjectRepository projectRepository;
	private final DockerCommandService dockerCommandService;
	private final ProjectPermissionService permissionService;
	private final ActiveInstanceRepository activeInstanceRepository;
	private final FileMetaRepository fileMetaRepository;

	public void createFileorDirectory(Long projectId, CreateFileRequest request, Long userId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
		ActiveInstance inst = activeInstanceRepository.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));
		Long instanceId = inst.getId();

		String cid = inst.getContainerId();
		String rel = request.getPath().startsWith("/")
			? request.getPath().substring(1)
			: request.getPath();
		String full = CONTAINER_BASE + "/" + rel;

		try {
			if ("file".equalsIgnoreCase(request.getType())) {
				// 부모 폴더 생성
				String parent = full.contains("/")
					? full.substring(0, full.lastIndexOf('/'))
					: CONTAINER_BASE;
				dockerCommandService.execInContainer(cid, "mkdir -p \"" + parent + "\"");
				// 빈 파일 만들기
				dockerCommandService.execInContainer(cid, "touch \"" + full + "\"");

			} else if ("folder".equalsIgnoreCase(request.getType())) {
				dockerCommandService.execInContainer(cid, "mkdir -p \"" + full + "\"");
			} else {
				throw new CustomException(ErrorCode.BAD_REQUEST);
			}
		} catch (CustomException ce) {
			throw ce;
		} catch (Exception e) {
			log.error("Failed to create file in container.", e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		FileMeta fileMeta = fileMetaRepository.save(FileMeta.of(project, request.getPath(), request.getType()));

		// ✅ WebSocket 이벤트 푸시
		WebSocketMessage msg = new WebSocketMessage(
			"tree:add",
			new TreeAddEventDto(fileMeta.getId(), request.getPath(), request.getType())
		);
		log.info("[WS ▶ add] sending tree:add → projectId={}", projectId);
		messagingTemplate.convertAndSend(
			"/topic/projects/" + projectId + "/tree",
			msg
		);
	}

	public void deleteFileorDirectory(Long projectId, String path, Long userId) {
		ActiveInstance inst = activeInstanceRepository.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		Long instanceId = inst.getId();

		String cid = inst.getContainerId();

		String rel = path.startsWith("/") ? path.substring(1) : path;

		String full = CONTAINER_BASE + "/" + rel;

		// exec rm -rf
		try {
			dockerCommandService.execInContainer(cid,
				String.format("rm -rf \"%s\"", full)
			);
		} catch (CustomException ce) {
			throw ce;
		} catch (Exception e) {
			log.error("Failed to delete file or directory in container.", e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		FileMeta meta = fileMetaRepository.findByProjectIdAndPath(projectId, path)
			.orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
		meta.markDeleted();
		fileMetaRepository.save(meta);

		// ✅ WebSocket 이벤트 푸시
		WebSocketMessage msg = new WebSocketMessage(
			"tree:remove",
			new TreeRemoveEventDto(meta.getId(), path)
		);
		messagingTemplate.convertAndSend(
			"/topic/projects/" + projectId + "/tree",
			msg
		);

	}

	public void moveFileorDirectory(Long projectId, String fromPath, String toPath, Long userId) {
		ActiveInstance inst = activeInstanceRepository.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		Long instanceId = inst.getId();

		String cid = inst.getContainerId();

		String from = fromPath.startsWith("/") ? fromPath.substring(1) : fromPath;
		String to = toPath.startsWith("/") ? toPath.substring(1) : toPath;

		String fullFrom = CONTAINER_BASE + "/" + from;
		String fullTo = CONTAINER_BASE + "/" + to;

		String parent = fullTo.contains("/")
			? fullTo.substring(0, fullTo.lastIndexOf('/'))
			: CONTAINER_BASE;

		try {
			// (1) mkdir -p <parent>
			dockerCommandService.execInContainer(cid,
				String.format("mkdir -p \"%s\"", parent)
			);
			// (2) mv <fullFrom> <fullTo>
			dockerCommandService.execInContainer(cid,
				String.format("mv \"%s\" \"%s\"", fullFrom, fullTo)
			);
		} catch (CustomException ce) {
			throw ce;
		} catch (Exception e) {
			log.error("Failed move file or directory in container.", e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		FileMeta meta = fileMetaRepository.findByProjectIdAndPath(projectId, fromPath)
			.orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

		meta.updatePath(toPath);
		fileMetaRepository.save(meta);

		// ✅ WebSocket 이벤트 푸시
		WebSocketMessage msg = new WebSocketMessage(
			"tree:move",
			new TreeMoveEventDto(meta.getId(), fromPath, toPath)
		);
		messagingTemplate.convertAndSend(
			"/topic/projects/" + projectId + "/tree",
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
		log.info("📂 Open file - containerId: {}, path: {}", containerId, relativePath);

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

		log.info("✅ File saved successfully. - containerId: {}, path: {}", containerId, relativePath);
	}

	public List<FileSearchResponseDto> searchFilesByName(Long projectId, String query) {
		return fileMetaRepository.findByProjectIdAndNameContainingIgnoreCaseAndDeletedFalse(projectId, query)
			.stream()
			.map(FileSearchResponseDto::from)
			.toList();
	}

}
