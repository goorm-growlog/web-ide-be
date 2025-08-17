package com.growlog.webide.domain.files.entity;

import com.growlog.webide.domain.projects.entity.Project;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_meta")
@Getter
@NoArgsConstructor
public class FileMeta {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;
	private String name;
	private String path; // /src/main/File.java
	private String type; // file | folder

	// 추후 정렬, 필터 등에 유용
	private boolean deleted = false;

	@Builder
	public FileMeta(Long id, Project project, String name, String path, String type, boolean deleted) {
		this.id = id;
		this.project = project;
		this.name = name;
		this.path = path;
		this.type = type;
		this.deleted = deleted;
	}

	public static FileMeta of(Project project, String path, String type) {
		String name = path.substring(path.lastIndexOf('/') + 1);
		return new FileMeta(null, project, name, path, type, false);
	}

	public void markDeleted() {
		this.deleted = true;
	}

	public void updatePath(String newPath) {
		this.path = newPath;
		this.name = newPath.substring(newPath.lastIndexOf('/') + 1);
	}
}
