package com.growlog.webide.domain.files.dto;

import java.nio.file.Paths;

import com.growlog.webide.global.util.LanguageUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileOpenResponseDto {
	private Long projectId;
	private String filePath;
	private String fileName;
	private String content;
	private String language;
	private boolean editable;

	public static FileOpenResponseDto of(Long projectId, String filePath, String content, boolean editable) {
		String fileName = Paths.get(filePath).getFileName().toString();
		String language = LanguageUtils.detect(fileName); // 확장자 기반 언어 추론
		return new FileOpenResponseDto(projectId, filePath, fileName, content, language, editable);
	}
}
