package com.growlog.webide.workers.container;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ContainerManager {
	public static final String IMAGE_NAME = "openjdk:17";
	private final DockerClient dockerClient;
	private final String projectBasePath;
	private final String containerWorkspacePath;

	private final Cache<Long, String> activeProjectContainers;

	public ContainerManager(DockerClient dockerClient,
		@Value("${efs.base-path}") String projectBasePath,
		@Value("${container.workspace-path}") String containerWorkspacePath) {
		this.dockerClient = dockerClient;
		this.projectBasePath = projectBasePath;
		this.containerWorkspacePath = containerWorkspacePath;
		this.activeProjectContainers = buildContainerCache();
	}

	public String getOrCreateContainerForSession(Long sessionId, Long projectId) {
		try {
			return activeProjectContainers.get(sessionId, () -> {
				log.info("No active container for session {}. Creating a new one for project {}.", sessionId,
					projectId);
				return createAndStartContainerForProject(projectId);
			});
		} catch (ExecutionException e) {
			log.error("Failed to get or create container for session {}", sessionId, e);
			throw new RuntimeException(e.getCause());
		}
	}

	/**
	 * 한 세션의 컨테이너를 중지하고 영구적으로 삭제합니다.
	 * 이 메서드는 main-server의 SessionListener에 의해 호출됩니다.
	 * @param sessionId 정리할 컨테이너를 가진 세션 ID
	 */
	public void cleanupContainerForSession(Long sessionId) {
		log.info("Request to clean up container for session {}.", sessionId);
		activeProjectContainers.invalidate(sessionId);
	}

	private String createAndStartContainerForProject(Long projectId) {
		log.info("No active container for project {}. Creating a new one...", projectId);
		final Path projectPath = Paths.get(projectBasePath + "/" + String.valueOf(projectId));

		HostConfig hostConfig = new HostConfig().withBinds(
			new Bind(projectPath.toString(), new Volume(containerWorkspacePath))
		);

		String containerId = Strings.EMPTY;

		try {
			containerId = dockerClient.createContainerCmd(IMAGE_NAME)
				.withHostConfig(hostConfig)
				.withCmd("sleep", "infinity")
				.withWorkingDir(containerWorkspacePath)
				.exec().getId();

			dockerClient.startContainerCmd(containerId).exec();
			log.info("Successfully created and started container '{}' for project {}", containerId, projectId);
			return containerId;
		} catch (Exception e) {
			rollback(containerId);
			throw new RuntimeException("Failed to create container for project " + projectId, e);
		}
	}

	private void rollback(String containerId) {
		if (StringUtils.hasText(containerId)) {
			try {
				log.warn("[rollback]: Deleting created container '{}'", containerId);
				cleanupContainer(containerId);
			} catch (Exception rollbackException) {
				log.error("Failed to clean up container '{}' during rollback.", containerId, rollbackException);
			}
		}
	}

	private void cleanupContainer(String containerId) {
		try {
			log.info("Cleaning up container '{}'", containerId);
			dockerClient.stopContainerCmd(containerId).exec();
			dockerClient.removeContainerCmd(containerId).exec();
			log.info("Container '{}' was successfully stopped and removed.", containerId);
		} catch (NotFoundException e) {
			log.debug("Container '{}' was already removed.", containerId);
		} catch (Exception e) {
			log.error("Failed to clean up container '{}'. Manual check may be required.", containerId, e);
		}
	}

	private Cache<Long, String> buildContainerCache() {
		return CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES)
			.removalListener(this::handleContainerRemoval)
			.build();
	}

	// 캐시에서 항목이 제거될 때 (만료 또는 수동 제거 시) 컨테이너를 정리합니다.
	private void handleContainerRemoval(RemovalNotification<Long, String> notification) {
		final String containerId = notification.getValue();
		final Long sessionId = notification.getKey();
		log.info("Session {} has been idle. Releasing container '{}'.", sessionId, containerId);
		cleanupContainer(containerId);
	}

	/**
	 * 애플리케이션 종료 시 모든 활성 컨테이너를 정리합니다.
	 */
	@PreDestroy
	private void shutdown() {
		log.info("Shutting down ContainerManager. Cleaning up all active project containers...");
		activeProjectContainers.invalidateAll();
		log.info("All active containers have been cleaned up.");
	}
}
