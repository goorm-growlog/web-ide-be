package com.growlog.webide.domain.files.dto.tree;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class TreeNodeDto {
	private String path;
	private String type;
	private List<TreeNodeDto> children;

	public TreeNodeDto(String path, String type) {
		this.path = path;
		this.type = type;
		this.children = "folder".equals(type) ? new ArrayList<>() : null;
	}
}
