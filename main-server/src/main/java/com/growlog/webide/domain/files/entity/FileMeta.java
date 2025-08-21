package com.growlog.webide.domain.files.entity;

import java.time.LocalDateTime;

import com.growlog.webide.domain.projects.entity.Project;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_meta", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"project_id", "path", "deleted_at"})
})
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

	private LocalDateTime deletedAt;

	@Builder
	public FileMeta(Long id, Project project, String name, String path, String type, boolean deleted,
		LocalDateTime deletedAt) {
		this.id = id;
		this.project = project;
		this.name = name;
		this.path = path;
		this.type = type;
		this.deleted = deleted;
		this.deletedAt = deletedAt;
	}

	public static FileMeta relativePath(Project project, String path, String type) {
		String name = path.substring(path.lastIndexOf('/') + 1);
		return new FileMeta(null, project, name, path, type, false, null);
	}

	public static FileMeta absolutePath(Project project, String path, String type) {
		return FileMeta.relativePath(project, "/" + path, type);
	}

	public void markDeleted() {
		this.deleted = true;
		this.deletedAt = LocalDateTime.now();
	}

	public void updatePath(String newPath) {
		this.path = newPath;
		this.name = newPath.substring(newPath.lastIndexOf('/') + 1);
	}
}
