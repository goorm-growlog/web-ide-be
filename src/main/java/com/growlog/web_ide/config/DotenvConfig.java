package com.growlog.web_ide.config;

import org.springframework.context.annotation.Configuration;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;

@Configuration
public class DotenvConfig {

	@PostConstruct
	public void init() {
		// .env 파일에서 환경 변수 로드

		Dotenv dotenv = Dotenv.configure()
			.directory("./") // 루트 디렉토리
			.ignoreIfMissing() // 파일 없어도 에러 X
			.load();

		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
		});
	}
}
