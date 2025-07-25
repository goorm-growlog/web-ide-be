package com.growlog.webide.global.docker;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class MockDockerCommandService implements DockerCommandService {
	@Override
	public String readFileContent(String containerName, String filePathInContainer) {
		return "// Mock file content for " + filePathInContainer;
	}
}
