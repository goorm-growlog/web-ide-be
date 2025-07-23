package com.growlog.webide.domain.projects.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateVolumeCmd;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.projects.dto.CreateProjectRequest;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.User;

@ExtendWith(MockitoExtension.class)
class WorkspaceManagerServiceTest {

	@InjectMocks
	private WorkspaceManagerService workspaceManagerService;

	@Mock
	private DockerClient dockerClient;
	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private ImageRepository imageRepository;

	@Test
	@DisplayName("프로젝트 생성 단위 테스트 - 성공 ")
	void createProject_Unit_Test_Success() {
		// given (준비): 테스트에 필요한 객체와 Mock의 행동을 정의합니다.

		// 1. 입력 데이터 준비
		User testUser = User.builder().username("tester").build();
		Long imageId = 1L;
		CreateProjectRequest request = new CreateProjectRequest("Unit Test Project", "Description", imageId);

		// 2. Mock 객체의 행동 정의 (Stubbing)
		// ImageRepository.findById가 호출되면, 가짜 Image 객체를 담은 Optional을 반환하도록 설정
		Image fakeImage = Image.builder()
			.imageName("Java")
			.version("17")
			.dockerBaseImage("openjdk:17")
			.build();
		when(imageRepository.findById(imageId)).thenReturn(Optional.of(fakeImage));

		// DockerClient.createVolumeCmd...가 호출되면, 단순히 비어있는 응답을 반환하도록 설정
		// (실제 Docker 명령이 실행되지 않도록 함)
		CreateVolumeCmd mockCreateVolumeCmd = mock(CreateVolumeCmd.class);
		when(dockerClient.createVolumeCmd()).thenReturn(mockCreateVolumeCmd);
		when(mockCreateVolumeCmd.withName(anyString())).thenReturn(mockCreateVolumeCmd);
		when(mockCreateVolumeCmd.exec()).thenReturn(new CreateVolumeResponse());

		// ProjectRepository.save가 어떤 Project 객체든 받아서 호출되면,
		// 그 받은 Project 객체를 그대로 반환하도록 설정 (실제 DB 저장 흉내)
		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
			Project projectToSave = invocation.getArgument(0);
			// 실제 DB처럼 ID를 세팅해주는 것까지 흉내낼 수 있습니다.
			// 여기서는 간단하게 받은 객체를 그대로 반환합니다.
			return projectToSave;
		});

		// when (실행): 테스트하려는 메소드를 호출합니다.
		Project createdProject = workspaceManagerService.createProject(request, testUser);

		// then (검증): 메소드 호출 후의 결과와 Mock 객체와의 상호작용을 검증합니다.

		// 1. 반환된 객체의 속성이 올바른지 검증
		assertThat(createdProject).isNotNull();
		assertThat(createdProject.getProjectName()).isEqualTo(request.getProjectName());
		assertThat(createdProject.getOwner()).isEqualTo(testUser);
		assertThat(createdProject.getImage()).isEqualTo(fakeImage);
		assertThat(createdProject.getStorageVolumeName()).startsWith("project-vol-");

		// 2. Mock 객체의 메소드가 예상대로 호출되었는지 검증 (가장 중요)

		// imageRepository의 findById가 정확히 1번 호출되었는지 검증
		verify(imageRepository, times(1)).findById(imageId);

		// dockerClient의 createVolumeCmd가 정확히 1번 호출되었는지 검증
		verify(dockerClient, times(1)).createVolumeCmd();

		// projectRepository의 save가 정확히 1번 호출되었는지 검증
		verify(projectRepository, times(1)).save(any(Project.class));

		// 3. (심화) projectRepository.save에 어떤 인자가 전달되었는지 캡처하여 검증
		ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
		verify(projectRepository).save(projectArgumentCaptor.capture()); // save에 전달된 Project 객체를 캡처

		Project capturedProject = projectArgumentCaptor.getValue();
		assertThat(capturedProject.getProjectName()).isEqualTo("Unit Test Project");
		assertThat(capturedProject.getOwner().getUsername()).isEqualTo("tester");
	}

}
