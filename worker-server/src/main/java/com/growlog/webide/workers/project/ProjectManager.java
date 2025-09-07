package com.growlog.webide.workers.project;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProjectManager {

	// TODO: application-prod.yml 파일?
	private final String efsBasePath;
	private final ProjectManagementProducer projectManagementProducer;

	public ProjectManager(@Value("${efs.base-path}") String efsBasePath,
		ProjectManagementProducer projectManagementProducer) {
		this.efsBasePath = efsBasePath;
		this.projectManagementProducer = projectManagementProducer;
	}

	public void handleProjectDeleteRequest(Long projectId) {
		if (projectId == null) {
			log.error("project id is empty");
			return;
		}

		String projectPath = efsBasePath + "/" + projectId;

		ProcessBuilder processBuilder = new ProcessBuilder("rm", "-rf", projectPath);

		try {
			log.info("Running rm command: {}", processBuilder.command());
			Process process = processBuilder.start();

			int exitCode = process.waitFor();

			if (exitCode == 0) {
				log.info("Successfully deleted EFS directory for project ID: {}", projectId);
				projectManagementProducer.sendDeleteSuccess(projectId);
			}

			if (exitCode != 0) {
				log.error("Failed to delete EFS directory for project ID: {}. Exit code: {}", projectId, exitCode);
				throw new RuntimeException("EFS deletion failed with exit code: " + exitCode);
			}
		} catch (IOException | InterruptedException e) {
			log.error("Failed to delete EFS directory for project ID: {}", projectId, e);
			Thread.currentThread().interrupt();
			throw new RuntimeException("EFS deletion process was interrupted or failed.", e);
		}
	}

}
