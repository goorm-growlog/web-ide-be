package com.growlog.webide.domain.files.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FileSaveRequestDto {
	private String path;
	private String content;
}
