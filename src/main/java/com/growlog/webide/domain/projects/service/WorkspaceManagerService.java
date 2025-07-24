package com.growlog.webide.domain.projects.service;

/* WorkspaceManagerService
 *  프로젝트 생성부터 사용자의 세션(컨테이너) 관리, 종료까지 전체적인 생명주기를 조율하고 관리
 * */

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.projects.dto.CreateProjectRequest;
import com.growlog.webide.domain.projects.dto.OpenProjectResponse;
import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.entity.ProjectStatus;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.Users;

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
	private final SessionScheduler sessionScheduler;
	// TODO: PortManager, LiveblocksService 등 주입

	/*
	1. 프로젝트 생성 (Create Project)
	Docker 볼륨 생성, 프로젝트 메타데이터 DB에 저장
	 */
	public Project createProject(CreateProjectRequest request, Users owner) {
		log.info("Create project '{}', user '{}'", request.getProjectName(), owner.getUsername());

		// 1. 사용할 개발 환경 이미지 조회
		Image image = imageRepository.findById(request.getImageId())
			.orElseThrow(() -> new IllegalArgumentException("Image not found"));

		// 2. 고유한 도커 볼륨 이름 생성 및 물리적 생성
		String volumeName = "project-vol-" + UUID.randomUUID();
		try {
			dockerClient.createVolumeCmd().withName(volumeName).exec();
			log.info("Docker volume create: {}", volumeName);

			dockerClient.listVolumesCmd().exec().getVolumes().forEach(volume -> {
				if (volume.getName().equals(volumeName)) {
					System.out.println("생성된 볼륨 정보:");
					System.out.println("  이름: " + volume.getName());
					System.out.println("  드라이버: " + volume.getDriver());
					System.out.println("  마운트 포인트: " + volume.getMountpoint());
				}
			});
		} catch (Exception e) {
			System.err.println("볼륨 생성 오류: " + e.getMessage());
		}

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
	사용자가 특정 프로젝트를 열기 위해 API를 호출하면, 시스템은 해당 프로젝트의 공유 볼륨을 마운트한, 오직 그 사용자만을 위한
	새로운 격리된 Docker 컨테이너를 동적으로 실행하고, 그 세션 정보를 DB에 기록한 후, 접속 정보를 사용자에게 돌려줍니다.
	 */
	public OpenProjectResponse openProject(Long projectId, Users user) {
		log.info("User '{}' is opening project '{}'", user.getUsername(), projectId);

		// 1. 프로젝트 정보 및 사용할 이미지 조회
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

		// 해당 프로젝트의 삭제 작업이 예정되어 있으면 작업 취소
		sessionScheduler.cancelDeletion(user.getId(), projectId);

		// 2. 이미 해당 사용자의 활성 세션이 있는지 확인
		activeInstanceRepository.findByUserAndProject(user, project).ifPresent(activeInstance -> {
			throw new IllegalStateException("User already has an active session for this project.");
		});

		// 3. Docker 볼륨/이미지 정보 준비 및 포트 할당
		String volumeName = project.getStorageVolumeName();
		String imageName = project.getImage().getDockerBaseImage();
		// TODO: PortManager 통해 사용 가능한 포트를 동적으로 할당 받아야 함
		int assignedPort = 9001;

		// 3-1. Docker 이미지가 로컬에 존재하는지 확인하고, 없으면 pull
		pullImageIfNotExists(imageName);

		// 4. Docker 컨테이너 생성 및 실행 (docker run...)
		// 3-1. 볼륨 마운트 설정 (-v 옵션)
		HostConfig hostConfig = new HostConfig()
			.withBinds(new Bind(volumeName, new Volume("/app")))
			.withPortBindings(new PortBinding(Ports.Binding.bindPort(assignedPort), new ExposedPort(80)));

		// 3-2. 컨테이너 생성
		CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
			.withHostConfig(hostConfig)
			.withTty(true)
			.withCmd("/bin/sh") // 컨테이너 실행 시 기본 명령어
			.exec();
		String containerId = container.getId();

		// 3-3. 컨테이너 시작
		dockerClient.startContainerCmd(containerId).exec();
		log.info("Container '{}' started", containerId);

		// 4. 활성 세션 정보(ActiveInstance)를 DB에 기록
		ActiveInstance instance = ActiveInstance.builder()
			.project(project)
			.user(user)
			.containerId(containerId)
			.webSocketPort(assignedPort)
			.build();
		activeInstanceRepository.save(instance);

		// 5. 프로젝트 상태 변경
		if (project.getStatus() == ProjectStatus.INACTIVE) {
			project.activate();
		}

		// TODO: LiveblocksService 통해 실제 토큰 발급
		String liveblocksToken = "dummy-liveblocks-token-for-" + user.getUsername();

		return new OpenProjectResponse(projectId, containerId, assignedPort, liveblocksToken);
	}

	/**
	 * 지정된 Docker 이미지가 로컬에 존재하지 않으면 Docker Hub에서 pull 합니다.
	 * @param imageName 확인할 Docker 이미지 이름 (예: "openjdk:17-jdk-slim")
	 */
	private void pullImageIfNotExists(String imageName) {
		try {
			// 1. 이미지가 로컬에 있는지 먼저 검사합니다.
			dockerClient.inspectImageCmd(imageName).exec();
			log.info("Image '{}' already exists locally.", imageName);
		} catch (NotFoundException e) {
			// 2. NotFoundException이 발생하면 이미지가 없는 것이므로 pull을 시작합니다.
			log.info("Image '{}' not found locally. Pulling from Docker Hub...", imageName);
			try {
				// pullImageCmd는 비동기로 동작하므로, 완료될 때까지 기다려야 합니다.
				dockerClient.pullImageCmd(imageName)
					.exec(new ResultCallback.Adapter<PullResponseItem>() {
						@Override
						public void onNext(PullResponseItem item) {
							// pull 진행 상태를 로그로 남길 수 있습니다.
							log.debug(item.getStatus());
						}
					}).awaitCompletion(5, TimeUnit.MINUTES); // 최대 5분까지 기다립니다.

				log.info("Image '{}' pulled successfully.", imageName);
			} catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				log.error("Image pull for '{}' was interrupted.", imageName);
				throw new RuntimeException("Image pull was interrupted", interruptedException);
			}
		}
	}


	/*
	3-1. 프로젝트 닫기 (Close Project)
	세션 닫기 요청 처리
	 */
	public void closeProjectSession(String containerId) {

		activeInstanceRepository.findByContainerId(containerId).ifPresent(instance -> {
			sessionScheduler.scheduleDeletion(
				instance.getContainerId(), instance.getUser().getId(), instance.getProject().getId()
			);
		});
	}
	/*
	3-2. 컨테이너 삭제
	사용자의 컨테이너를 중지/제거하고, ActiveInstance 삭제
	 */

	@Transactional
	public void deleteContainer(String containerId) {
		log.info("Closing session for container '{}'", containerId);

		// 1. 컨테이너 ID로 DB에서 ActiveInstance 정보 조회
		ActiveInstance instance = activeInstanceRepository.findByContainerId(containerId)
			.orElseThrow(() -> new IllegalArgumentException("ActiveInstance not found: " + containerId));

		Project project = instance.getProject();
		int portToRelease = instance.getWebSocketPort();

		// 2. 물리적인 Docker 컨테이너를 중지하고 제거
		try {
			log.info("Stopping container '{}'", containerId);
			dockerClient.stopContainerCmd(containerId).exec();
			log.info("Removing container '{}'", containerId);
			dockerClient.removeContainerCmd(containerId).exec();
		} catch (NotFoundException e) {
			log.warn("Container '{}' not found.", containerId);
		} catch (Exception e) {
			log.error("Error while removing container '{}': {}", containerId, e.getMessage());
			throw new RuntimeException("Failed to remove container: " + containerId, e);
		}

		// 3. DB에서 ActiveInstance 레코드 삭제
		activeInstanceRepository.delete(instance);
		log.info("ActiveInstance deleted for container '{}'", containerId);

		// TODO: PortManager 통해 사용했던 포트 반납
		// portManager.releasePort(portToRelease);
		// log.info("Port {} released", portToRelease);

		// 5. 이 세션이 해당 프로젝트의 마지막 세션이었는지 확인
		if (activeInstanceRepository.countByProject(project) == 0) {
			project.deactivate();
			log.info("Project '{}' is now INACTIVE.", project.getProjectName());
		}
	}
}
