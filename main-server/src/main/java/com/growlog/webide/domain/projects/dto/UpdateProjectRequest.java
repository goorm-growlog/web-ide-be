package com.growlog.webide.domain.projects.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProjectRequest {
	private String projectName;
	private String description;
}
