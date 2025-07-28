package com.growlog.webide.domain.projects.service;

/* WorkspaceManagerService
 *  프로젝트 생성부터 사용자의 세션(컨테이너) 관리, 종료까지 전체적인 생명주기를 조율하고 관리
 * */

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.projects.dto.CreateProjectRequest;
import com.growlog.webide.domain.projects.dto.OpenProjectResponse;
import com.growlog.webide.domain.projects.dto.ProjectResponse;
import com.growlog.webide.domain.projects.dto.UpdateProjectRequest;
import com.growlog.webide.domain.projects.entity.ActiveInstance;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.entity.ProjectStatus;
import com.growlog.webide.domain.projects.repository.ActiveInstanceRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.factory.DockerClientFactory;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class WorkspaceManagerService {

	private final DockerClientFactory dockerClientFactory;
	private final ProjectRepository projectRepository;
	private final ActiveInstanceRepository activeInstanceRepository;
	private final ImageRepository imageRepository;
	private final SessionScheduler sessionScheduler;

	public WorkspaceManagerService(DockerClientFactory dockerClientFactory, ProjectRepository projectRepository,
		ActiveInstanceRepository activeInstanceRepository, ImageRepository imageRepository,
		@Lazy SessionScheduler sessionScheduler) {
		this.dockerClientFactory = dockerClientFactory;
		this.projectRepository = projectRepository;
		this.activeInstanceRepository = activeInstanceRepository;
		this.imageRepository = imageRepository;
		this.sessionScheduler = sessionScheduler;
	}

	/*
	1. 프로젝트 생성 (Create Project)
	Docker 볼륨 생성, 프로젝트 메타데이터 DB에 저장
	 */
	public Project createProject(CreateProjectRequest request, Users owner) {
		log.info("Create project '{}', user '{}'", request.getProjectName(), owner.getName());

		DockerClient dockerClient = dockerClientFactory.buildDockerClient();

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

		try {
			dockerClient.close();
		} catch (IOException e) {
			log.warn("Error closing DockerClient", e);
		}

		return projectRepository.save(project);
	}


	/*
	2. 프로젝트 열기 (Open Project)
	사용자를 위한 격리된 컨테이너 생성, ActiveInstance 기록
	사용자가 특정 프로젝트를 열기 위해 API를 호출하면, 시스템은 해당 프로젝트의 공유 볼륨을 마운트한, 오직 그 사용자만을 위한
	새로운 격리된 Docker 컨테이너를 동적으로 실행하고, 그 세션 정보를 DB에 기록한 후, 접속 정보를 사용자에게 돌려줍니다.
	 */
	public OpenProjectResponse openProject(Long projectId, Users user) {
		log.info("User '{}' is opening project '{}'", user.getName(), projectId);

		// 1. 프로젝트 정보 및 사용할 이미지 조회
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

		// 해당 프로젝트의 삭제 작업이 예정되어 있으면 작업 취소
		sessionScheduler.cancelDeletion(user.getUserId(), projectId);

		// 2. 이미 해당 사용자의 활성 세션이 있는지 확인
		activeInstanceRepository.findByUserAndProject(user, project).ifPresent(activeInstance -> {
			throw new IllegalStateException("User already has an active session for this project.");
		});

		// 3. Docker 볼륨/이미지 정보 준비 및 포트 할당
		DockerClient dockerClient = dockerClientFactory.buildDockerClient();

		String volumeName = project.getStorageVolumeName();
		String imageName = project.getImage().getDockerBaseImage();

		ExposedPort internalPort = new ExposedPort(8080, InternetProtocol.TCP);
		Ports portBindings = new Ports();
		portBindings.bind(internalPort, Ports.Binding.empty());

		// 3-1. Docker 이미지가 로컬에 존재하는지 확인하고, 없으면 pull
		pullImageIfNotExists(dockerClient, imageName);

		// 4. Docker 컨테이너 생성 및 실행 (docker run...)
		// 3-1. 볼륨 마운트 설정 (-v 옵션)
		HostConfig hostConfig = new HostConfig()
			.withBinds(new Bind(volumeName, new Volume("/app")))
			.withPortBindings(portBindings);

		// 3-2. 컨테이너 생성
		CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
			.withHostConfig(hostConfig)
			.withExposedPorts(internalPort)
			.withTty(true)
			.withCmd("/bin/sh") // 컨테이너 실행 시 기본 명령어
			.exec();
		String containerId = container.getId();

		// 3-3. 컨테이너 시작
		dockerClient.startContainerCmd(containerId).exec();
		log.info("Container '{}' started", containerId);

		int assignedPort = -1;
		int maxRetries = 5; // 최대 5번까지 재시도
		long delayMillis = 200; // 매 시도 사이 0.2초 대기

		for (int i = 0; i < maxRetries; i++) {
			InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
			Ports.Binding[] bindings = inspectResponse.getNetworkSettings().getPorts().getBindings().get(internalPort);

			if (bindings != null && bindings.length > 0 && bindings[0].getHostPortSpec() != null) {
				assignedPort = Integer.parseInt(bindings[0].getHostPortSpec());
				log.info("Attempt {}: Container '{}' assigned to host port: {}", i + 1, containerId, assignedPort);
				break; // 성공! 루프를 빠져나감
			}

			log.warn("Attempt {}: Could not find port bindings yet for container '{}'. Retrying in {}ms...", i + 1, containerId, delayMillis);
			try {
				Thread.sleep(delayMillis); // 잠시 대기
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				// 대기 중 인터럽트가 발생하면, 정리하고 예외를 던짐
				removeContainerIfExists(dockerClient, containerId);
				throw new RuntimeException("Port binding check was interrupted for container: " + containerId, e);
			}
		}

		if (assignedPort == -1) {
			// 최대 재시도 횟수(5번)를 모두 소진했는데도 포트를 찾지 못한 경우
			removeContainerIfExists(dockerClient, containerId);
			throw new RuntimeException("Could not find port bindings for container: " + containerId + " after " + maxRetries + " retries.");
		}

		// 3-4. 실행된 컨테이너 검사해 실제로 할당된 포트 확인
		// InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
		// Ports.Binding[] bindings = inspectResponse.getNetworkSettings().getPorts().getBindings().get(internalPort);
		//
		// if (bindings == null || bindings.length == 0) {
		// 	removeContainerIfExists(dockerClient, containerId);
		// 	throw new RuntimeException("Could not find port bindings for container: " + containerId);
		// }
		// int assignedPort = Integer.parseInt(bindings[0].getHostPortSpec());
		// log.info("Container '{}' assigned to host port: {}", containerId, assignedPort);

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

		try {
			dockerClient.close();
		} catch (IOException e) {
			log.warn("Error closing DockerClient", e);
		}

		return new OpenProjectResponse(projectId, containerId, assignedPort);
	}

	/**
	 * 지정된 Docker 이미지가 로컬에 존재하지 않으면 Docker Hub에서 pull 합니다.
	 * @param imageName 확인할 Docker 이미지 이름 (예: "openjdk:17-jdk-slim")
	 */
	private void pullImageIfNotExists(DockerClient dockerClient, String imageName) {
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
			} finally {
				try {
					dockerClient.close();
				} catch (IOException ex) {
					log.warn("Error closing DockerClient after pulling image", ex);
				}
			}
		}
	}


	/*
	 * 3-1. 프로젝트 닫기 (Close Project): 세션 닫기 요청 처리
	 */
	public void closeProjectSession(Long projectId, Users user) {

		Project project = projectRepository.findById(projectId).orElseThrow();
		ActiveInstance instance = activeInstanceRepository.findByUserAndProject(user, project)
			.orElseThrow(() -> new IllegalArgumentException(
				"Active session not found for project " + projectId + "and user " + user));

		String containerId = instance.getContainerId();

		sessionScheduler.scheduleDeletion(containerId, user.getUserId(), projectId);
	}
	/*
	3-2. 컨테이너 삭제
	사용자의 컨테이너를 중지/제거하고, ActiveInstance 삭제
	 */

	@Transactional
	public void deleteContainer(String containerId) {
		log.info("Closing session for container '{}'", containerId);

		DockerClient dockerClient = dockerClientFactory.buildDockerClient();

		// 1. 컨테이너 ID로 DB에서 ActiveInstance 정보 조회
		ActiveInstance instance = activeInstanceRepository.findByContainerId(containerId)
			.orElseThrow(() -> new IllegalArgumentException("ActiveInstance not found: " + containerId));

		Project project = instance.getProject();
		int portToRelease = instance.getWebSocketPort();

		// 2. 물리적인 Docker 컨테이너를 중지하고 제거
		removeContainerIfExists(dockerClient, containerId);


		// 3. DB에서 ActiveInstance 레코드 삭제
		activeInstanceRepository.delete(instance);
		log.info("ActiveInstance deleted for container '{}'", containerId);

		// 5. 이 세션이 해당 프로젝트의 마지막 세션이었는지 확인
		if (activeInstanceRepository.countByProject(project) == 0) {
			project.deactivate();
			log.info("Project '{}' is now INACTIVE.", project.getProjectName());
		}

		try {
			dockerClient.close();
		} catch (IOException e) {
			log.warn("Error closing DockerClient", e);
		}
	}

	// 예외 상황에서 컨테이너 정리
	public void removeContainerIfExists(DockerClient dockerClient, String containerId) {
		if (containerId == null || containerId.isBlank()) {
			return;
		}
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
	}

	public void deleteProject(Long projectId) {
		log.info("Deleting project with ID: {}", projectId);

		// 1. DB에서 프로젝트 정보 조회
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

		// TODO: 삭제하려는 사용자가 프로젝트의 소유자인지 확인하는 권한 검사
		//  checkPermission(currentUser, project.getOwner());

		if (activeInstanceRepository.countByProject(project) > 0) {
			throw new IllegalStateException("Cannot delete project with active sessions running.");
		}
		String volumeName = project.getStorageVolumeName();

		// 2. db에서 프로젝트 레코드 먼저 삭제
		projectRepository.delete(project);
		log.info("Project record deleted from DB for volume: {}", volumeName);

		// 3. 물리적인 Docker 볼륨 삭제 (별도의 DockerClient 생성 및 사용)
		DockerClient dockerClient = dockerClientFactory.buildDockerClient();
		try {
			dockerClient.removeVolumeCmd(volumeName).exec();
			log.info("Docker volume '{}' successfully removed.", volumeName);
		} catch (NotFoundException e) {
			log.warn("Docker volume '{}' not found.", volumeName);
		} catch (Exception e) {
			log.error("Error removing docker volume '{}': {}", volumeName, e.getMessage());
			throw new RuntimeException("Failed to remove volume: " + volumeName, e);
		} finally {
			try {
				dockerClient.close();
			} catch (IOException e) {
				log.warn("Error closing DockerClient after deleting volume", e);
			}
		}
	}

	@Transactional
	public Project updateProject(Long projectId, UpdateProjectRequest request) {
		log.info("Updating project with ID: {}", projectId);

		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

		// checkPermission(currentUser, project.getOwner());

		project.updateDetails(request.getProjectName(), request.getDescription());
		return project;
	}

	public Project getProjectDetails(Long projectId) {
		log.info("Getting project details for project with ID: {}", projectId);

		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

		return project;
	}

}
