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

	@Override
	public void execInContainer(String containerId, String shellCommand) {
		// 테스트 시에는 실제로 아무 동작하지 않도록 no-op 또는 로깅만 합니다.
		System.out.printf("[Mock] execInContainer - containerId: %s, cmd: %s%n",
			containerId, shellCommand);
	}

	@Override
	public String execAndReturn(String containerId, String shellCommand) {
		System.out.printf("[Mock] execAndReturn - containerId: %s, cmd: %s%n",
			containerId, shellCommand);
		// 테스트를 위한 가짜 파일 목록 반환 (예시)
		return """
			/app
			/app/src
			/app/src/Main.java
			/app/README.md
			""";
	}

}
