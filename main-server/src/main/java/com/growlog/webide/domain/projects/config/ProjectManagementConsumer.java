package com.growlog.webide.domain.projects.config;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.growlog.webide.domain.files.repository.FileMetaRepository;
import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.projects.service.WorkspaceManagerService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProjectManagementConsumer {

	private final WorkspaceManagerService workspaceManagerService;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final FileMetaRepository fileMetaRepository;
	private final ProjectManagementProducer projectManagementProducer;

	public ProjectManagementConsumer(WorkspaceManagerService workspaceManagerService,
		ProjectRepository projectRepository,
		ProjectMemberRepository projectMemberRepository,
		FileMetaRepository fileMetaRepository, ProjectManagementProducer projectManagementProducer) {
		this.workspaceManagerService = workspaceManagerService;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.fileMetaRepository = fileMetaRepository;
		this.projectManagementProducer = projectManagementProducer;
	}

	@RabbitListener(queues = "${project-response.rabbitmq.queue.name.delete-success}")
	public void handleDeleteSuccess(Long projectId) {
		log.info("Received delete success for project {}", projectId);
		workspaceManagerService.terminateSessions(projectId);

		try {
			log.info("Waiting for 5 seconds to allow session cleanup...");
			Thread.sleep(5000L);
		} catch (InterruptedException e) {
			log.error("Thread sleep was interrupted", e);
			Thread.currentThread().interrupt();
		}

		log.info("Proceeding to request DB cleanup for project {}.", projectId);
		projectManagementProducer.requestCleanupProjectDb(projectId);
	}

	@RabbitListener(queues = "${project-db-cleanup.rabbitmq.queue.name}")
	public void handleDbCleanupRequest(Long projectId) {
		log.info("Received DB cleanup request for project {}", projectId);
		projectRepository.deleteById(projectId);
		projectMemberRepository.deleteByProject_Id(projectId);
		fileMetaRepository.deleteById(projectId);
	}

}
