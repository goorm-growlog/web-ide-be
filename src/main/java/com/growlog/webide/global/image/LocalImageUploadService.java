package com.growlog.webide.global.image;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@Profile("local")
@RequiredArgsConstructor
public class LocalImageUploadService implements ImageUploadService {

	@Value("${upload.profile.local-dir}")
	private String uploadDir;

	@Override
	public String uploadProfileImage(MultipartFile file) {
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null) {
			throw new RuntimeException("File name not found.");
		}

		// 확장자 분리
		String extension = "";
		int dotIndex = originalFilename.lastIndexOf(".");
		if (dotIndex != -1) {
			extension = originalFilename.substring(dotIndex); // ".png"
		}
		String baseName = originalFilename.substring(0, dotIndex)
			.replaceAll("[^a-zA-Z0-9]", "_");

		String filename = UUID.randomUUID() + "_" + baseName + extension;

		// ✅ 여기부터 중요!
		String rootPath = System.getProperty("user.dir"); // 프로젝트 루트
		String fullPath = rootPath + File.separator + uploadDir;

		File dir = new File(fullPath);
		if (!dir.exists() && !dir.mkdirs()) {
			throw new RuntimeException("Failed to create directory: " + fullPath);
		}

		File target = new File(fullPath, filename);

		try {
			file.transferTo(target); // 실제 파일 저장
		} catch (IOException e) {
			throw new RuntimeException("Failed to upload local file", e);
		}

		// 프론트에서 접근할 수 있는 URL 형식으로 반환 (테스트용)
		return "/test/profile/" + filename;
	}
}
