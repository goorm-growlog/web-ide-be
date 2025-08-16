package com.growlog.webide.global.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TemporaryPasswordGenerator {
	private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
	private static final String DIGITS = "0123456789";
	private static final String SPECIAL = "!@#$%^&*";
	private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

	private static final int PASSWORD_LENGTH = 10;
	private static final SecureRandom RANDOM = new SecureRandom();

	public static String generateTemporaryPassword() {
		List<Character> passwordChars = new ArrayList<>();

		// 최소 한 글자씩 넣기(대문자, 소문자, 숫자, 특수문자)
		passwordChars.add(randomChar(UPPER));
		passwordChars.add(randomChar(LOWER));
		passwordChars.add(randomChar(DIGITS));
		passwordChars.add(randomChar(SPECIAL));

		// 나머지는 전체 조합에서 랜덤으로 채우기
		for (int i = 4; i < PASSWORD_LENGTH; i++) {
			passwordChars.add(randomChar(ALL));
		}

		// 순서 랜덤 섞기
		Collections.shuffle(passwordChars, RANDOM);

		// 문자 리스트를 문자열로 변환
		StringBuilder password = new StringBuilder();
		for (char c : passwordChars) {
			password.append(c);
		}

		return password.toString();
	}

	private static char randomChar(String source) {
		int index = RANDOM.nextInt(source.length());
		return source.charAt(index);
	}
}
