package com.growlog.webide.domain.chats.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.growlog.webide.domain.chats.dto.CodeLink;

public final class CodeLinkParser {

	private static final Pattern CODE_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(ide://([^:]+):(\\d+)\\)");

	private CodeLinkParser() {
	}

	// 문자열에서 [텍스트](ide://경로:줄번호) 패턴을 찾아 CodeLink 객체로 변환
	public static CodeLink parse(String content) {
		Matcher matcher = CODE_LINK_PATTERN.matcher(content);

		if (matcher.find()) {
			String text = matcher.group(1);
			String path = matcher.group(2);
			int line = Integer.parseInt(matcher.group(3));
			return new CodeLink(text, path, line);
		}
		return null;
	}
}
