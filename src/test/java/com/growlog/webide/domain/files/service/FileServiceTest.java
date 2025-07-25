package com.growlog.webide.domain.files.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.growlog.webide.domain.files.dto.FileOpenResponseDto;
import com.growlog.webide.domain.permissions.service.ProjectPermissionService;
import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.global.docker.DockerCommandService;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

	@InjectMocks
	private FileService fileService;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ActiveInstanceRepository activeInstanceRepository;

	@Mock
	private DockerCommandService dockerCommandService;

	@Mock
	private ProjectPermissionService permissionService;

	@Test
	void openFile_ok() {
		// given
		Long userId = 1L;
		Long projectId = 10L;
		String relativePath = "Main.java";

		Project mockProject = Project.builder()
			.owner(null)
			.projectName("Test Project")
			.storageVolumeName("volume-test")
			.image(null)
			.description("Test desc")
			.build();
		ReflectionTestUtils.setField(mockProject, "id", projectId);

		ActiveInstance mockInstance = ActiveInstance.builder()
			.containerId("container-123")
			.project(mockProject)
			.user(null) // 테스트 목적이므로 생략 가능
			.webSocketPort(3000)
			.build();

		given(projectRepository.findById(projectId)).willReturn(Optional.of(mockProject));
		given(activeInstanceRepository.findByUser_UserIdAndProject_Id(userId, projectId)).willReturn(
			Optional.of(mockInstance));
		given(dockerCommandService.readFileContent("container-123", relativePath)).willReturn("파일 내용");

		// when
		FileOpenResponseDto response = fileService.openFile(projectId, relativePath, userId);

		// then
		assertThat(response.getFileName()).isEqualTo("Main.java");
		assertThat(response.getContent()).isEqualTo("파일 내용");
		assertThat(response.isEditable()).isTrue();
	}

}
