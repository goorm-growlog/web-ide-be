package com.growlog.webide.domain.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenProjectResponse {

	public Long projectId;
	public String containerId;
	private Integer webSocketPort;
	private String liveblocksToken;
	private Long instanceId;
}
