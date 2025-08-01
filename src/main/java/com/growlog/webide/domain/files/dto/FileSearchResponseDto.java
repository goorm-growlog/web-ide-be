package com.growlog.webide.domain.files.dto;

import com.growlog.webide.domain.files.entity.FileMeta;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileSearchResponseDto {
	private Long id;
	private String name;
	private String type;
	private String path;

	public static FileSearchResponseDto from(FileMeta meta) {
		return new FileSearchResponseDto(
			meta.getId(),
			meta.getName(),
			meta.getType(),
			meta.getPath()
		);
	}
}
