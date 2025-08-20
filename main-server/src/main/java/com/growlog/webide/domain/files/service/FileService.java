package com.growlog.webide.domain.files.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.files.dto.CreateFileRequest;
import com.growlog.webide.domain.files.dto.FileOpenResponseDto;
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

import jakarta.transaction.Transactional;
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

	@Transactional
	public void createFileorDirectory(Long projectId, CreateFileRequest request, Long userId) {
		log.info("--- SERVICE START: createFileorDirectory 진입 ---");
		//프로젝트 정보 가져오기
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		if (fileMetaRepository.findByProjectIdAndPathAndDeletedFalse(projectId, request.getPath()).isPresent()) {
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

		//파일/폴더 정보를 db에 저장
		log.info("8. DB에 메타데이터 저장 시작");
		FileMeta fileMeta = fileMetaRepository.save(FileMeta.of(project, request.getPath(), request.getType()));

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

		log.info("9. DB 저장 완료. WebSocket 이벤트 전송 시작.");
		// ✅ WebSocket 이벤트 푸시
		WebSocketMessage msg = new WebSocketMessage("tree:add",
			new TreeAddEventDto(fileMeta.getId(), request.getPath(), request.getType()));
		log.info("[WS ▶ add] sending tree:add → projectId={}", projectId);
		messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/tree", msg);

		log.info("--- SERVICE END ---");
	}

	public void deleteFileorDirectory(Long projectId, String path, Long userId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		//쓰기(삭제) 권한 확인
		//파일 삭제는 쓰기(and 오너) 권한을 가진 사람만 가능(읽기 권한이 아닌 사람.)
		permissionService.checkWriteAccess(project, userId);

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
		WebSocketMessage msg = new WebSocketMessage("tree:remove", new TreeRemoveEventDto(meta.getId(), path));
		messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/tree", msg);

	}

	@Transactional
	public void moveFileorDirectory(Long projectId, String fromPath, String toPath, Long userId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
		permissionService.checkWriteAccess(project, userId);

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
		if (!Files.exists(sourcePath)) {
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
			// 이 경우에도 WebSocket 이벤트는 보내주는 것이 UI 일관성에 좋을 수 있습니다.
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
		FileMeta rootMeta = metasToMove.stream()
			.filter(m -> m.getPath().equals(toPath)) // 경로가 업데이트 되었으므로 toPath와 비교
			.findFirst()
			.orElse(null); // 만약 DB에 정보가 없었다면 null일 수 있음

		if (rootMeta != null) {
			WebSocketMessage msg = new WebSocketMessage(
				"tree:move",
				new TreeMoveEventDto(rootMeta.getId(), fromPath, toPath)
			);
			messagingTemplate.convertAndSend(
				"/topic/projects/" + projectId + "/tree",
				msg
			);
		}
	}

	public FileOpenResponseDto openFile(Long projectId, String relativePath, Long userId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

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

			return FileOpenResponseDto.of(projectId, relativePath, fileContent, true); // editable은 write 권한 체크 결과로 설정 가능
		} catch (IOException e) {
			log.error("Failed to read file on EFS. path: {}", targetPath, e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}
	}

	/*
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
