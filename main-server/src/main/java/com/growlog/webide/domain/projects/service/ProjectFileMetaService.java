package com.growlog.webide.domain.projects.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.growlog.webide.domain.files.entity.FileMeta;
import com.growlog.webide.domain.files.repository.FileMetaRepository;
import com.growlog.webide.domain.projects.entity.Project;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProjectFileMetaService {

	private final FileMetaRepository fileMetaRepository;

	public ProjectFileMetaService(FileMetaRepository fileMetaRepository) {
		this.fileMetaRepository = fileMetaRepository;
	}

	/**
	 * 특정 프로젝트의 모든 파일 및 디렉터리 메타데이터를 추출합니다.
	 */
	@Transactional
	public void saveFileMetadataForProject(Project project, Path projectPath) {
		try (Stream<Path> paths = Files.walk(projectPath)) {
			paths
				.map(path -> toFileMeta(project, projectPath, path))
				.forEach(fileMetaRepository::save);
		} catch (IOException ex) {
			log.error("Failed to read file metadata for project: {}", project.getId(), ex);
			throw new UncheckedIOException("Error reading project structure for project id: " + project.getId(), ex);
		}
	}

	private FileMeta toFileMeta(Project project, Path projectPath, Path path) {
		String type = Files.isDirectory(path) ? "folder" : "file";
		return FileMeta.absolutePath(project, projectPath.relativize(path).toString(), type);
	}

}
