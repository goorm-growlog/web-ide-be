package com.growlog.webide.domain.files.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.growlog.webide.domain.files.dto.CreateFileRequest;
import com.growlog.webide.domain.files.dto.FileOpenResponseDto;
import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
import com.growlog.webide.domain.files.entity.FileMeta;
import com.growlog.webide.domain.files.repository.FileMetaRepository;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.permissions.service.ProjectPermissionService;
import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.docker.DockerCommandService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

	@Mock // '가짜' 객체(스턴트 배우)를 만듭니다. DB 접근 등 실제 동작은 하지 않습니다.
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ProjectPermissionService permissionService;

	@Mock
	private FileMetaRepository fileMetaRepository;

	@InjectMocks
	private FileService fileService;

	private FileSystem jimfs;

	@BeforeEach
	void setup() throws IOException {
		// 메모리 파일시스템 생성
		jimfs = Jimfs.newFileSystem(Configuration.unix());

		fileService.setFileSystem(jimfs);

		// FileService의 efsBasePath를 jimfs 경로로 덮어쓰기
		ReflectionTestUtils.setField(fileService, "efsBasePath", "/app");
	}


	@AfterEach
	void tearDown() throws Exception {
		jimfs.close(); // 테스트 끝날 때마다 메모리 파일시스템 닫기
	}


	@Test
	@DisplayName("파일 생성 - 성공")
	void createFile_success() {
		// given
		log.info("--- TEST START: 파일이 없는 상태인지 먼저 검증합니다. ---");
		Path initialCheckPath = jimfs.getPath("/app/1/src/main.java");
		assertFalse(Files.exists(initialCheckPath), "테스트는 반드시 파일이 없는 상태에서 시작해야 합니다!");
		log.info("--- 검증 완료: 파일이 없음을 확인했습니다. 테스트를 계속합니다. ---");

		Long projectId = 1L;
		Long userId = 123L;
		String filePath = "src/main.java";
		CreateFileRequest request = new CreateFileRequest(filePath, "file");

		Users fakeOwner = Users.builder().build();
		Image fakeImage = Image.builder().build();
		Project fakeProject = Project.builder()
			.owner(fakeOwner).projectName("p").storageVolumeName("v").image(fakeImage)
			.build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);

		given(projectRepository.findById(projectId)).willReturn(Optional.of(fakeProject));
		given(fileMetaRepository.save(any(FileMeta.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		log.info("--- 이제 fileService.createFileorDirectory를 호출합니다. ---");
		fileService.createFileorDirectory(projectId, request, userId);

		// then
		Path expectedPath = jimfs.getPath("/app/1/src/main.java");
		assertTrue(Files.exists(expectedPath), "파일이 정상적으로 생성되어야 합니다.");
		then(fileMetaRepository).should(times(1)).save(any(FileMeta.class));
		then(messagingTemplate).should(times(1))
			.convertAndSend(eq("/topic/projects/" + projectId + "/tree"), any(WebSocketMessage.class));
	}

	@Test
	@DisplayName("파일 생성 - 실패 (파일이 이미 존재하는 경우)")
	void createFile_failed() throws Exception {
		// given - 테스트를 위한 환경과 데이터를 준비하는 단계
		Long projectId = 1L;
		Long userId = 123L;
		String filePath = "src/main.java";
		CreateFileRequest request = new CreateFileRequest(filePath, "file");
		Project fakeProject = Project.builder().build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);

		// 테스트를 위해 미리 가상 파일 시스템에 파일을 생성해 둠 (실패 조건)
		Path targetPath = jimfs.getPath("/app/1/src/main.java");
		Files.createDirectories(targetPath.getParent());
		Files.createFile(targetPath);

		// Mock 객체의 행동 정의
		given(projectRepository.findById(projectId)).willReturn(Optional.of(fakeProject));


		// when & then - 예외가 발생하는 것을 실행하고 동시에 검증하는 단계
		CustomException exception = assertThrows(CustomException.class, () -> {
			// 이 블록 안에서 예외가 발생할 것으로 기대함 (when)
			fileService.createFileorDirectory(projectId, request, userId);
		});

		// 발생한 예외가 올바른 종류인지 검증 (then)
		assertEquals(ErrorCode.FILE_ALREADY_EXISTS, exception.getErrorCode());
	}

	@Test
	@DisplayName("파일 삭제 - 성공")
	void deleteFile_success() throws Exception {
		//given
		Long projectId = 1L;
		long userId = 123L;
		String filePath = "src/main.java";

		Project fakeProject = Project.builder().build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);

		FileMeta fakeFileMeta = FileMeta.of(fakeProject, filePath, "file");

		//삭제할 파일을 미리 가상 파일 시스템에 생성
		Path targetPath = jimfs.getPath("/app/1/src/main.java");
		Files.createDirectories(targetPath.getParent());
		Files.createFile(targetPath);
		assertTrue(Files.exists(targetPath), "삭제 테스트를 위해 파일이 미리 존재해야 합니다.");

		// Mock 객체의 행동 정의
		given(projectRepository.findById(projectId)).willReturn(Optional.of(fakeProject));
		willDoNothing().given(permissionService).checkWriteAccess(fakeProject, userId); // BDDMockito 스타일
		given(fileMetaRepository.findByProjectIdAndPath(projectId, filePath)).willReturn(Optional.of(fakeFileMeta));
		given(fileMetaRepository.save(any(FileMeta.class))).willReturn(fakeFileMeta);

		// when - 실제 테스트할 메서드를 실행하는 단계
		fileService.deleteFileorDirectory(projectId, filePath, userId);

		// then - 실행 결과가 예상과 맞는지 검증하는 단계
		assertFalse(Files.exists(targetPath), "파일이 성공적으로 삭제되어야 합니다.");

		then(permissionService).should(times(1)).checkWriteAccess(fakeProject, userId);
		then(fileMetaRepository).should(times(1)).save(fakeFileMeta);
		assertTrue(fakeFileMeta.isDeleted(), "FileMeta의 isDeleted 플래그가 true여야 합니다.");
		then(messagingTemplate).should(times(1))
			.convertAndSend(eq("/topic/projects/" + projectId + "/tree"), any(WebSocketMessage.class));


	}

	@Test
	@DisplayName("디렉터리 삭제 - 성공 (하위 파일 포함)")
	void deleteDirectory_success() throws Exception {
		// given
		Long projectId = 1L;
		Long userId = 123L;
		String dirPath = "src";
		Project fakeProject = Project.builder().build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);
		FileMeta fakeDirMeta = FileMeta.of(fakeProject, dirPath, "folder");

		// 삭제할 디렉터리와 그 안의 파일을 미리 생성
		Path targetDir = jimfs.getPath("/app/1/src");
		Path innerFile = targetDir.resolve("main.java");
		Files.createDirectories(targetDir);
		Files.createFile(innerFile);
		assertTrue(Files.exists(targetDir) && Files.exists(innerFile), "테스트용 디렉터리와 파일이 존재해야 합니다.");

		given(projectRepository.findById(projectId)).willReturn(Optional.of(fakeProject));
		willDoNothing().given(permissionService).checkWriteAccess(fakeProject, userId);
		given(fileMetaRepository.findByProjectIdAndPath(projectId, dirPath)).willReturn(Optional.of(fakeDirMeta));

		// when
		fileService.deleteFileorDirectory(projectId, dirPath, userId);

		// then
		assertFalse(Files.exists(targetDir), "디렉터리가 삭제되어야 합니다.");
		assertFalse(Files.exists(innerFile), "디렉터리 내부 파일도 함께 삭제되어야 합니다.");
		then(fileMetaRepository).should(times(1)).save(fakeDirMeta);
		then(messagingTemplate).should(times(1)).convertAndSend(anyString(), any(WebSocketMessage.class));
	}

	@Test
	@DisplayName("파일 삭제 - 실패 (쓰기 권한 없음)")
	void deleteFile_fail_NoWritePermission() {
		// given
		Long projectId = 1L;
		Long userId = 123L;
		String filePath = "src/main.java";
		Project fakeProject = Project.builder().build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);

		given(projectRepository.findById(projectId)).willReturn(Optional.of(fakeProject));
		// permissionService.checkWriteAccess가 호출되면 예외를 던지도록 설정
		willThrow(new CustomException(ErrorCode.NO_WRITE_PERMISSION))
			.given(permissionService).checkWriteAccess(fakeProject, userId);

		// when & then
		CustomException exception = assertThrows(CustomException.class, () -> {
			fileService.deleteFileorDirectory(projectId, filePath, userId);
		});

		assertEquals(ErrorCode.NO_WRITE_PERMISSION, exception.getErrorCode());

		// 권한 체크에서 실패했으므로, 그 이후의 작업(DB 조회, 저장, 메시지 전송)은 절대 호출되면 안 됨
		then(fileMetaRepository).should(never()).findByProjectIdAndPath(anyLong(), anyString());
		then(fileMetaRepository).should(never()).save(any(FileMeta.class));
		then(messagingTemplate).should(never()).convertAndSend(anyString(), any(Objects.class));
	}


	/*@Test
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
	}*/

}
