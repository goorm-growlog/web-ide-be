package com.growlog.webide.global.docker;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class MockDockerCommandService implements DockerCommandService {
	@Override
	public String readFileContent(String containerId, String filePathInContainer) {
		// 테스트 목적: 단순 고정 응답
		return "Mock Content from container";
	}

	@Override
	public String readFileFromVolume(String volumeName, String filePathInContainer) {
		// 테스트 목적: 단순 고정 응답
		return "Mock Content from volume";
	}

	@Override
	public void writeFileContent(String containerId, String filePathInContainer, String content) {
		System.out.printf("[Mock] 파일 저장됨 - containerId: %s, path: %s, content: %s%n",
			containerId, filePathInContainer, content);
	}
}
