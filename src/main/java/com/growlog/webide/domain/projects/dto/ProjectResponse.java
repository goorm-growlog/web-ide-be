package com.growlog.webide.domain.projects.dto;

import java.time.LocalDateTime;

import com.growlog.webide.domain.projects.entity.Project;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectResponse {
	private Long projectId;
	private String projectName;
	private String description;
	private String ownerName;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public static ProjectResponse from(Project project) {
		return new ProjectResponse(
			project.getId(),
			project.getProjectName(),
			project.getDescription(),
			project.getOwner().getName(),
			project.getStatus().toString(),
			project.getCreatedAt(),
			project.getUpdatedAt());
		// TODO: members 추가
	}
}
