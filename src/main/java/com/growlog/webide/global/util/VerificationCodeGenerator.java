package com.growlog.webide.global.util;

import java.security.SecureRandom;

public class VerificationCodeGenerator {
	private static final SecureRandom RANDOM = new SecureRandom();

	public static String generateCode() {
		return String.valueOf(RANDOM.nextInt(900000) + 100000); // 6자리 랜덤 숫자 생성
	}
}
