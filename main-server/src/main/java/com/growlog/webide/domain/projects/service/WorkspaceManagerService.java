package com.growlog.webide.domain.projects.service;

/* WorkspaceManagerService
 *  프로젝트 생성부터 사용자의 세션(컨테이너) 관리, 종료까지 전체적인 생명주기를 조율하고 관리
 * */

import static org.apache.commons.io.file.PathUtils.copyDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.projects.dto.CreateProjectRequest;
import com.growlog.webide.domain.projects.dto.ProjectResponse;
import com.growlog.webide.domain.projects.dto.UpdateProjectRequest;
import com.growlog.webide.domain.projects.entity.ActiveSession;
import com.growlog.webide.domain.projects.entity.MemberRole;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.entity.ProjectMembers;
import com.growlog.webide.domain.projects.repository.ActiveSessionRepository;
import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
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

	private final ActiveSessionRepository activeSessionRepository;
	private final DockerClientFactory dockerClientFactory;
	private final ProjectRepository projectRepository;
	private final ImageRepository imageRepository;
	private final UserRepository userRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectFileMetaService projectFileMetaService;
	private final WebSocketNotificationService webSocketNotificationService;

	private final String projectsBasePath;
	private final String templatesBasePath;
	private final String serverId;

	public WorkspaceManagerService(DockerClientFactory dockerClientFactory,
		ProjectRepository projectRepository,
		ActiveSessionRepository activeSessionRepository,
		ImageRepository imageRepository,
		UserRepository userRepository,
		ProjectMemberRepository projectMemberRepository,
		ProjectFileMetaService projectFileMetaService,
		WebSocketNotificationService webSocketNotificationService,
		@Value("${efs.base-path}") String projectsBasePath,
		@Value("${efs.templates-path}") String templatesBasePath,
		@Value("${SERVER_ID}") String serverId) {
		this.dockerClientFactory = dockerClientFactory;
		this.projectRepository = projectRepository;
		this.activeSessionRepository = activeSessionRepository;
		this.imageRepository = imageRepository;
		this.userRepository = userRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.projectFileMetaService = projectFileMetaService;
		this.webSocketNotificationService = webSocketNotificationService;
		this.projectsBasePath = projectsBasePath;
		this.templatesBasePath = templatesBasePath;
		this.serverId = serverId;
	}

	/*
	1. 프로젝트 생성 (Create Project)
	EFS 디렉토리 생성, 프로젝트 메타데이터 DB에 저장
	 */
	@Transactional
	public ProjectResponse createProject(CreateProjectRequest request, Long userId) {
		log.info("Create project '{}'", request.getProjectName());

		// 1. DB 조회
		Users owner = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		Image image = imageRepository.findById(request.getImageId())
			.orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

		// 2. Project 엔티티 생성 및 DB 저장
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

		// 3. ProjectMembers 엔티티 생성 및 DB 저장
		ProjectMembers member = ProjectMembers.builder()
			.user(owner)
			.role(MemberRole.OWNER)
			.build();
		createdProject.addProjectMember(member);
		projectMemberRepository.save(member);

		// 4. file metadata 저장
		projectFileMetaService.saveFileMetadataForProject(project, projectPath);

		return ProjectResponse.from(createdProject, owner);
	}

	/*
	2. 프로젝트 열기 (Open Project)
	EFS 프로젝트 접근 확인 및 실시간 세션 정보 기록

	사용자가 특정 프로젝트를 열기 위해 API를 호출하면, 시스템은 먼저 해당 프로젝트(EFS)에 대한 접근 권한 확인
	확인 후, 사용자의 실시간 연결 상태를 관리하기 위해 'active_sessions' 테이블에 세션 정보 기록
	 */
	@Transactional
	public void openProject(Long projectId, Long userId) {
		log.info("User '{}' is opening project '{}'", userId, projectId);

		// 프로젝트 정보 및 사용할 이미지 조회
		Users user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		projectMemberRepository.findByUserAndProject(user, project)
			.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

		// 프로젝트 상태 변경
		project.activate();

		// 활성 세션 정보(ActiveSession)를 DB에 기록
		ActiveSession session = ActiveSession.builder()
			.project(project)
			.user(user)
			.serverId(serverId)
			.build();
		activeSessionRepository.save(session);
	}

	/*
	프로젝트 비활성화 (Inactivate Project)

	프로젝트 소유자(OWNER)의 요청에 따라 프로젝트를 비활성화 상태로 변경
 	해당 프로젝트에 접속해 있는 모든 사용자의 세션 강제 종료 및 세션 종료 메시지 실시간 전달
	 */
	@Transactional
	public void inactivateProject(Long projectId, Long userId) {
		log.info("User '{}' is deactivating project '{}'", userId, projectId);

		final ProjectMembers member = projectMemberRepository.findByProject_IdAndUser_UserId(projectId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_A_MEMBER));

		if (member.getRole() != MemberRole.OWNER) {
			throw new CustomException(ErrorCode.NO_OWNER_PERMISSION);
		}

		final List<ActiveSession> activeSessions = activeSessionRepository.findAllByProject_Id(projectId);
		log.info("projectId: {}, activeSessions length: {}", projectId, activeSessions.toArray().length);

		for (ActiveSession activeSession : activeSessions) {
			final Long targetUserId = activeSession.getUser().getUserId();
			final String message = "Connection terminated by the project owner";
			log.info("inactivateProject: {}", message);
			webSocketNotificationService.sendSessionTerminationMessage(targetUserId, message);
		}

		activeSessionRepository.deleteAll(activeSessions);

		final Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

		project.inactivate();
		projectRepository.save(project);
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
		ActiveSession session = activeSessionRepository.findByContainerId(containerId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_CONTAINER_NOT_FOUND));

		// 2. 물리적인 Docker 컨테이너를 중지하고 제거
		removeContainerIfExists(dockerClient, containerId);

		// 3. DB에서 ActiveInstance 레코드 삭제
		activeSessionRepository.delete(session);

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

		// if (activeSessionRepository.countByProjectAndStatus(project, InstanceStatus.ACTIVE) > 0) {
		// 	throw new CustomException(ErrorCode.CONTAINER_STILL_RUNNING);
		// }

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
