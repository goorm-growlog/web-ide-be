package com.growlog.webide.domain.files.service;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.files.dto.CreateFileRequest;
import com.growlog.webide.domain.files.dto.tree.TreeAddEventDto;
import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
import com.growlog.webide.domain.files.entity.FileMeta;
import com.growlog.webide.domain.files.repository.FileMetaRepository;
import com.growlog.webide.domain.permissions.service.ProjectPermissionService;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
	@Value("${project.efs-base-path}") // application.yml에 설정한 값을 가져옴
	private String efsBasePath;

	private final SimpMessagingTemplate messagingTemplate;
	private final ProjectRepository projectRepository;
	private final ProjectPermissionService permissionService;
	private final FileMetaRepository fileMetaRepository;

	// 기본 파일시스템 (운영에서는 EFS 마운트 경로)
	private FileSystem fileSystem = FileSystems.getDefault();

	//테스트에서 주입을 위한 setter
	public void setFileSystem(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	public void createFileorDirectory(Long projectId, CreateFileRequest request, Long userId) {
		log.info("--- SERVICE START: createFileorDirectory 진입 ---");
		//프로젝트 정보 가져오기
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		if (fileMetaRepository.findByProjectIdAndPath(projectId, request.getPath()).isPresent()) {
			throw new CustomException(ErrorCode.FILE_ALREADY_EXISTS);
		}

		Path targetPath;
		try {
			log.info("1. resolveProjectPath 호출 시작");
			//파일 경로 찾기
			targetPath = resolveProjectPath(projectId, request.getPath());
			log.info("2. 경로 계산 완료: {}", targetPath);
		} catch (IOException e) {
			throw new CustomException(ErrorCode.INVALID_FILE_PATH);
		}

		log.info("3. Files.exists()로 파일 존재 여부 확인 시작");
		//동일한 파일/폴더 체크
		if (Files.exists(targetPath)) {
			log.error("!!! 문제 발생: 파일 생성 로직 이전에 파일이 이미 존재함: {}", targetPath);
			throw new CustomException(ErrorCode.FILE_ALREADY_EXISTS);
		}

		try {
			log.info("4. 파일이 존재하지 않음을 확인. 생성 로직으로 진행.");
			if ("file".equalsIgnoreCase(request.getType())) {
				// 부모 폴더 생성
				log.info("5a. 부모 디렉토리 생성 시도: {}", targetPath.getParent());
				Files.createDirectories(targetPath.getParent());

				// 빈 파일 만들기
				log.info("6a. 파일 생성 시도: {}", targetPath);
				Files.createFile(targetPath);

			} else if ("folder".equalsIgnoreCase(request.getType())) {
				//폴더 생성
				log.info("5b. 디렉토리 생성 시도: {}", targetPath);
				Files.createDirectories(targetPath);
			} else {
				throw new CustomException(ErrorCode.BAD_REQUEST);
			}

		} catch (IOException e) {
			log.error("IO 예외 발생.", e);
			log.error("Failed to create file or directory on EFS.", e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		//파일/폴더 정보를 db에 저장
		log.info("8. DB에 메타데이터 저장 시작");
		FileMeta fileMeta = fileMetaRepository.save(FileMeta.of(project, request.getPath(), request.getType()));
		log.info("9. DB 저장 완료. WebSocket 이벤트 전송 시작.");

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

		log.info("--- SERVICE END ---");
	}

/*
	public void deleteFileorDirectory(Long projectId, String path, Long userId) {
		ActiveInstance inst = activeInstanceRepository.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

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
	}*/

	//입력한 파일 전체 경로 생성
	private Path resolveProjectPath(Long projectId, String relativePath) throws IOException {
		//프로젝트별 기본 경로 생성 (ex: /app/123)
		Path projectRoot = fileSystem.getPath(efsBasePath, String.valueOf(projectId));

		//전체 경로 생성 (ex: /app/123/src/main.java)
		// normalize()는 ../ 같은 경로 조작을 방지
		Path fullPath = projectRoot.resolve(relativePath).normalize();

		if (!fullPath.startsWith(projectRoot)) {
			throw new CustomException(ErrorCode.PATH_NOT_ALLOWED);
		}
		return fullPath;
	}

}
