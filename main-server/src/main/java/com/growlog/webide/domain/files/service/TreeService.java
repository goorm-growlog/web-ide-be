package com.growlog.webide.domain.files.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.files.dto.tree.TreeNodeDto;
import com.growlog.webide.domain.files.entity.FileMeta;
import com.growlog.webide.domain.files.repository.FileMetaRepository;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.docker.DockerCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TreeService {
	private static final String CONTAINER_BASE = "/app";
	private final DockerCommandService dockerCommandService;
	private final FileMetaRepository fileMetaRepository;
	private final ProjectRepository projectRepository;

	/**
	 * 최초 1회: 컨테이너 내부 구조 스캔해서 DB에 FileMeta 생성
	 */
	@Transactional
	public void syncFromContainer(Long projectId, String containerId) {
		List<String> dirPaths = execFind(containerId, "-type d");
		List<String> filePaths = execFind(containerId, "-type f");

		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		List<FileMeta> existing = fileMetaRepository.findAllByProjectIdAndDeletedFalse(projectId);
		Map<String, FileMeta> metaMap = existing.stream()
			.collect(Collectors.toMap(FileMeta::getPath, m -> m));

		for (String absPath : dirPaths) {
			String relPath = toRelPath(absPath);
			if (relPath != null && !metaMap.containsKey(relPath)) {
				FileMeta saved = fileMetaRepository.save(FileMeta.relativePath(project, relPath, "folder"));
				metaMap.put(relPath, saved);
			}
		}

		for (String absPath : filePaths) {
			String relPath = toRelPath(absPath);
			if (relPath != null && !metaMap.containsKey(relPath)) {
				FileMeta saved = fileMetaRepository.save(FileMeta.relativePath(project, relPath, "file"));
				metaMap.put(relPath, saved);
			}
		}
	}

	/**
	 * DB 기반으로 트리 빌드
	 */
	@Transactional(readOnly = true)
	public List<TreeNodeDto> buildTreeFromDb(Long projectId) {
		List<FileMeta> files = fileMetaRepository.findAllByProjectIdAndDeletedFalse(projectId);

		Map<String, TreeNodeDto> nodes = new LinkedHashMap<>();
		TreeNodeDto root = new TreeNodeDto(null, "", "folder");
		nodes.put("", root);

		for (FileMeta meta : files) {
			String path = meta.getPath();
			TreeNodeDto node = new TreeNodeDto(meta.getId(), path, meta.getType());
			nodes.put(path, node);
		}

		// parent-child 관계 구성
		for (Map.Entry<String, TreeNodeDto> entry : nodes.entrySet()) {
			String path = entry.getKey();
			TreeNodeDto node = entry.getValue();
			if (path.isEmpty()) {
				continue;
			}

			String parent = getParentPath(path);
			TreeNodeDto parentNode = nodes.getOrDefault(parent, root);
			parentNode.addChild(node);
		}

		return List.of(root);
	}

	private List<String> execFind(String containerId, String typeOption) {
		String cmd = String.format("find %s %s -print", CONTAINER_BASE, typeOption);
		try {
			String raw = dockerCommandService.execAndReturn(containerId, cmd);
			return Arrays.stream(raw.split("\\r?\\n"))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("❌ [execFind] Failed to execute find command in container ({}).", containerId, e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}
	}

	private String toRelPath(String absolutePath) {
		if (!absolutePath.startsWith(CONTAINER_BASE)) {
			return null;
		}

		String rel = absolutePath.substring(CONTAINER_BASE.length());
		if (rel.isEmpty() || rel.equals("/")) {
			return null;
		}
		return rel.startsWith("/") ? rel.substring(1) : rel;
	}

	private String getParentPath(String path) {
		int idx = path.lastIndexOf('/');
		return (idx == -1) ? "" : path.substring(0, idx);
	}
}

