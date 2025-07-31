package com.growlog.webide.domain.files.dto.tree;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class TreeNodeDto {
	private Long id;
	private String path;
	private String type;
	private List<TreeNodeDto> children;

	public TreeNodeDto(Long id, String path, String type) {
		this.id = id;
		this.path = path;
		this.type = type;
		this.children = "folder".equals(type) ? new ArrayList<>() : null;
	}

	public void addChild(TreeNodeDto child) {
		this.children.add(child);
	}
}
