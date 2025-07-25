package com.growlog.webide.global.docker;

public interface DockerCommandService {

	String readFileContent(String containerName, String filePathInContainer);

}
