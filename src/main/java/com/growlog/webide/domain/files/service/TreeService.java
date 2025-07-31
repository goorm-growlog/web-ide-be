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
import com.growlog.webide.domain.projects.entity.ActiveInstance;
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
	private final InstanceService instanceService;
	private final DockerCommandService dockerCommandService;
	private final FileMetaRepository fileMetaRepository;

	/**
	 * 프로젝트 볼륨에서 전체 트리(Root 포함)를 DTO로 빌드하여 반환.
	 */
	@Transactional(readOnly = true)
	public List<TreeNodeDto> buildTree(Long instanceId) {

		ActiveInstance inst = instanceService.getActiveInstance(instanceId);
		long projectId = inst.getProject().getId();
		String containerId = inst.getContainerId();

		// 컨테이너 내부에서 디렉토리/파일 경로 추출
		List<String> dirPaths = execFind(containerId, "-type d");
		List<String> filePaths = execFind(containerId, "-type f");

		// 절대경로 → 상대경로 매핑 & DTO 생성
		Map<String, TreeNodeDto> nodes = new LinkedHashMap<>();
		TreeNodeDto root = new TreeNodeDto(null, "", "folder");
		nodes.put("", root);

		addNodes(dirPaths, "folder", nodes, projectId);
		addNodes(filePaths, "file", nodes, projectId);

		nodes.forEach((path, node) -> {
			if (path.isEmpty())
				return; // root

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

	private void addNodes(List<String> absolutePaths, String type, Map<String, TreeNodeDto> nodes, long projectId) {
		for (String absPath : absolutePaths) {
			String relPath = toRelPath(absPath);
			if (relPath == null) {
				// 🛑 null이면 로그 찍고 continue 하자
				log.warn("🚫 무시된 경로 (루트 또는 base 외 경로): {}", absPath);
				continue;
			}
			Long id = fileMetaRepository.findByProjectIdAndPath(projectId, relPath)
				.map(FileMeta::getId).orElse(null);
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
			log.error("❌ [execFind] 컨테이너({})에서 find 명령 실패", containerId, e);
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}
	}

	// 절대경로 → 상대경로 (예: /app/foo/bar → /foo/bar)
	private String toRelPath(String absolutePath) {
		if (!absolutePath.startsWith(CONTAINER_BASE))
			return null;

		String rel = absolutePath.substring(CONTAINER_BASE.length());
		if (rel.isEmpty() || rel.equals("/")) {
			log.debug("📁 root path 제외: {}", absolutePath);
			return null;
		}

		// ✅ 앞 슬래시 제거 (add와 동일하게)
		return rel.startsWith("/") ? rel.substring(1) : rel;
	}

	// 부모 경로 추출
	private String getParentPath(String path) {
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash == -1)
			return null; // 최상위 노드
		return path.substring(0, lastSlash);
	}
}
