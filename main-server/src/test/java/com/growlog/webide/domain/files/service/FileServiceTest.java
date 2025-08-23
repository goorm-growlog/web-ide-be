package com.growlog.webide.domain.files.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.jimfs.Jimfs;
import com.growlog.webide.domain.files.dto.CreateFileRequest;
import com.growlog.webide.domain.files.dto.tree.WebSocketMessage;
import com.growlog.webide.domain.files.entity.FileMeta;
import com.growlog.webide.domain.files.repository.FileMetaRepository;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.permissions.service.ProjectPermissionService;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

	public static final String ROOT_PATH = "/app";
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

	private FileSystem fileSystem;

	@BeforeEach
	void setup() {
		// memory file system
		this.fileSystem = Jimfs.newFileSystem();
		ReflectionTestUtils.setField(fileService, "fileSystem", this.fileSystem);
		ReflectionTestUtils.setField(fileService, "efsBasePath", ROOT_PATH);
	}

	@AfterEach
	void tearDown() throws Exception {
		fileSystem.close(); // 테스트 끝날 때마다 메모리 파일시스템 닫기
	}

	@Test
	@DisplayName("파일 생성 - 성공")
	void createFile_success() {
		// given
		log.info("--- TEST START: 파일이 없는 상태인지 먼저 검증합니다. ---");
		Path initialCheckPath = fileSystem.getPath("/app/1/src/main.java");
		assertFalse(Files.exists(initialCheckPath), "테스트는 반드시 파일이 없는 상태에서 시작해야 합니다!");
		log.info("--- 검증 완료: 파일이 없음을 확인했습니다. 테스트를 계속합니다. ---");

		Long projectId = 1L;
		Long userId = 123L;
		String filePath = "/src/main.java";
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
		fileService.createFileOrDirectory(projectId, request, userId);

		// then
		Path expectedPath = fileSystem.getPath("/app/1/src/main.java");
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
		String filePath = "/src/main.java";
		CreateFileRequest request = new CreateFileRequest(filePath, "file");
		Project fakeProject = Project.builder().build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);

		// 테스트를 위해 미리 가상 파일 시스템에 파일을 생성해 둠 (실패 조건)
		Path targetPath = fileSystem.getPath("/app/1/src/main.java");
		Files.createDirectories(targetPath.getParent());
		Files.createFile(targetPath);

		// Mock 객체의 행동 정의
		given(projectRepository.findById(projectId)).willReturn(Optional.of(fakeProject));

		// when & then - 예외가 발생하는 것을 실행하고 동시에 검증하는 단계
		CustomException exception = assertThrows(CustomException.class, () -> {
			// 이 블록 안에서 예외가 발생할 것으로 기대함 (when)
			fileService.createFileOrDirectory(projectId, request, userId);
		});

		// 발생한 예외가 올바른 종류인지 검증 (then)
		assertEquals(ErrorCode.FILE_ALREADY_EXISTS, exception.getErrorCode());
	}

	/*delete*/

	@Test
	@DisplayName("파일 삭제 - 성공")
	void deleteFile_success() throws Exception {
		// given
		Long projectId = 1L;
		long userId = 123L;
		String filePath = "/src/main.java";

		Project fakeProject = Project.builder().build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);

		FileMeta fakeFileMeta = FileMeta.relativePath(fakeProject, filePath, "file");

		// 가상 파일 시스템에 삭제할 파일 미리 생성
		Path targetPath = fileSystem.getPath("/app", String.valueOf(projectId), filePath);
		Files.createDirectories(targetPath.getParent());
		Files.createFile(targetPath);
		assertTrue(Files.exists(targetPath), "삭제 테스트를 위해 파일이 미리 존재해야 합니다.");

		// Mock 객체의 행동 정의
		given(projectRepository.findById(eq(projectId))).willReturn(Optional.of(fakeProject));
		willDoNothing().given(permissionService).checkWriteAccess(eq(userId), eq(fakeProject.getId()));

		given(fileMetaRepository.findByProjectIdAndPathAndDeletedFalse(eq(projectId), eq(filePath)))
			.willReturn(Optional.of(fakeFileMeta));

		// when
		fileService.deleteFileOrDirectory(projectId, filePath, userId);

		// then
		assertFalse(Files.exists(targetPath), "파일이 성공적으로 삭제되어야 합니다.");
		then(permissionService).should(times(1)).checkWriteAccess(eq(userId), eq(fakeProject.getId()));
		then(fileMetaRepository).should(times(1)).save(eq(fakeFileMeta));
		assertTrue(fakeFileMeta.isDeleted(), "FileMeta의 isDeleted 플래그가 true여야 합니다.");

		then(messagingTemplate).should(times(1))
			.convertAndSend(eq("/topic/projects/" + projectId + "/tree"), any(Object.class));
	}

	@Test
	@DisplayName("디렉터리 삭제 - 성공 (하위 파일 포함)")
	void deleteDirectory_success() throws Exception {
		// given
		Long projectId = 1L;
		Long userId = 123L;
		String dirPath = "/src";

		Project fakeProject = Project.builder().build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);
		FileMeta fakeDirMeta = FileMeta.relativePath(fakeProject, dirPath, "folder");

		// 삭제할 디렉터리와 그 안의 파일을 미리 생성
		Path targetDir = fileSystem.getPath("/app", String.valueOf(projectId), dirPath);
		Path innerFile = targetDir.resolve("main.java");
		Files.createDirectories(targetDir);
		Files.createFile(innerFile);
		assertTrue(Files.exists(targetDir) && Files.exists(innerFile), "테스트용 디렉터리와 파일이 존재해야 합니다.");

		given(projectRepository.findById(projectId)).willReturn(Optional.of(fakeProject));
		willDoNothing().given(permissionService).checkWriteAccess(userId, fakeProject.getId());
		given(fileMetaRepository.findByProjectIdAndPathAndDeletedFalse(eq(projectId), eq(dirPath)))
			.willReturn(Optional.of(fakeDirMeta));

		// when
		fileService.deleteFileOrDirectory(projectId, dirPath, userId);

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
		String filePath = "/src/main.java";
		Project fakeProject = Project.builder().build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);

		given(projectRepository.findById(projectId)).willReturn(Optional.of(fakeProject));
		// permissionService.checkWriteAccess가 호출되면 예외를 던지도록 설정
		willThrow(new CustomException(ErrorCode.NO_WRITE_PERMISSION))
			.given(permissionService).checkWriteAccess(userId, fakeProject.getId());

		// when & then
		CustomException exception = assertThrows(CustomException.class, () -> {
			fileService.deleteFileOrDirectory(projectId, filePath, userId);
		});

		assertEquals(ErrorCode.NO_WRITE_PERMISSION, exception.getErrorCode());

		// 권한 체크에서 실패했으므로, 그 이후의 작업(DB 조회, 저장, 메시지 전송)은 절대 호출되면 안 됨
		then(fileMetaRepository).should(never()).findByProjectIdAndPath(anyLong(), anyString());
		then(fileMetaRepository).should(never()).save(any(FileMeta.class));
		then(messagingTemplate).should(never()).convertAndSend(anyString(), any(Objects.class));
	}

	/*move*/
	@Test
	@DisplayName("폴더 이동 - 성공")
	void moveDirectory_success() throws Exception {
		// given
		Long projectId = 1L;
		Long userId = 123L;
		String fromPath = "/src";
		String toPath = "/source";

		Project fakeProject = Project.builder().build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);

		// 이동할 폴더와 그 안의 파일에 대한 FileMeta를 미리 준비
		FileMeta dirMeta = FileMeta.relativePath(fakeProject, fromPath, "folder");
		FileMeta fileMeta = FileMeta.relativePath(fakeProject, fromPath + "/main.java", "file");
		List<FileMeta> metasToMove = Arrays.asList(dirMeta, fileMeta);

		// 가상 파일 시스템에 실제 폴더와 파일을 생성
		Path sourceDir = fileSystem.getPath("/app", String.valueOf(projectId), fromPath);
		Path sourceFile = sourceDir.resolve("main.java");
		Files.createDirectories(sourceDir);
		Files.createFile(sourceFile);

		// Mock 객체 행동 정의
		given(projectRepository.findById(projectId)).willReturn(Optional.of(fakeProject));
		willDoNothing().given(permissionService).checkWriteAccess(userId, fakeProject.getId());
		given(fileMetaRepository.findByProjectIdAndPathStartingWith(projectId, fromPath)).willReturn(metasToMove);

		// when
		fileService.moveFileOrDirectory(projectId, fromPath, toPath, userId);

		// then
		// 1. 파일 시스템 검증
		Path targetDir = fileSystem.getPath("/app", String.valueOf(projectId), toPath);
		Path targetFile = targetDir.resolve("main.java");

		assertFalse(Files.exists(sourceDir), "원본 폴더는 삭제되어야 합니다.");
		assertTrue(Files.exists(targetDir), "대상 폴더가 생성되어야 합니다.");
		assertTrue(Files.exists(targetFile), "내부 파일도 함께 이동해야 합니다.");

		// 2. DB 저장 로직 검증 (ArgumentCaptor 사용)
		ArgumentCaptor<List<FileMeta>> captor = ArgumentCaptor.forClass(List.class);
		then(fileMetaRepository).should(times(1)).saveAll(captor.capture());

		List<FileMeta> savedMetas = captor.getValue();
		assertThat(savedMetas).hasSize(2); // 2개의 객체가 저장되었는지 확인
		// 각 객체의 경로가 올바르게 변경되었는지 확인
		assertThat(savedMetas).anyMatch(meta -> meta.getPath().equals(toPath)); // "source"
		assertThat(savedMetas).anyMatch(meta -> meta.getPath().equals(toPath + "/main.java")); // "source/main.java"

		// 3. WebSocket 메시지 전송 검증
		then(messagingTemplate).should(times(1)).convertAndSend(anyString(), any(Object.class));
	}

	@Test
	@DisplayName("폴더 이동 - 실패 (자신의 하위 폴더로 이동)")
	void moveDirectory_fail_CannotMoveToSubfolder() throws Exception {
		// given
		Long projectId = 1L;
		Long userId = 123L;
		String fromPath = "/src";
		String toPath = "/src/child"; // 자신의 하위 폴더로 이동 시도

		Project fakeProject = Project.builder().build();
		ReflectionTestUtils.setField(fakeProject, "id", projectId);

		Path sourceDir = fileSystem.getPath("/app", String.valueOf(projectId), fromPath);
		Files.createDirectories(sourceDir);

		given(projectRepository.findById(projectId)).willReturn(Optional.of(fakeProject));
		willDoNothing().given(permissionService).checkWriteAccess(userId, fakeProject.getId());

		// when & then
		CustomException exception = assertThrows(CustomException.class, () -> {
			fileService.moveFileOrDirectory(projectId, fromPath, toPath, userId);
		});

		assertEquals(ErrorCode.CANNOT_MOVE_TO_SUBFOLDER, exception.getErrorCode());

		// 실패했으므로 파일 시스템이나 DB에 어떤 변경도 일어나면 안 됨
		then(fileMetaRepository).should(never()).findByProjectIdAndPathStartingWith(anyLong(), anyString());
		then(fileMetaRepository).should(never()).saveAll(any());
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
