package com.growlog.webide.domain.files.service;

import com.growlog.webide.domain.files.dto.tree.TreeNodeDto;
import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TreeService {

	private final InstanceService instanceService;

	@Value("${docker.volume.host-path:/var/lib/docker/volumes}")
	private String volumeHostPath;

	/**
	 * 프로젝트 볼륨에서 전체 트리(Root 포함)를 DTO로 빌드하여 반환.
	 */
	public List<TreeNodeDto> buildTree(Long instanceId) {

		ActiveInstance inst = instanceService.getActiveInstance(instanceId);
		String volumeName = inst.getProject().getStorageVolumeName();
		String base = Paths.get(volumeHostPath, volumeName, "_data")
			.toString();

		File root = new File(base);
		if (!root.exists()) {
			throw new CustomException(ErrorCode.FILE_NOT_FOUND);
		}

		// 루트 노드 생성 (path="/" type="folder")
		TreeNodeDto rootNode = new TreeNodeDto("/", "folder");
		rootNode.getChildren().addAll(buildNodes(root, ""));

		return Collections.singletonList(rootNode);
	}

	/**
	 * 재귀적으로 폴더 내부를 순회하며 TreeNodeDto 생성
	 */
	private List<TreeNodeDto> buildNodes(File dir, String relPath) {
		List<TreeNodeDto> nodes = new ArrayList<>();
		File[] files = dir.listFiles();
		if (files == null) return nodes;

		for (File f : files) {
			String path = relPath + "/" + f.getName();
			String type = f.isDirectory() ? "folder" : "file";
			TreeNodeDto node = new TreeNodeDto(path, type);

			if (f.isDirectory()) {
				node.getChildren().addAll(buildNodes(f, path));
			}
			nodes.add(node);
		}
		return nodes;
	}
}
