package com.growlog.webide.domain.files.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileService {
	private static final String FILE = "file";
	private static final String FOLDER = "folder";
	private final String efsBasePath;

	private final SimpMessagingTemplate messagingTemplate;
	private final ProjectRepository projectRepository;
	private final ProjectPermissionService permissionService;
	private final FileMetaRepository fileMetaRepository;

	// 기본 파일시스템 (운영에서는 EFS 마운트 경로)
	private final FileSystem fileSystem;

	public FileService(@Value("${efs.base-path}") String efsBasePath,
		SimpMessagingTemplate messagingTemplate,
		ProjectRepository projectRepository,
		ProjectPermissionService permissionService,
		FileMetaRepository fileMetaRepository) {
		this.efsBasePath = efsBasePath;
		this.messagingTemplate = messagingTemplate;
		this.projectRepository = projectRepository;
		this.permissionService = permissionService;
		this.fileMetaRepository = fileMetaRepository;
		this.fileSystem = FileSystems.getDefault();
	}

	// TODO: validation
	private static void checkRequest(CreateFileRequest request) {
		if (!(FILE.equals(request.getType()) || FOLDER.equals(request.getType()))) {
			throw new CustomException(ErrorCode.BAD_REQUEST);
		}
	}

	@Transactional
	public void createFileOrDirectory(Long projectId, CreateFileRequest request, Long userId) {
		log.info("====== CREATE FILE OR DIRECTORY CALLED FOR: {} ======", request.getPath());

		checkRequest(request);
		permissionService.checkWriteAccess(userId, projectId);
		checkAlreadyExistsFile(projectId, request.getPath());

		Project project = findProjectById(projectId);
		File file = new File(request.getPath());

		// Save DB
		saveAllParentFolders(file, project);
		Long fileMetaId = saveFileMeta(request, project);

		// Save EFS
		saveFileOrDirectoryEfs(projectId, request.getType(), request.getPath());

		sendEvent(new WebSocketMessage("tree:add",
			new TreeAddEventDto(fileMetaId, request.getPath(), request.getType())), projectId);
	}

	@Transactional
	public void deleteFileOrDirectory(Long projectId, String path, Long userId) {
		Project project = findProjectById(projectId);

		//쓰기(삭제) 권한 확인
		//파일 삭제는 쓰기(and 오너) 권한을 가진 사람만 가능(읽기 권한이 아닌 사람.)
		permissionService.checkWriteAccess(userId, project.getId());

		//db에서 파일 메타 정보 조회
		FileMeta meta = fileMetaRepository.findByProjectIdAndPathAndDeletedFalse(projectId, path)
			.orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

		Path targetPath;
		try {
			// efs 상의 실제 파일/폴더 경로 계산
			targetPath = resolveProjectPath(projectId, path);
		} catch (IOException e) {
			throw new CustomException(ErrorCode.INVALID_FILE_PATH);
		}

		//java nio api를 사용한 파일/폴더 삭제
		try {
			if (Files.exists(targetPath)) {
				if (Files.isDirectory(targetPath)) {
					// 디렉터리인 경우, 재귀적으로 삭제
					try (Stream<Path> walk = Files.walk(targetPath)) {
						walk.sorted(Comparator.reverseOrder()).forEach(p -> {
							try {
								Files.delete(p);
							} catch (IOException ex) {
								throw new UncheckedIOException(ex);
							}
						});
					}
				} else {
					// 파일인 경우, 바로 삭제
					Files.delete(targetPath);
				}
			} else {
				log.warn("File not found on EFS, but metadata exists. Path: {}", targetPath);
			}
		} catch (UncheckedIOException | IOException e) {
			log.error("Failed to delete file or directory on EFS. Path: {}", targetPath, e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		//db에서 메타데이터 삭제
		meta.markDeleted();
		fileMetaRepository.save(meta);

		// ✅ WebSocket 이벤트 푸시
		sendEvent(new WebSocketMessage("tree:remove", new TreeRemoveEventDto(meta.getId(), path)), projectId);
	}

	@Transactional
	public void moveFileOrDirectory(Long projectId, String fromPath, String toPath, Long userId) {
		Project project = findProjectById(projectId);
		permissionService.checkWriteAccess(userId, project.getId());

		Path sourcePath;
		Path targetPath;

		try {
			//EFS 상의 원본(source) 및 대상(target) 경로 계산
			sourcePath = resolveProjectPath(projectId, fromPath);
			targetPath = resolveProjectPath(projectId, toPath);
		} catch (IOException e) {
			throw new CustomException(ErrorCode.INVALID_FILE_PATH);
		}

		//조건 확인
		//원본 파일이 존재하지 않으면
		boolean fileMetaRepository2 = !Files.exists(sourcePath);
		if (fileMetaRepository2) {
			throw new CustomException(ErrorCode.FILE_NOT_FOUND);
		}

		//이동하려는 파일이 이미 있으면
		if (Files.exists(targetPath)) {
			throw new CustomException(ErrorCode.FILE_ALREADY_EXISTS);
		}

		// 자기 자신의 하위 폴더로 이동하는 것 방지
		if (targetPath.startsWith(sourcePath)) {
			throw new CustomException(ErrorCode.CANNOT_MOVE_TO_SUBFOLDER);
		}

		try {
			//EFS 파일 시스템 작업 (mkdir -p + mv)
			//대상 경로의 부모 디렉터리가 없으면 생성
			Files.createDirectories(targetPath.getParent());

			Files.move(sourcePath, targetPath);
		} catch (IOException e) {
			log.error("Failed to move file or directory on EFS. from: {}, to: {}", sourcePath, targetPath, e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		//DB 메타데이터 업데이트
		// 이동할 대상과 그 하위의 모든 파일/폴더 메타데이터를 DB에서 조회
		List<FileMeta> metasToMove = fileMetaRepository.findByProjectIdAndPathStartingWith(projectId, fromPath);

		if (metasToMove.isEmpty()) {
			// 실제 파일은 있으나 DB에 정보가 없는 경우. 에러를 던지거나 경고 로그를 남길 수 있음.
			log.warn("File was moved on EFS, but no corresponding metadata found in DB for path starting with: {}",
				fromPath);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		for (FileMeta meta : metasToMove) {
			String oldPath = meta.getPath();
			// 기존 경로의 시작 부분(fromPath)을 새로운 경로(toPath)로 교체
			String newPath = oldPath.replaceFirst(Pattern.quote(fromPath), toPath);
			meta.updatePath(newPath);
		}
		fileMetaRepository.saveAll(metasToMove); // 변경된 모든 메타데이터를 한번에 저장

		// ✅ WebSocket 이벤트 푸시
		// 가장 상위의 메타데이터 ID를 사용
		metasToMove.stream()
			.filter(m -> m.getPath().equals(toPath)) // 경로가 업데이트 되었으므로 toPath와 비교
			.findFirst()
			.ifPresent(rootMeta -> sendEvent(
				new WebSocketMessage("tree:move", new TreeMoveEventDto(rootMeta.getId(), fromPath, toPath)),
				projectId));

	}

	public FileOpenResponseDto openFile(Long projectId, String relativePath, Long userId) {
		Project project = findProjectById(projectId);

		// 권한 확인
		permissionService.checkReadAccess(project, userId);

		Path targetPath;
		try {
			//실제 파일 경로 계산
			targetPath = resolveProjectPath(projectId, relativePath);
		} catch (IOException e) {
			throw new CustomException(ErrorCode.INVALID_FILE_PATH);
		}

		//파일 존재 여부 확인
		if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
			throw new CustomException(ErrorCode.FILE_NOT_FOUND);
		}

		try {
			String fileContent = Files.readString(targetPath);

			return FileOpenResponseDto.of(projectId, relativePath, fileContent,
				true); // editable은 write 권한 체크 결과로 설정 가능
		} catch (IOException e) {
			log.error("Failed to read file on EFS. path: {}", targetPath, e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}
	}

	public List<FileSearchResponseDto> searchFilesByName(Long projectId, String query) {
		List<FileMeta> searchResults = fileMetaRepository.findByProjectIdAndNameContainingIgnoreCaseAndDeletedFalse(projectId, query);

		if (searchResults.isEmpty()) {
			throw new CustomException(ErrorCode.FILE_NOT_FOUND);
		}

		return searchResults
			.stream()
			.map(FileSearchResponseDto::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public void saveFileToStorage(Long projectId, String relativePath, String content, Long userId) {
		Project project = findProjectById(projectId);

		permissionService.checkWriteAccess(userId, project.getId());

		Path targetPath;
		try {
			targetPath = resolveProjectPath(projectId, relativePath);
		} catch (IOException e) {
			throw new CustomException(ErrorCode.INVALID_FILE_PATH);
		}

		//파일이 존재하는지 확인
		fileMetaRepository.findByProjectIdAndPathAndDeletedFalse(projectId, relativePath)
			.orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

		try {
			Files.createDirectories(targetPath.getParent());
			Files.writeString(targetPath, content);
			log.info("✅ File saved successfully. - path: {}", targetPath);
		} catch (IOException e) {
			log.error("Failed to save file on EFS. path: {}", targetPath, e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}
	}

	private Long saveFileMeta(CreateFileRequest request, Project project) {
		FileMeta fileMeta = FileMeta.relativePath(project, request.getPath(), request.getType());
		fileMetaRepository.save(fileMeta);
		return fileMeta.getId();
	}

	private Project findProjectById(Long projectId) {
		return projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
	}

	//입력한 파일 전체 경로 생성
	private Path resolveProjectPath(Long projectId, String relativePath) throws IOException {
		//프로젝트별 기본 경로 생성 (ex: /app/123)
		Path projectRoot = fileSystem.getPath(efsBasePath, String.valueOf(projectId));
		log.info("Resolved project path: {}, {}", projectRoot, relativePath);

		if (relativePath == null || !relativePath.startsWith("/")) {
			throw new CustomException(ErrorCode.BAD_REQUEST);
		}

		Path fullPath = getFullPath(relativePath, projectRoot);

		log.info("Resolved project path: {}", fullPath);
		log.info("fullPath: {}, projectRoot: {}", fullPath, projectRoot);

		if (!fullPath.startsWith(projectRoot)) {
			throw new CustomException(ErrorCode.PATH_NOT_ALLOWED);
		}

		return fullPath;
	}

	/**
	 * 전체 경로 생성 (ex: /app/123/src/main.java)
	 * Path.resolve 는 상대 경로만 인식
	 *
	 * @param relativePath
	 * @param projectRoot
	 * @return
	 */
	private Path getFullPath(String relativePath, Path projectRoot) {
		log.info("Clean relative path: {}", relativePath);
		return projectRoot.resolve(relativePath.substring(1));
	}

	private void checkAlreadyExistsFile(Long projectId, String path) {
		if (fileMetaRepository.findByProjectIdAndPathAndDeletedFalse(projectId, path).isPresent()) {
			throw new CustomException(ErrorCode.FILE_ALREADY_EXISTS);
		}
	}

	private void sendEvent(WebSocketMessage fileMeta, Long projectId) {
		messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/tree", fileMeta);
	}

	private void saveFileOrDirectoryEfs(Long projectId, String type, String path) {
		try {
			Path targetPath = resolveProjectPath(projectId, path);
			if (FILE.equals(type)) {
				Files.createDirectories(targetPath.getParent());
				Files.createFile(targetPath);
			}

			if (FOLDER.equalsIgnoreCase(type)) {
				Files.createDirectories(targetPath);
			}
		} catch (FileAlreadyExistsException e) {
			log.error("Race Condition or Inconsistent State: File already exists on EFS. path: {}", e.getFile(), e);
			throw new CustomException(ErrorCode.FILE_ALREADY_EXISTS);
		} catch (IOException e) {
			log.error("IO Exception Occurred.", e);
			log.error("Failed to create file or directory on EFS.", e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}
	}

	private void saveAllParentFolders(File file, Project project) {
		if (StringUtils.hasText(file.getParent())) {
			List<String> parents = new ArrayList<>();

			File parent = file.getParentFile();

			while (parent != null) {
				parents.add(parent.getPath());
				parent = parent.getParentFile();
			}

			Set<String> existingPaths = fileMetaRepository
				.findByProjectIdAndPathInAndDeletedFalse(project.getId(), parents)
				.stream()
				.map(FileMeta::getPath)
				.collect(Collectors.toSet());

			List<FileMeta> newFoldersToSave = parents.stream()
				.filter(path -> !existingPaths.contains(path))
				.map(path -> FileMeta.relativePath(project, path, "folder"))
				.toList();

			if (!newFoldersToSave.isEmpty()) {
				fileMetaRepository.saveAll(newFoldersToSave);
			}
		}
	}

}
