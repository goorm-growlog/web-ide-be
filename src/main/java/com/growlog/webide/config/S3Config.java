package com.growlog.webide.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"prod"})
@Configuration
public class S3Config {

	private final String accessKey;
	private final String secretKey;
	private final String region;

	private S3Config(@Value("${spring.cloud.aws.credentials.access-key}") final String accessKey,
					 @Value("${spring.cloud.aws.credentials.secret-key}") final String secretKey,
					 @Value("${spring.cloud.aws.region.static}") final String region) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.region = region;
	}

	@Bean
	public AmazonS3 amazonS3() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
		return AmazonS3ClientBuilder.standard()
			.withRegion(region)
			.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
			.build();
	}
}
