package com.growlog.webide.domain.files.dto.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreeAddEventDto {
	private String path;
	private String type;
}
