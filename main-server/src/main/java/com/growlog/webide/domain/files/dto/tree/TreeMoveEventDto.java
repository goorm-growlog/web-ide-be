package com.growlog.webide.domain.files.dto.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreeMoveEventDto {
	private Long id;
	private String fromPath;
	private String toPath;
}
