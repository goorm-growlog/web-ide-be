package com.growlog.webide.domain.files.service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.files.dto.tree.TreeNodeDto;
import com.growlog.webide.domain.files.entity.FileMeta;
import com.growlog.webide.domain.files.repository.FileMetaRepository;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TreeService {
	/*private static final String CONTAINER_BASE = "/app";
	private final DockerCommandService dockerCommandService;*/

	private final FileMetaRepository fileMetaRepository;
	private final ProjectRepository projectRepository;
	@Value("${efs.base-path}")
	private String efsBasePath;

	@Transactional
	public TreeNodeDto getInitialTree(Long projectId) {
		boolean isEmpty = fileMetaRepository.findAllByProjectIdAndDeletedFalse(projectId).isEmpty();
		if (isEmpty) {
			log.info("[TreeService] DB가 비어있어 EFS와 동기화를 시작합니다.");
			syncFromEfs(projectId);
		}
		return buildTreeFromDb(projectId);
	}

	@Transactional
	public void syncFromEfs(Long projectId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		// DB에서 루트 경로("/")가 이미 있는지 확인(없으면 생성하여 오류 방지)
		fileMetaRepository.findByProjectIdAndPathAndDeletedFalse(projectId, "/")
			.ifPresentOrElse(
				(meta) -> log.info("DB에 루트'/' 정보가 이미 존재합니다. 동기화를 계속합니다. projectId: {}", projectId),
				() -> {
					log.info("DB에 루트'/' 정보가 없어 새로 생성합니다. projectId: {}", projectId);
					FileMeta rootMeta = FileMeta.relativePath(project, "/", "folder");
					fileMetaRepository.save(rootMeta);
				}
			);

		Path projectPath = FileSystems.getDefault().getPath(efsBasePath, String.valueOf(projectId));

		if (!Files.exists(projectPath)) {
			log.warn("프로젝트 경로가 EFS에 존재하지 않습니다. projectId: {}", projectId);
			return;
		}

		try (Stream<Path> paths = Files.walk(projectPath)) {
			List<FileMeta> metasToSave = new ArrayList<>();
			paths.filter(path -> !path.equals(projectPath)).forEach(path -> {
				String relPath = "/" + projectPath.relativize(path).toString().replace("\\", "/");
				String type = Files.isDirectory(path) ? "folder" : "file";
				metasToSave.add(FileMeta.relativePath(project, relPath, type));
			});
			fileMetaRepository.saveAll(metasToSave);
		} catch (IOException e) {
			log.error("EFS 스캔 중 오류 발생. projectId={}", projectId, e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}
	}

	/**
	 * DB 기반으로 트리 빌드
	 */
	@Transactional(readOnly = true)
	public TreeNodeDto buildTreeFromDb(Long projectId) {
		List<FileMeta> files = fileMetaRepository.findAllByProjectIdAndDeletedFalse(projectId);

		if (files.isEmpty()) {
			throw new IllegalStateException("데이터 오류: projectId " + projectId + "에 해당하는 파일 정보가 없습니다.");
		}

		Map<String, TreeNodeDto> nodes = new HashMap<>();

		for (FileMeta meta : files) {
			String path = meta.getPath();
			TreeNodeDto node = new TreeNodeDto(meta.getId(), path, meta.getType());
			nodes.put(path, node);
		}

		TreeNodeDto root = nodes.get("/");
		if (root == null) {
			// 이 경우는 데이터가 잘못된 상태이므로 예외를 발생
			throw new IllegalStateException("데이터 오류: projectId " + projectId + "의 FileMeta에 루트('/') 정보가 없습니다.");
		}

		// parent-child 관계 구성
		for (FileMeta meta : files) {
			if (meta.getPath().equals("/")) {
				continue; //루트 노드는 건너뜀
			}

			String parent = getParentPath(meta.getPath());
			TreeNodeDto parentNode = nodes.get(parent);
			TreeNodeDto childNode = nodes.get(meta.getPath());

			if (parentNode != null && childNode != null) {
				parentNode.addChild(childNode);
			}
		}

		return root;
	}

	private String getParentPath(String path) {
		if (path == null || path.equals("/")) {
			return null;
		}
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash == 0) {
			return "/"; // 최상위 파일/폴더의 부모는 "/"
		}
		return path.substring(0, lastSlash);
	}
}

