package com.growlog.webide.global.util;

/**
 * LanguageUtils 클래스는 파일 이름(특히 확장자)을 기반으로
 * 해당 파일이 어떤 프로그래밍 언어인지 추론하는 기능을 제공.
 * <p>
 * 예를 들어 "main.java" → "java", "app.py" → "python" 과 같이
 * 코드 에디터에서 문법 하이라이팅, 런타임 처리 등을 위해 사용.
 * <p>
 * 사용 예:
 * String language = LanguageUtils.detect("main.js");
 * // 결과: "javascript"
 */
public class LanguageUtils {

	public static String detect(String fileName) {
		if (fileName == null || !fileName.contains(".")) {
			return "plaintext";
		}

		String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

		return switch (extension) {
			case "java" -> "java";
			case "js" -> "javascript";
			case "ts" -> "typescript";
			case "py" -> "python";
			case "html" -> "html";
			case "css" -> "css";
			case "json" -> "json";
			case "xml" -> "xml";
			case "c" -> "c";
			case "cpp", "cc", "cxx" -> "cpp";
			case "cs" -> "csharp";
			case "rb" -> "ruby";
			case "go" -> "go";
			case "rs" -> "rust";
			case "php" -> "php";
			case "sh" -> "bash";
			case "kt" -> "kotlin";
			case "swift" -> "swift";
			default -> "plaintext";
		};
	}
}
