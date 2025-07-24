package com.growlog.webide.domain.files.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

	@TempDir
	Path tempDir;

	@Mock
	private ProjectRepository projectRepository;

	private FileService fileService;

	private final Long PROJECT_ID = 42L;
	private final String VOLUME_NAME = "vol-test";

	@BeforeEach
	void setUp() throws Exception {
		// 1) 서비스 인스턴스 생성
		fileService = new FileService(projectRepository);

		// 2) @Value 로 주입되는 volumesHostPath 를 임시 디렉터리로 세팅
		ReflectionTestUtils.setField(
			fileService,
			"volumeHostPath",
			tempDir.toString()
		);

		// 3) mock ProjectRepository 반환값 세팅
		Project mockProject = mock(Project.class);
		when(mockProject.getStorageVolumeName()).thenReturn(VOLUME_NAME);
		when(projectRepository.findById(PROJECT_ID))
			.thenReturn(Optional.of(mockProject));

		// 4) Docker 볼륨 내부 데이터 루트 디렉터리 생성
		Files.createDirectories(tempDir.resolve(VOLUME_NAME).resolve("_data"));
	}

	@Test
	void createFile_FileSlashFolder_Success() {
		// 파일 생성
		fileService.createFileorDirectory(
			PROJECT_ID,
			new com.growlog.webide.domain.files.dto.CreateFileRequest(
				"/foo.txt", "file"
			)
		);
		File f = tempDir.resolve(VOLUME_NAME)
			.resolve("_data")
			.resolve("foo.txt")
			.toFile();
		assertThat(f).exists().isFile();

		// 폴더 생성
		fileService.createFileorDirectory(
			PROJECT_ID,
			new com.growlog.webide.domain.files.dto.CreateFileRequest(
				"/bar/baz", "folder"
			)
		);
		File d = tempDir.resolve(VOLUME_NAME)
			.resolve("_data")
			.resolve("bar")
			.resolve("baz")
			.toFile();
		assertThat(d).exists().isDirectory();
	}

	@Test
	void createFile_AlreadyExists_Throws() throws IOException {
		// 미리 foo.txt 생성
		Path foo = tempDir.resolve(VOLUME_NAME).resolve("_data").resolve("foo.txt");
		foo.getParent().toFile().mkdirs();
		foo.toFile().createNewFile();

		CustomException ex = assertThrows(
			CustomException.class,
			() -> fileService.createFileorDirectory(
				PROJECT_ID,
				new com.growlog.webide.domain.files.dto.CreateFileRequest(
					"/foo.txt", "file"
				)
			)
		);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_ALREADY_EXISTS);
	}

	@Test
	void deleteFileOrDirectory_FileAndFolder_Success() throws Exception {
		// 파일과 폴더 트리 생성
		Path base = tempDir.resolve(VOLUME_NAME).resolve("_data");
		Files.createDirectories(base.resolve("a/b"));
		Files.createFile(base.resolve("a/b/c.txt"));

		// 1) 파일만 삭제
		fileService.deleteFileorDirectory(PROJECT_ID, "a/b/c.txt");
		assertThat(base.resolve("a/b/c.txt")).doesNotExist();
		assertThat(base.resolve("a/b")).exists(); // 폴더는 남아 있어야

		// 2) 빈 폴더 삭제
		fileService.deleteFileorDirectory(PROJECT_ID, "a/b");
		assertThat(base.resolve("a/b")).doesNotExist();

		// 3) 루트 바로 아래 폴더 통째 삭제
		Files.createDirectories(base.resolve("x/y"));
		Files.createFile(base.resolve("x/y/z.txt"));
		fileService.deleteFileorDirectory(PROJECT_ID, "x");
		assertThat(base.resolve("x")).doesNotExist();
	}

	@Test
	void deleteFileOrDirectory_NotFound_Throws() {
		CustomException ex = assertThrows(
			CustomException.class,
			() -> fileService.deleteFileorDirectory(PROJECT_ID, "no/such.file")
		);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND);
	}

	@Test
	void moveFileOrDirectory_Success() throws Exception {
		Path base = tempDir.resolve(VOLUME_NAME).resolve("_data");
		Files.createDirectories(base.resolve("oldDir"));
		Files.createFile(base.resolve("oldDir/one.txt"));

		// 디렉터리 이동
		fileService.moveFileorDirectory(
			PROJECT_ID,
			new com.growlog.webide.domain.files.dto.MoveFileRequest(
				"/oldDir", "/newDir"
			)
		);
		assertThat(base.resolve("oldDir")).doesNotExist();
		assertThat(base.resolve("newDir/one.txt")).exists();

		// 파일 단일 이름 변경
		Files.createFile(base.resolve("file1.txt"));
		fileService.moveFileorDirectory(
			PROJECT_ID,
			new com.growlog.webide.domain.files.dto.MoveFileRequest(
				"/file1.txt", "/file2.txt"
			)
		);
		assertThat(base.resolve("file1.txt")).doesNotExist();
		assertThat(base.resolve("file2.txt")).exists();
	}

	@Test
	void moveFileOrDirectory_SourceNotFound_Throws() {
		CustomException ex = assertThrows(
			CustomException.class,
			() -> fileService.moveFileorDirectory(
				PROJECT_ID,
				new com.growlog.webide.domain.files.dto.MoveFileRequest(
					"/nope", "/dest"
				)
			)
		);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND);
	}

	@Test
	void moveFileOrDirectory_DestExists_Throws() throws Exception {
		Path base = tempDir.resolve(VOLUME_NAME).resolve("_data");
		Files.createFile(base.resolve("src.txt"));
		Files.createFile(base.resolve("dst.txt"));

		CustomException ex = assertThrows(
			CustomException.class,
			() -> fileService.moveFileorDirectory(
				PROJECT_ID,
				new com.growlog.webide.domain.files.dto.MoveFileRequest(
					"/src.txt", "/dst.txt"
				)
			)
		);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_ALREADY_EXISTS);
	}
}
