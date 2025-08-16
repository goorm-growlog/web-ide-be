package com.growlog.webide.global.image;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {
	String uploadProfileImage(MultipartFile file);
}
