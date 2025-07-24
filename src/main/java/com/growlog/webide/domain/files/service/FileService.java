package com.growlog.webide.domain.files.service;

import java.io.File;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.files.dto.CreateFileRequest;
import com.growlog.webide.domain.files.dto.MoveFileRequest;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

	private final ProjectRepository projectRepository;

	@Value("${docker.volume.host-path:/var/lib/docker/volumes}")
	private String volumeHostPath;

	public void createFileorDirectory(Long projectId, CreateFileRequest request) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		String base = Paths.get(volumeHostPath, project.getStorageVolumeName(), "_data").toString();
		String rel = request.getPath().startsWith("/") ? request.getPath().substring(1) : request.getPath();
		File target = new File(base, rel);

		if (target.exists()) {
			throw new CustomException(ErrorCode.FILE_ALREADY_EXISTS);
		}

		try {
			boolean success;
			if ("file".equalsIgnoreCase(request.getType())) {
				File parent = target.getParentFile();
				if (parent != null && !parent.exists() && !parent.mkdirs()) {
					throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
				}
				success = target.createNewFile();
			} else if ("folder".equalsIgnoreCase(request.getType())) {
				success = target.mkdirs();
			} else {
				throw new CustomException(ErrorCode.BAD_REQUEST);
			}

			if (!success) {
				throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
			}
		} catch (CustomException ce) {
			throw ce;
		} catch (Exception e) {
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}
	}

	public void deleteFileorDirectory(Long projectId, String path) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		String base = Paths.get(volumeHostPath, project.getStorageVolumeName(), "_data").toString();
		String rel = path.startsWith("/") ? path.substring(1) : path;
		File target = new File(base, rel);

		if (!target.exists()) {
			throw new CustomException(ErrorCode.FILE_NOT_FOUND);
		}

		boolean deleted = deleteRecursively(target);
		if (!deleted) {
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

	}

	public void moveFileorDirectory(Long projectId, MoveFileRequest request) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		String base = Paths.get(volumeHostPath, project.getStorageVolumeName(), "_data")
			.toString();
		String from = request.getFromPath().startsWith("/")
			? request.getFromPath().substring(1)
			: request.getFromPath();
		String to = request.getToPath().startsWith("/")
			? request.getToPath().substring(1)
			: request.getToPath();

		File src = new File(base, from);

		if (!src.exists()) {
			throw new CustomException(ErrorCode.FILE_NOT_FOUND);
		}

		File dst = new File(base, to);
		if (dst.exists()) {
			throw new CustomException(ErrorCode.FILE_ALREADY_EXISTS);
		}

		File parent = dst.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}

		boolean renamed = src.renameTo(dst);
		if (!renamed) {
			throw new CustomException(ErrorCode.FILE_OPERATION_FAILED);
		}
	}

	private boolean deleteRecursively(File file) {
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					if (!deleteRecursively(child)) {
						return false;
					}
				}
			}
		}
		return file.delete();
	}
}
