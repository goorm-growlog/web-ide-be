package com.growlog.webide.domain.files.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TreeService {
	private static final String CONTAINER_BASE = "/app";
	private final DockerCommandService dockerCommandService;
	private final FileMetaRepository fileMetaRepository;
	private final ProjectRepository projectRepository;

	/**
	 * 프로젝트 볼륨에서 전체 트리(Root 포함)를 DTO로 빌드하여 반환.
	 */
	@Transactional(readOnly = false)
	public List<TreeNodeDto> buildTree(Long projectId, String containerId) {

		// 컨테이너 내부에서 디렉토리/파일 경로 추출
		List<String> dirPaths = execFind(containerId, "-type d");
		List<String> filePaths = execFind(containerId, "-type f");

		// 1. 프로젝트의 모든 FileMeta를 한 번에 조회
		Map<String, Long> pathIdMap = fileMetaRepository.findAllByProjectIdAndDeletedFalse(projectId)
			.stream()
			.collect(Collectors.toMap(FileMeta::getPath, FileMeta::getId));

		Map<String, TreeNodeDto> nodes = new LinkedHashMap<>();
		TreeNodeDto root = new TreeNodeDto(null, "", "folder");
		nodes.put("", root);

		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		addNodes(dirPaths, "folder", nodes, pathIdMap, project);
		addNodes(filePaths, "file", nodes, pathIdMap, project);

		nodes.forEach((path, node) -> {
			if (path.isEmpty()) {
				return; // root
			}

			String parent = getParentPath(path);
			TreeNodeDto parentNode = nodes.get(parent);
			if (parentNode != null) {
				parentNode.addChild(node);
			} else {
				root.addChild(node); // 예외 fallback
			}
		});

		return List.of(root);
	}

	private void addNodes(List<String> absolutePaths, String type, Map<String, TreeNodeDto> nodes,
						  Map<String, Long> pathIdMap, Project project) {
		for (String absPath : absolutePaths) {
			String relPath = toRelPath(absPath);
			if (relPath == null) {
				log.warn("🚫 무시된 경로 (루트 또는 base 외 경로): {}", absPath);
				continue;
			}

			// ✅ DB에 없는 경우 자동 생성
			if (!pathIdMap.containsKey(relPath)) {
				FileMeta meta = fileMetaRepository.save(FileMeta.of(project, relPath, type));
				pathIdMap.put(relPath, meta.getId());
			}

			// 3. Map에서 바로 ID 조회
			Long id = pathIdMap.get(relPath);
			nodes.put(relPath, new TreeNodeDto(id, relPath, type));
		}
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

	// 절대경로 → 상대경로 (예: /app/foo/bar → /foo/bar)
	private String toRelPath(String absolutePath) {
		if (!absolutePath.startsWith(CONTAINER_BASE)) {
			return null;
		}

		String rel = absolutePath.substring(CONTAINER_BASE.length());
		if (rel.isEmpty() || rel.equals("/")) {
			log.debug("📁 Excluding root path.: {}", absolutePath);
			return null;
		}

		// ✅ 앞 슬래시 제거 (add와 동일하게)
		return rel.startsWith("/") ? rel.substring(1) : rel;
	}

	// 부모 경로 추출
	private String getParentPath(String path) {
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash == -1) {
			return ""; // 최상위 노드
		}
		return path.substring(0, lastSlash);
	}
}
