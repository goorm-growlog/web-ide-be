package com.growlog.webide.domain.liveblocks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

@Component
public class RoomIdGenerator {

	@Value("${liveblocks.salt}")
	private String salt;

	// sha-256으로 해시해서 예측 불가능한 roomId로 바꿔 보안성 높임
	public String generateRoomId(String projectId, String filePath) {
		String raw = projectId + ":" + filePath + ":" + salt;
		return "room-" + sha256Hex(raw);
	}

	private String sha256Hex(String input) {
		try {
			// 입력 문자열을 SHA-256 해시로 변환
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

			// 바이트 배열을 16진수 문자열(hex) 로 바꿔서 리턴
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				hexString.append(String.format("%02x", b));
			}

			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			//해싱 실패시 예외 처리
			throw new CustomException(ErrorCode.SHA256_NOT_CREATED);
		}
	}
}
