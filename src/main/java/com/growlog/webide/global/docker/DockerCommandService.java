package com.growlog.webide.global.docker;

public interface DockerCommandService {

	// ✅ 볼륨 기반 (기존 방식)
	String readFileFromVolume(String volumeName, String filePathInContainer);

	// ✅ 컨테이너 기반 (ActiveInstance 용)
	String readFileContent(String containerId, String filePathInContainer); //activeInstance 도입하여 사용자 단위로 실행된 컨테이너로 연결

}
