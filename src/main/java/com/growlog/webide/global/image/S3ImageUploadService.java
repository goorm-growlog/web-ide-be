package com.growlog.webide.global.image;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import lombok.RequiredArgsConstructor;


@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3ImageUploadService implements ImageUploadService {
	private final AmazonS3 amazonS3;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	@Override
	public String uploadProfileImage(MultipartFile file) {
		String key = "profile-images/" + UUID.randomUUID();
		try (InputStream is = file.getInputStream()) {
			ObjectMetadata meta = new ObjectMetadata();
			meta.setContentLength(file.getSize());
			meta.setContentType(file.getContentType());

			amazonS3.putObject(new PutObjectRequest(bucket, key, is, meta)
				.withCannedAcl(CannedAccessControlList.PublicRead));
		} catch (IOException e) {
			throw new RuntimeException("S3 업로드 실패", e);
		}
		return amazonS3.getUrl(bucket, key).toString();
	}
}
