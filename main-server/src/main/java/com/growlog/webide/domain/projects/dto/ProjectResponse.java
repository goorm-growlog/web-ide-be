package com.growlog.webide.domain.projects.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.users.entity.Users;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectResponse {
	private Long projectId;
	private String projectName;
	private String description;
	private String ownerName;
	private List<String> memberNames;
	private String myRole;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public static ProjectResponse from(Project project, Users user) {
		List<String> memberNames = project.getMembers().stream()
			.map(member -> member.getUser().getName())
			.collect(Collectors.toList());

		String role = project.getMembers().stream()
			.filter(member -> member.getUser().getUserId().equals(user.getUserId()))
			.findFirst()
			.map(member -> member.getRole().name())
			.orElse(null);

		return new ProjectResponse(
			project.getId(),
			project.getProjectName(),
			project.getDescription(),
			project.getOwner().getName(),
			memberNames,
			role,
			project.getStatus().toString(),
			project.getCreatedAt(),
			project.getUpdatedAt());
	}
}
