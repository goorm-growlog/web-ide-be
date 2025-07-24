package com.growlog.webide.domain.projects.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.Disposables.never;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateVolumeCmd;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.projects.dto.CreateProjectRequest;
import com.growlog.webide.domain.projects.dto.OpenProjectResponse;
import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.entity.ProjectStatus;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
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
	@Mock
	private ActiveInstanceRepository activeInstanceRepository;

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

	/*
	* 1. DB에서 Project 정보를 올바르게 조회하는가?
	* 2. 중복된 활성 세션이 있는지 확인하는가?
	* 3. 필요한 경우 Docker 이미지를 pull 하는 로직을 호출하는가?
	* 4. 올바른 볼륨과 이미지 이름으로 Docker 컨테이너 생성을 요청하는가?
	* 5. 생성된 컨테이너를 시작하는가?
	* 6. ActiveInstance 정보를 올바르게 생성하여 DB에 저장을 요청하는가?
	* 7. Project의 상태를 ACTIVE로 변경하는가?
	* 8. 최종적으로 올바른 OpenProjectResponse를 반환하는가?
	* */
	@Test
	@DisplayName("프로젝트 열기 단위 테스트 - 성공 ")
	void openProject_Unit_Test_Success() throws NoSuchFieldException {
		// given
		long projectId = 1L;
		String fakeProjectName = "fakeProject";
		User testUser = User.builder().username("tester").build();
		String expectedImageName = "openjdk:17-jdk-slim";
		String expectedVolumeName = "project-vol-test";
		String fakeContainerId = "fake-container-id-12345";

		Image fakeImage = Image.builder().dockerBaseImage("openjdk:17-jdk-slim").build();
		Project fakeProject = Project.builder()
			.owner(testUser)
			.projectName(fakeProjectName)
			.image(fakeImage)
			.storageVolumeName(expectedVolumeName)
			.build();
		setEntityId(fakeProject, projectId);

		// 1. repository mock 설정
		when(projectRepository.findById(anyLong())).thenReturn(Optional.of(fakeProject));
		when(activeInstanceRepository.findByUserAndProject(testUser, fakeProject))
			.thenReturn(Optional.empty()); // 기존 세션 없음
		when(activeInstanceRepository.save(any(ActiveInstance.class)))
			.thenAnswer(inv -> inv.getArgument(0));

		// 2. DockerClient Mock 설정
		// 2-1. 이미지 확인(inspect)은 성공한다고 가정 (이미지가 로컬에 있음)
		when(dockerClient.inspectImageCmd(anyString()))
			.thenReturn(mock(com.github.dockerjava.api.command.InspectImageCmd.class));

		// 2-2. 컨테이너 생성(create) Mock 설정
		CreateContainerResponse mockContainerResponse = mock(CreateContainerResponse.class);
		when(mockContainerResponse.getId()).thenReturn(fakeContainerId);

		CreateContainerCmd mockCreateContainerCmd = mock(CreateContainerCmd.class);
		when(dockerClient.createContainerCmd(expectedImageName)).thenReturn(mockCreateContainerCmd);
		when(mockCreateContainerCmd.withHostConfig(any())).thenReturn(mockCreateContainerCmd);
		when(mockCreateContainerCmd.withTty(anyBoolean())).thenReturn(mockCreateContainerCmd);
		when(mockCreateContainerCmd.withCmd(anyString())).thenReturn(mockCreateContainerCmd);
		when(mockCreateContainerCmd.exec()).thenReturn(mockContainerResponse);

		// 2-3. 컨테이너 시작(start) Mock 설정
		StartContainerCmd mockStartContainerCmd = mock(StartContainerCmd.class);
		when(dockerClient.startContainerCmd(fakeContainerId)).thenReturn(mockStartContainerCmd);

		// when
		OpenProjectResponse response = workspaceManagerService.openProject(1L, testUser);

		// then
		// 1. 반환된 DTO 검증
		assertThat(response).isNotNull();
		assertThat(response.getProjectId()).isEqualTo(projectId);
		assertThat(fakeProject.getId()).isEqualTo(projectId);
		assertThat(response.getContainerId()).isEqualTo(fakeContainerId);
		assertThat(response.getWebSocketPort()).isEqualTo(9001); // 서비스 코드에 하드코딩된 임시 포트

		// 2. Mock 객체 호출 검증
		verify(projectRepository, times(1)).findById(anyLong());
		verify(activeInstanceRepository, times(1)).findByUserAndProject(testUser, fakeProject);
		verify(dockerClient, times(1)).createContainerCmd(expectedImageName);
		verify(dockerClient, times(1)).startContainerCmd(fakeContainerId);

		// 3. ActiveInstance가 올바른 정보로 저장되었는지 ArgumentCaptor로 검증 (심화)
		ArgumentCaptor<ActiveInstance> instanceCaptor = ArgumentCaptor.forClass(ActiveInstance.class);
		verify(activeInstanceRepository, times(1)).save(instanceCaptor.capture());

		ActiveInstance savedInstance = instanceCaptor.getValue();
		assertThat(savedInstance.getProject()).isEqualTo(fakeProject);
		assertThat(savedInstance.getUser()).isEqualTo(testUser);
		assertThat(savedInstance.getContainerId()).isEqualTo(fakeContainerId);

		// 4. 프로젝트의 상태가 ACTIVE로 변경되었는지 검증
		assertThat(fakeProject.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
	}

	@Test
	@DisplayName("프로젝트 열기(openProject) 단위 테스트 - 이미 세션이 존재하여 실패")
	void openProject_Fail_SessionAlreadyExists() throws NoSuchFieldException {
		// given
		long projectId = 1L;
		String fakeProjectName = "fakeProjectName";
		String expectedVolumeName = "project-vol-test";
		String fakeContainerId = "fake-container-id-12345";
		User testUser = User.builder().id(1L).username("tester").build();
		Image fakeImage = Image.builder().dockerBaseImage("openjdk:17-jdk-slim").build();
		Project fakeProject = Project.builder()
			.projectName(fakeProjectName)
			.image(fakeImage)
			.storageVolumeName(expectedVolumeName).build();

		setEntityId(fakeProject, projectId);
		ActiveInstance fakeInstance = ActiveInstance.builder().build(); // 비어있는 가짜 객체

		when(projectRepository.findById(anyLong())).thenReturn(Optional.of(fakeProject));
		// 이번에는 findByUserAndProject가 비어있지 않은 Optional을 반환하도록 설정
		when(activeInstanceRepository.findByUserAndProject(testUser, fakeProject))
			.thenReturn(Optional.of(fakeInstance));

		// when & then
		// openProject 실행 시 IllegalStateException이 발생하는지 검증
		assertThatThrownBy(() -> workspaceManagerService.openProject(projectId, testUser))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("User already has an active session for this project.");

		// save나 dockerClient 관련 메소드가 전혀 호출되지 않았는지 검증
		verify(activeInstanceRepository, Mockito.never()).save(any());
		verify(dockerClient, Mockito.never()).createContainerCmd(anyString());

	}

	// 리플렉션을 사용하여 엔티티의 ID를 강제로 설정하는 헬퍼 메소드
	private void setEntityId(Object entity, Long id) throws NoSuchFieldException {
		Field idField = entity.getClass().getDeclaredField("id");
		idField.setAccessible(true); // private 필드에 접근 가능하도록 설정
		ReflectionUtils.setField(idField, entity, id);
	}

	@Test
	@DisplayName("프로젝트 닫기 단위 테스트 - 다른 사용자 남아있음")
	void closeProjectSession_Success_OtherRemain() {

		// given
		String containerId = "test-container-id-123";
		User fakeUser = User.builder().username("tester").build();
		Image fakeImage = Image.builder().imageName("java").build();
		Project fakeProject = Project.builder()
			.projectName("test-project")
			.owner(fakeUser)
			.image(fakeImage)
			.build();
		fakeProject.activate(); // 초기 상태를 ACTIVE로 설정
		ActiveInstance fakeInstance = ActiveInstance.builder()
			.containerId(containerId)
			.project(fakeProject)
			.user(fakeUser)
			.build();

		// 1. Repository Mock 설정
		when(activeInstanceRepository.findByContainerId(containerId)).thenReturn(Optional.of(fakeInstance));
		when(activeInstanceRepository.countByProject(fakeProject)).thenReturn(1L); // 아직 다른 사용자 잔류

		// 2. DockerClient Mock 설정
		StopContainerCmd mockStopeCmd = mock(StopContainerCmd.class);
		RemoveContainerCmd mockRemoveCmd = mock(RemoveContainerCmd.class);
		when(dockerClient.stopContainerCmd(containerId)).thenReturn(mockStopeCmd);
		when(dockerClient.removeContainerCmd(containerId)).thenReturn(mockRemoveCmd);

		// when
		workspaceManagerService.closeProjectSession(containerId);

		// then
		assertThat(fakeProject.getStatus()).isNotEqualTo(ProjectStatus.INACTIVE);
		verify(activeInstanceRepository, Mockito.times(1)).delete(fakeInstance);
	}

	@Test
	@DisplayName("프로젝트 닫기 단위 테스트 - 마지막 사용자")
	void closeProjectSession_Success_LastUser() {
		// given
		String containerId = "test-container-123";
		Project fakeProject = Project.builder().projectName("test-project").build();
		fakeProject.activate(); // 초기 상태를 ACTIVE로 설정

		ActiveInstance fakeInstance = ActiveInstance.builder()
			.containerId(containerId)
			.project(fakeProject)
			.build();

		// 1. Repository Mock 설정
		when(activeInstanceRepository.findByContainerId(containerId)).thenReturn(Optional.of(fakeInstance));
		when(activeInstanceRepository.countByProject(fakeProject)).thenReturn(0L); // 남은 사용자 없음

		// 2. DockerClient Mock 설정
		StopContainerCmd mockStopCmd = mock(StopContainerCmd.class);
		RemoveContainerCmd mockRemoveCmd = mock(RemoveContainerCmd.class);
		when(dockerClient.stopContainerCmd(containerId)).thenReturn(mockStopCmd);
		when(dockerClient.removeContainerCmd(containerId)).thenReturn(mockRemoveCmd);

		// when
		workspaceManagerService.closeProjectSession(containerId);

		// then
		assertThat(fakeProject.getStatus()).isEqualTo(ProjectStatus.INACTIVE);

		// 프로젝트의 deactivate 메소드가 '정확히 1번 호출되었는지' 검증 (중요)
		verify(activeInstanceRepository, times(1)).findByContainerId(containerId);
		verify(activeInstanceRepository, times(1)).delete(fakeInstance);
		verify(dockerClient, times(1)).stopContainerCmd(containerId);
	}
}
