package com.growlog.webide.domain.projects.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.ReflectionUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateVolumeCmd;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.InspectConfigCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.ListVolumesCmd;
import com.github.dockerjava.api.command.ListVolumesResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.Ports;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.projects.dto.CreateProjectRequest;
import com.growlog.webide.domain.projects.dto.OpenProjectResponse;
import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.entity.MemberRole;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.entity.ProjectStatus;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.ProjectMembers;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.factory.DockerClientFactory;

@ExtendWith(MockitoExtension.class)
class WorkspaceManagerServiceTest {

	@InjectMocks
	private WorkspaceManagerService workspaceManagerService;
	@Mock
	private DockerClientFactory dockerClientFactory;
	@Mock
	private DockerClient mockDockerClient;
	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private ImageRepository imageRepository;
	@Mock
	private ActiveInstanceRepository activeInstanceRepository;
	@Mock
	private SessionScheduler sessionScheduler;
	@Mock
	private UserRepository userRepository;
	@Mock
	private ProjectMemberRepository projectMemberRepository;

	// @BeforeEach
	// void setUp() {
	// 	when(dockerClientFactory.buildDockerClient()).thenReturn(mockDockerClient);
	// }

	@Test
	@DisplayName("프로젝트 생성 단위 테스트 - 성공 ")
	void createProject_Unit_Test_Success() {
		// given (준비): 테스트에 필요한 객체와 Mock의 행동을 정의합니다.

		// 1. 입력 데이터 준비
		Long userId = 1L;
		Long imageId = 1L;
		CreateProjectRequest request = new CreateProjectRequest("Unit Test Project", "Description", imageId);
		Users fakeUser = new Users();
		fakeUser.setName("test");
		fakeUser.setUserId(userId);
		Image fakeImage = Image.builder()
			.imageName("Java")
			.version("17")
			.dockerBaseImage("openjdk:17")
			.build();
		when(imageRepository.findById(imageId)).thenReturn(Optional.of(fakeImage));

		// 2. Mock 객체의 행동 정의 (Stubbing)
		when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
		when(imageRepository.findById(imageId)).thenReturn(Optional.of(fakeImage));

		// ProjectRepository.save가 어떤 projectMembers 객체든 받아서 호출되면, ID가 할당된 Project 객체를 반환하도록 설정
		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
				Project projectToSave = invocation.getArgument(0);
			return projectToSave;
		});

		// DockerClient.createVolumeCmd...가 호출되면, 단순히 비어있는 응답을 반환하도록 설정
		// (실제 Docker 명령이 실행되지 않도록 함)
		when(dockerClientFactory.buildDockerClient()).thenReturn(mockDockerClient);
		CreateVolumeCmd mockCreateVolumeCmd = mock(CreateVolumeCmd.class);
		when(mockDockerClient.createVolumeCmd()).thenReturn(mockCreateVolumeCmd);
		when(mockCreateVolumeCmd.withName(anyString())).thenReturn(mockCreateVolumeCmd);

		ListVolumesResponse mockListVolumesResponse = mock(ListVolumesResponse.class);
		when(mockListVolumesResponse.getVolumes()).thenReturn(Collections.emptyList());
		ListVolumesCmd mockListVolumesCmd = mock(ListVolumesCmd.class);
		when(mockListVolumesCmd.exec()).thenReturn(mockListVolumesResponse);
		when(mockDockerClient.listVolumesCmd()).thenReturn(mockListVolumesCmd);

		// when (실행): 테스트하려는 메소드를 호출합니다.
		Project createdProject = workspaceManagerService.createProject(request, userId);

		// then (검증): 메소드 호출 후의 결과와 Mock 객체와의 상호작용을 검증합니다.

		// 1. 반환된 객체의 속성이 올바른지 검증
		assertThat(createdProject).isNotNull();
		assertThat(createdProject.getProjectName()).isEqualTo(request.getProjectName());
		assertThat(createdProject.getOwner()).isEqualTo(fakeUser);
		assertThat(createdProject.getImage()).isEqualTo(fakeImage);
		assertThat(createdProject.getStorageVolumeName()).startsWith("project-vol-");

		ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
		ArgumentCaptor<ProjectMembers> projectMembersArgumentCaptor = ArgumentCaptor.forClass(ProjectMembers.class);

		InOrder inOrder = inOrder(projectRepository, projectMemberRepository);

		inOrder.verify(projectRepository, times(1)).save(projectArgumentCaptor.capture());
		inOrder.verify(projectMemberRepository, times(1)).save(projectMembersArgumentCaptor.capture());

		Project capturedProject = projectArgumentCaptor.getValue();
		assertThat(capturedProject.getProjectName()).isEqualTo("Unit Test Project");
		assertThat(capturedProject.getOwner()).isEqualTo(fakeUser);

		ProjectMembers capturedMember = projectMembersArgumentCaptor.getValue();
		assertThat(capturedMember.getUser()).isEqualTo(fakeUser);
		assertThat(capturedMember.getRole()).isEqualTo(MemberRole.OWNER);
		assertThat(capturedMember.getProject()).isEqualTo(capturedProject);

		// Mock 객체의 메소드가 예상대로 호출되었는지 검증
		verify(imageRepository, times(1)).findById(imageId);
		verify(dockerClientFactory, times(1)).buildDockerClient();
		verify(mockDockerClient, times(1)).createVolumeCmd();
		try {
			verify(mockDockerClient, times(1)).close();
		} catch (IOException e) {
			fail("IOException should not be thrown in mock close()");
		}

		// projectRepository의 save가 정확히 1번 호출되었는지 검증
		verify(projectRepository, times(1)).save(any(Project.class));
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
	 * 9. 자동 포트 할당
	 * */
	@Test
	@DisplayName("프로젝트 열기 단위 테스트 - 성공 ")
	void openProject_Unit_Test_Success() throws NoSuchFieldException {
		// given
		long projectId = 1L;
		long userId = 1L;
		String fakeProjectName = "fakeProject";
		String expectedImageName = "openjdk:17-jdk-slim";
		String expectedVolumeName = "project-vol-test";
		String fakeContainerId = "fake-container-id-12345";
		int assignedPortByDocker = 49152;

		Users fakeUser = new Users();
		fakeUser.setName("fakeUser");
		fakeUser.setUserId(userId);

		Image fakeImage = Image.builder().dockerBaseImage("openjdk:17-jdk-slim").build();
		Project fakeProject = Project.builder()
			.owner(fakeUser)
			.projectName(fakeProjectName)
			.image(fakeImage)
			.storageVolumeName(expectedVolumeName)
			.build();
		setEntityId(fakeProject, projectId);
		fakeProject.activate();

		ProjectMembers member = ProjectMembers.builder().user(fakeUser).role(MemberRole.OWNER).build();
		fakeProject.addProjectMember(member);

		// 1. repository mock 설정
		when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
		when(projectRepository.findById(anyLong())).thenReturn(Optional.of(fakeProject));
		when(activeInstanceRepository.findByUserAndProject(any(Users.class), any(Project.class)))
			.thenReturn(Optional.empty()); // 기존 세션 없음
		when(activeInstanceRepository.save(any(ActiveInstance.class)))
			.thenAnswer(inv -> inv.getArgument(0));
		when(projectMemberRepository.findByUserAndProject(any(Users.class), any(Project.class)))
			.thenReturn(Optional.of(member));

		// 2. DockerClient Mock 설정
		when(dockerClientFactory.buildDockerClient()).thenReturn(mockDockerClient);
		// 2-1. 이미지 확인(inspect)은 성공한다고 가정 (이미지가 로컬에 있음)
		when(mockDockerClient.inspectImageCmd(anyString()))
			.thenReturn(mock(com.github.dockerjava.api.command.InspectImageCmd.class));

		// 2-2. 컨테이너 생성(create) Mock 설정
		CreateContainerResponse mockContainerResponse = mock(CreateContainerResponse.class);
		when(mockContainerResponse.getId()).thenReturn(fakeContainerId);

		CreateContainerCmd mockCreateContainerCmd = mock(CreateContainerCmd.class);
		when(mockDockerClient.createContainerCmd(expectedImageName)).thenReturn(mockCreateContainerCmd);
		when(mockCreateContainerCmd.withHostConfig(any())).thenReturn(mockCreateContainerCmd);
		when(mockCreateContainerCmd.withExposedPorts(any(ExposedPort.class))).thenReturn(mockCreateContainerCmd);
		when(mockCreateContainerCmd.withTty(anyBoolean())).thenReturn(mockCreateContainerCmd);
		when(mockCreateContainerCmd.withCmd(anyString())).thenReturn(mockCreateContainerCmd);
		when(mockCreateContainerCmd.exec()).thenReturn(mockContainerResponse);

		// 2-3. 컨테이너 시작(start) Mock 설정
		StartContainerCmd mockStartContainerCmd = mock(StartContainerCmd.class);
		when(mockDockerClient.startContainerCmd(fakeContainerId)).thenReturn(mockStartContainerCmd);

		// 2-4. 컨테이너 검사(inspectContainerCmd) Mock 설정
		InspectContainerResponse mockInspectResponse = mock(InspectContainerResponse.class);
		NetworkSettings mockNetworkSettings = mock(NetworkSettings.class);
		Ports mockPorts = mock(Ports.class);
		Ports.Binding mockBinding = mock(Ports.Binding.class);

		when(mockBinding.getHostPortSpec()).thenReturn(String.valueOf(assignedPortByDocker));

		ExposedPort internalPort = new ExposedPort(8080, InternetProtocol.TCP);
		when(mockPorts.getBindings()).thenReturn(Map.of(internalPort, new Ports.Binding[] {mockBinding}));
		when(mockNetworkSettings.getPorts()).thenReturn(mockPorts);
		when(mockInspectResponse.getNetworkSettings()).thenReturn(mockNetworkSettings);

		InspectContainerCmd mockInspectCmd = mock(InspectContainerCmd.class);
		when(mockInspectCmd.exec()).thenReturn(mockInspectResponse);
		when(mockDockerClient.inspectContainerCmd(fakeContainerId)).thenReturn(mockInspectCmd);

		// when
		OpenProjectResponse response = workspaceManagerService.openProject(projectId, userId);

		// then
		// 1. 반환된 DTO 검증
		assertThat(response).isNotNull();
		assertThat(response.getProjectId()).isEqualTo(projectId);
		assertThat(fakeProject.getId()).isEqualTo(projectId);
		assertThat(response.getContainerId()).isEqualTo(fakeContainerId);
		assertThat(response.getWebSocketPort()).isEqualTo(assignedPortByDocker);

		// 2. Mock 객체 호출 검증
		verify(userRepository, times(1)).findById(userId);
		verify(projectRepository, times(1)).findById(projectId);
		verify(activeInstanceRepository, times(1)).findByUserAndProject(any(Users.class), any(Project.class));
		verify(dockerClientFactory, times(1)).buildDockerClient();
		verify(mockDockerClient, times(1)).startContainerCmd(fakeContainerId);
		verify(projectMemberRepository, times(1)).findByUserAndProject(fakeUser, fakeProject);
		try {
			verify(mockDockerClient, times(1)).close();
		} catch (IOException e) {
			fail("IIException should not be thrown in mock close");
		}

		// 3. ActiveInstance가 올바른 정보로 저장되었는지 ArgumentCaptor로 검증
		ArgumentCaptor<ActiveInstance> instanceCaptor = ArgumentCaptor.forClass(ActiveInstance.class);
		verify(activeInstanceRepository, times(1)).save(instanceCaptor.capture());
		ActiveInstance savedInstance = instanceCaptor.getValue();
		assertThat(savedInstance.getProject()).isEqualTo(fakeProject);
		assertThat(savedInstance.getUser()).isEqualTo(fakeUser);
		assertThat(savedInstance.getContainerId()).isEqualTo(fakeContainerId);
		assertThat(savedInstance.getWebSocketPort()).isEqualTo(assignedPortByDocker);

		// 4. 프로젝트의 상태가 ACTIVE로 변경되었는지 검증
		assertThat(fakeProject.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
	}

	@Test
	@DisplayName("프로젝트 열기 - 삭제 예약 취소 로직 호출 테스트")
	void openProject_Cancel_Scheduled_Deletion() throws NoSuchFieldException {
		// given
		long projectId = 1L;
		long userId = 1L;
		Users fakeUser = new Users();
		fakeUser.setName("test");
		fakeUser.setUserId(userId);

		Project fakeProject = Project.builder().build();
		setEntityId(fakeProject, projectId);

		ProjectMembers member = ProjectMembers.builder().user(fakeUser).role(MemberRole.OWNER).build();
		fakeProject.addProjectMember(member);

		when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
		when(projectRepository.findById(projectId)).thenReturn(Optional.of(fakeProject));
		when(activeInstanceRepository.findByUserAndProject(fakeUser, fakeProject))
			.thenReturn(Optional.empty());
		when(projectMemberRepository.findByUserAndProject(fakeUser, fakeProject))
			.thenReturn(Optional.of(member));

		// when
		try {
			workspaceManagerService.openProject(projectId, userId);
		} catch (Exception e) {
		}

		// then
		verify(sessionScheduler, times(1)).cancelDeletion(userId, projectId);
	}

	@Test
	@DisplayName("프로젝트 열기(openProject) 단위 테스트 - 이미 세션이 존재하여 실패")
	void openProject_Fail_SessionAlreadyExists() throws NoSuchFieldException {
		// given
		long projectId = 1L;
		long userId = 1L;
		String fakeProjectName = "fakeProjectName";
		String expectedVolumeName = "project-vol-test";
		String fakeContainerId = "fake-container-id-12345";
		Users fakeUser = new Users();
		fakeUser.setName("fakeUser");
		fakeUser.setUserId(userId);
		Image fakeImage = Image.builder().dockerBaseImage("openjdk:17-jdk-slim").build();
		Project fakeProject = Project.builder()
			.projectName(fakeProjectName)
			.image(fakeImage)
			.storageVolumeName(expectedVolumeName).build();
		ProjectMembers member = ProjectMembers.builder().user(fakeUser).role(MemberRole.OWNER).build();
		fakeProject.addProjectMember(member);

		setEntityId(fakeProject, projectId);
		ActiveInstance fakeInstance = ActiveInstance.builder().build(); // 비어있는 가짜 객체

		when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
		when(projectRepository.findById(anyLong())).thenReturn(Optional.of(fakeProject));
		when(activeInstanceRepository.findByUserAndProject(any(Users.class), any(Project.class)))
			.thenReturn(Optional.of(fakeInstance));
		when(projectMemberRepository.findByUserAndProject(any(Users.class), any(Project.class)))
			.thenReturn(Optional.of(member));

		// when & then
		// openProject 실행 시 IllegalStateException이 발생하는지 검증
		assertThatThrownBy(() -> workspaceManagerService.openProject(projectId, fakeUser.getUserId()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("User already has an active session for this project.");

		// save나 dockerClient 관련 메소드가 전혀 호출되지 않았는지 검증
		verify(activeInstanceRepository, never()).save(any());
		verify(dockerClientFactory, never()).buildDockerClient();

	}

	@Test
	@DisplayName("프로젝트 열기 단위테스트 - 실패(멤버 아님)")
	void openProject_Fail_NotMember() throws NoSuchFieldException {
		// given
		long projectId = 1L;
		long userId = 1L;

		Users fakeUser = new Users();
		fakeUser.setName("fakeUser");
		fakeUser.setUserId(userId);

		Project fakeProject = Project.builder().build();
		setEntityId(fakeProject, projectId);

		when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
		when(projectRepository.findById(projectId)).thenReturn(Optional.of(fakeProject));
		when(projectMemberRepository.findByUserAndProject(fakeUser, fakeProject))
			.thenReturn(Optional.empty());
		// when&then
		assertThatThrownBy(() -> workspaceManagerService.openProject(projectId, userId))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("User has no active session for this project.");

		verify(dockerClientFactory, never()).buildDockerClient();
		verify(activeInstanceRepository, never()).save(any(ActiveInstance.class));
	}

	// 리플렉션을 사용하여 엔티티의 ID를 강제로 설정하는 헬퍼 메소드
	private void setEntityId(Object entity, Long id) throws NoSuchFieldException {
		Field idField = entity.getClass().getDeclaredField("id");
		idField.setAccessible(true); // private 필드에 접근 가능하도록 설정
		ReflectionUtils.setField(idField, entity, id);
	}

	@Test
	@DisplayName("프로젝트 닫기 단위 테스트 - 삭제 예약 로직")
	void closeProjectSession_Schedule_Deletion() throws NoSuchFieldException {
		// given
		String containerId = "test-container-123";
		long userId = 1L;
		long projectId = 1L;

		Users fakeUser = new Users();
		fakeUser.setName("test");
		fakeUser.setUserId(userId);
		Project fakeProject = Project.builder().build();
		setEntityId(fakeProject, projectId);

		ActiveInstance fakeInstance = ActiveInstance.builder()
			.containerId(containerId)
			.user(fakeUser)
			.project(fakeProject)
			.build();

		// 서비스 로직이 ActiveInstance를 찾을 수 있도록 Mocking
		when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
		when(projectRepository.findById(projectId)).thenReturn(Optional.of(fakeProject));
		when(activeInstanceRepository.findByUserAndProject(fakeUser, fakeProject)).thenReturn(
			Optional.of(fakeInstance));
		// when
		workspaceManagerService.closeProjectSession(projectId, userId);

		// then
		// SessionScheduler의 scheduleDeletion 메소드가 올바른 인자들로 호출되었는지 검증
		ArgumentCaptor<String> containerIdCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> projectIdCaptor = ArgumentCaptor.forClass(Long.class);

		verify(sessionScheduler, times(1)).scheduleDeletion(
			containerIdCaptor.capture(),
			userIdCaptor.capture(),
			projectIdCaptor.capture()
		);

		// 캡처된 인자들이 우리가 예상한 값과 일치하는지 확인
		assertThat(containerIdCaptor.getValue()).isEqualTo(containerId);
		assertThat(userIdCaptor.getValue()).isEqualTo(userId);
		assertThat(projectIdCaptor.getValue()).isEqualTo(projectId);

		// 이 메소드에서 더 이상 delete가 호출되지 않는지 확인
		verify(activeInstanceRepository, never()).delete(any(ActiveInstance.class));
		verify(dockerClientFactory, never()).buildDockerClient();
	}

	@Test
	@DisplayName("프로젝트 닫기 단위 테스트 - 다른 사용자 남아있어 컨테이너 ACTIVE 유")
	void closeProjectSession_Success_OtherRemain() {

		// given
		int portToRelease = 9001;
		String containerId = "test-container-id-123";
		Users fakeUser = new Users();
		fakeUser.setName("test");
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
			.webSocketPort(portToRelease)
			.user(fakeUser)
			.build();

		// 1. Repository Mock 설정
		when(activeInstanceRepository.findByContainerId(containerId)).thenReturn(Optional.of(fakeInstance));
		when(activeInstanceRepository.countByProject(fakeProject)).thenReturn(1L); // 아직 다른 사용자 잔류

		// 2. DockerClient Mock 설정
		when(dockerClientFactory.buildDockerClient()).thenReturn(mockDockerClient);
		StopContainerCmd mockStopeCmd = mock(StopContainerCmd.class);
		RemoveContainerCmd mockRemoveCmd = mock(RemoveContainerCmd.class);
		when(mockDockerClient.stopContainerCmd(containerId)).thenReturn(mockStopeCmd);
		when(mockDockerClient.removeContainerCmd(containerId)).thenReturn(mockRemoveCmd);

		// when
		workspaceManagerService.deleteContainer(containerId);

		// then
		assertThat(fakeProject.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
		verify(mockDockerClient, times(1)).stopContainerCmd(containerId);
		verify(mockDockerClient, times(1)).removeContainerCmd(containerId);
		verify(activeInstanceRepository, times(1)).delete(fakeInstance);
		verify(activeInstanceRepository, times(1)).countByProject(fakeProject);

		try {
			verify(mockDockerClient, times(1)).close();
		} catch (IOException e) {
			fail("IIException should not be thrown in mock close");
		}
	}

	@Test
	@DisplayName("프로젝트 닫기 단위 테스트 - 마지막 사용자 종료하여 컨테이너 inactive")
	void closeProjectSession_Success_LastUser() {
		// given
		String containerId = "test-container-123";
		int portToRelease = 9001;
		Project fakeProject = Project.builder().projectName("test-project").build();
		fakeProject.activate(); // 초기 상태를 ACTIVE로 설정

		ActiveInstance fakeInstance = ActiveInstance.builder()
			.containerId(containerId)
			.project(fakeProject)
			.webSocketPort(portToRelease)
			.build();

		// 1. Repository Mock 설정
		when(activeInstanceRepository.findByContainerId(containerId)).thenReturn(Optional.of(fakeInstance));
		when(activeInstanceRepository.countByProject(fakeProject)).thenReturn(0L); // 남은 사용자 없음

		// 2. DockerClient Mock 설정
		when(dockerClientFactory.buildDockerClient()).thenReturn(mockDockerClient);
		StopContainerCmd mockStopCmd = mock(StopContainerCmd.class);
		RemoveContainerCmd mockRemoveCmd = mock(RemoveContainerCmd.class);
		when(mockDockerClient.stopContainerCmd(containerId)).thenReturn(mockStopCmd);
		when(mockDockerClient.removeContainerCmd(containerId)).thenReturn(mockRemoveCmd);

		// when
		workspaceManagerService.deleteContainer(containerId);

		// then
		assertThat(fakeProject.getStatus()).isEqualTo(ProjectStatus.INACTIVE);

		// 프로젝트의 deactivate 메소드가 '정확히 1번 호출되었는지' 검증
		verify(activeInstanceRepository, times(1)).findByContainerId(containerId);
		verify(mockDockerClient, times(1)).stopContainerCmd(containerId);
		verify(mockDockerClient, times(1)).removeContainerCmd(containerId);
		verify(activeInstanceRepository, times(1)).delete(fakeInstance);
		verify(activeInstanceRepository, times(1)).countByProject(fakeProject);

		try {
			verify(mockDockerClient, times(1)).close();
		} catch (IOException e) {
			fail("IIException should not be thrown in mock close");
		}
	}

	@Test
	@DisplayName("프로젝트 상세 정보 조회 성공")
	void getProjectDetails_Success() throws NoSuchFieldException {

		// given
		long projectId = 1L;
		long userId = 1L;
		Users fakeUser = new Users();
		fakeUser.setName("test");
		fakeUser.setUserId(userId);
		Image fakeImage = Image.builder().imageName("java").build();
		Project fakeProject = Project.builder()
			.projectName("test-project")
			.owner(fakeUser)
			.image(fakeImage)
			.build();

		when(projectRepository.findById(projectId)).thenReturn(Optional.of(fakeProject));

		// when
		Project resultProject = workspaceManagerService.getProjectDetails(projectId, userId);

		// then
		assertThat(resultProject).isEqualTo(fakeProject);
		assertThat(resultProject.getProjectName()).isEqualTo("test-project");
		assertThat(resultProject.getOwner().getName()).isEqualTo("test");
		verify(projectRepository, times(1)).findById(projectId);
	}

}
