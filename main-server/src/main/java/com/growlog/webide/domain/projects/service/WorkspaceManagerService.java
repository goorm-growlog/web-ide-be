package com.growlog.webide.domain.projects.service;

/* WorkspaceManagerService
 *  프로젝트 생성부터 사용자의 세션(컨테이너) 관리, 종료까지 전체적인 생명주기를 조율하고 관리
 * */

import static org.apache.commons.io.file.PathUtils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.PullResponseItem;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.projects.dto.CreateProjectRequest;
import com.growlog.webide.domain.projects.dto.OpenProjectResponse;
import com.growlog.webide.domain.projects.dto.ProjectResponse;
import com.growlog.webide.domain.projects.dto.UpdateProjectRequest;
import com.growlog.webide.domain.projects.entity.InstanceStatus;
import com.growlog.webide.domain.projects.entity.MemberRole;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.entity.ProjectMembers;
import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.terminal.entity.ActiveInstance;
import com.growlog.webide.domain.terminal.repository.ActiveInstanceRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.factory.DockerClientFactory;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;

import jakarta.transaction.Transactional;
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
	private final UserRepository userRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectFileMetaService projectFileMetaService;
	@Value("${efs.base-path}")
	private String projectsBasePath;
	@Value("${efs.templates-path}")
	private String templatesBasePath;

	public WorkspaceManagerService(DockerClientFactory dockerClientFactory,
		ProjectRepository projectRepository,
		ActiveInstanceRepository activeInstanceRepository,
		ImageRepository imageRepository,
		@Lazy SessionScheduler sessionScheduler,
		UserRepository userRepository,
		ProjectMemberRepository projectMemberRepository,
		ProjectFileMetaService projectFileMetaService) {
		this.dockerClientFactory = dockerClientFactory;
		this.projectRepository = projectRepository;
		this.activeInstanceRepository = activeInstanceRepository;
		this.imageRepository = imageRepository;
		this.sessionScheduler = sessionScheduler;
		this.userRepository = userRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.projectFileMetaService = projectFileMetaService;
	}

	/*
	1. 프로젝트 생성 (Create Project)
	Docker 볼륨 생성, 프로젝트 메타데이터 DB에 저장
	 */
	@Transactional
	public ProjectResponse createProject(CreateProjectRequest request, Long userId) {
		log.info("Create project '{}'", request.getProjectName());

		// 1. DB 조회
		Users owner = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		Image image = imageRepository.findById(request.getImageId())
			.orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

		// 3. Project 엔티티 생성 및 DB 저장
		Project project = Project.builder()
			.owner(owner)
			.projectName(request.getProjectName())
			.description(request.getDescription())
			.image(image)
			.build();
		Project createdProject = projectRepository.save(project);
		Long projectId = createdProject.getId();

		Path projectPath = Path.of(projectsBasePath, String.valueOf(projectId));
		Path templatePath = Path.of(templatesBasePath,
			String.valueOf(image.getImageName()) + "-" + String.valueOf(image.getVersion()));

		try {
			Files.createDirectories(projectPath);
			log.info("Created EFS directory for project '{}'", projectPath);

			if (Files.exists(templatePath) && Files.isDirectory(templatePath)) {
				copyDirectory(templatePath, projectPath);
				log.info("Copied template '{}' to '{}'", templatePath.getFileName(), projectPath);
			} else {
				log.warn("Template '{}' does not exist", templatePath.getFileName());
			}
		} catch (IOException e) {
			log.error("Failed to set up EFS directory for project: {}", projectId, e);
			throw new RuntimeException("Error during project file system setup for project id: " + projectId, e);
		}

		// 4. ProjectMembers 엔티티 생성 및 DB 저장
		ProjectMembers member = ProjectMembers.builder()
			.user(owner)
			.role(MemberRole.OWNER)
			.build();
		createdProject.addProjectMember(member);
		projectMemberRepository.save(member);

		// 5. file metadata 저장
		projectFileMetaService.saveFileMetadataForProject(project, projectPath);

		return ProjectResponse.from(createdProject, owner);
	}

	/*
	2. 프로젝트 열기 (Open Project)
	사용자를 위한 격리된 컨테이너 생성, ActiveInstance 기록
	사용자가 특정 프로젝트를 열기 위해 API를 호출하면, 시스템은 해당 프로젝트의 공유 볼륨을 마운트한, 오직 그 사용자만을 위한
	새로운 격리된 Docker 컨테이너를 동적으로 실행하고, 그 세션 정보를 DB에 기록한 후, 접속 정보를 사용자에게 돌려줍니다.
	 */
	public OpenProjectResponse openProject(Long projectId, Long userId) {
		log.info("User '{}' is opening project '{}'", userId, projectId);

		// 1. 프로젝트 정보 및 사용할 이미지 조회
		Users user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		projectMemberRepository.findByUserAndProject(user, project)
			.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

		// 4. 활성 세션 정보(ActiveInstance)를 DB에 기록
		ActiveInstance instance = ActiveInstance.builder()
			.project(project)
			.user(user)
			.status(InstanceStatus.ACTIVE)
			.build();
		activeInstanceRepository.save(instance);

		return new OpenProjectResponse(projectId, instance.getId());
	}

	/**
	 * 지정된 Docker 이미지가 로컬에 존재하지 않으면 Docker Hub에서 pull 합니다.
	 *
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
	public void closeProjectSession(Long projectId, Long userId) {
		Users user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
		ActiveInstance instance = activeInstanceRepository.findByUser_UserIdAndProject_Id(userId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		instance.disconnect(); // InstanceStatus.DISCONNECTING

		String containerId = instance.getContainerId();
		sessionScheduler.scheduleDeletion(containerId, user.getUserId(), projectId);

		// 이 세션이 해당 프로젝트의 마지막 활성 세션이었는지 확인
		if (activeInstanceRepository.countByProjectAndStatus(project, InstanceStatus.ACTIVE) == 0) {
			project.deactivate();
		}
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
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		// 2. 물리적인 Docker 컨테이너를 중지하고 제거
		removeContainerIfExists(dockerClient, containerId);

		// 3. DB에서 ActiveInstance 레코드 삭제
		activeInstanceRepository.delete(instance);

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

	public void deleteProject(Long projectId, Long userId) {
		log.info("Deleting project with ID: {}", projectId);

		// 1. DB에서 프로젝트 정보 조회
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		if (!project.getOwner().getUserId().equals(userId)) {
			throw new CustomException(ErrorCode.NO_OWNER_PERMISSION);
		}

		if (activeInstanceRepository.countByProjectAndStatus(project, InstanceStatus.ACTIVE) > 0) {
			throw new CustomException(ErrorCode.CONTAINER_STILL_RUNNING);
		}

		// 2. db에서 프로젝트 레코드 먼저 삭제
		projectRepository.delete(project);
	}

	@Transactional
	public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request, Long userId) {
		log.info("Updating project with ID: {}", projectId);

		Users user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		if (!project.getOwner().getUserId().equals(userId)) {
			throw new CustomException(ErrorCode.NO_OWNER_PERMISSION);
		}

		project.updateDetails(request.getProjectName(), request.getDescription());
		return ProjectResponse.from(project, user);
	}

	public ProjectResponse getProjectDetails(Long projectId, Long userId) {
		log.info("Getting project details for project with ID: {}", projectId);

		Users user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		return ProjectResponse.from(project, user);
	}

	public List<ProjectResponse> findProjectListByUser(Long userId, String filterType) {
		Users user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		List<ProjectMembers> members;

		if ("own".equalsIgnoreCase(filterType)) {
			members = projectMemberRepository.findByUserAndRole(user, MemberRole.OWNER);
		} else if ("joined".equalsIgnoreCase(filterType)) {
			members = projectMemberRepository.findByUser(user).stream()
				.filter(member -> member.getRole() != MemberRole.OWNER)
				.collect(Collectors.toList());
		} else {
			members = projectMemberRepository.findByUser(user);
		}

		return members.stream()
			.map(member -> ProjectResponse.from(member.getProject(), user))
			.collect(Collectors.toList());
	}

}
