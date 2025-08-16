package com.growlog.webide.global.util;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

@Component
public class ProjectPathResolver { //프로젝트 언어별 기본 경로 관리 로직

	private static final Map<String, String> languageMainFileMap = Map.of(
		"java", "Main.java"/*,
		"python", "main.py",
		"cpp", "main.cpp"*/
	);
	@Value("${docker.workspace-path}")
	private String baseWorkspaceDir;

	/**
	 * 언어별로 기본 메인 파일 경로를 반환합니다.
	 * 언어의 기본 시작 파일 경로를 나타냄
	 * 예: "/app/Main.java"
	 */
	public String getDefaultMainFilePath(String language) {
		String fileName = languageMainFileMap.get(language.toLowerCase());

		if (fileName == null) {
			throw new CustomException(ErrorCode.NOT_SUPPORT_LANGUAGE);
		}

		return baseWorkspaceDir + "/" + fileName;

	}

	/**
	 * 언어별 기본 디렉토리 루트 경로
	 * 예: "/app/"
	 */
	public String getBaseDirectory() {
		return baseWorkspaceDir;
	}
}
