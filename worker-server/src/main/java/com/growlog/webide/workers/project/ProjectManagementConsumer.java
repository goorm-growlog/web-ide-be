package com.growlog.webide.workers.project;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProjectManagementConsumer {

	private final ProjectManager projectManager;

	public ProjectManagementConsumer(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}

	@RabbitListener(queues = "${project-delete.rabbitmq.queue.name}")
	public void handleProjectDeleteRequest(Long projectId) {
		log.info("Received project delete request for project: {}", projectId);
		projectManager.handleProjectDeleteRequest(projectId);
	}

}
