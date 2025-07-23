package com.growlog.webide.domain.projects.service;

/* WorkspaceManagerService
 *  프로젝트 생성부터 사용자의 세션(컨테이너) 관리, 종료까지 전체적인 생명주기를 조율하고 관리
 * */

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.projects.dto.CreateProjectRequest;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceManagerService {

	private final DockerClient dockerClient;
	private final ProjectRepository projectRepository;
	private final ActiveInstanceRepository activeInstanceRepository;
	private final ImageRepository imageRepository;

	/*
	1. 프로젝트 생성 (Create Project)
	Docker 볼륨 생성, 프로젝트 메타데이터 DB에 저장
	 */
	public Project createProject(CreateProjectRequest request, User owner) {
		log.info("Create project '{}', user '{}'", request.getProjectName(), owner.getUsername());

		// 1. 사용할 개발 환경 이미지 조회
		Image image = imageRepository.findById(request.getImageId())
			.orElseThrow(() -> new IllegalArgumentException("Image not found"));

		// 2. 고유한 도커 볼륨 이름 생성 및 물리적 생성
		String volumeName = "project-vol-" + UUID.randomUUID();
		dockerClient.createVolumeCmd().withName(volumeName).exec();
		log.info("Docker volume create: {}", volumeName);

		// 3. Project 엔티티 생성 및 DB 저장
		Project project = Project.builder()
			.owner(owner)
			.projectName(request.getProjectName())
			.description(request.getDescription())
			.storageVolumeName(volumeName)
			.image(image)
			.build();

		return projectRepository.save(project);
	}


	/*
	2. 프로젝트 열기 (Open Project)
	사용자를 위한 격리된 컨테이너 생성, ActiveInstance 기록
	 */

	/*
	3. 프로젝트 닫기 (Close Project)
	사용자의 컨테이너를 중지/제거하고, ActiveInstance 삭제
	컨테이너 ID 기반으로 동작함
	 */
}
