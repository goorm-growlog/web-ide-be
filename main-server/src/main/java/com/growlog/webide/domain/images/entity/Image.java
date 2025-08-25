package com.growlog.webide.domain.images.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "images", uniqueConstraints = {
	@UniqueConstraint(
		name = "uk_image_name_version",
		columnNames = {"image_name", "version"}
	)
})
public class Image {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "image_id")
	private Long id;

	@Column(name = "image_name", length = 50, nullable = false)
	private String imageName; // 예: "Java", "Python"

	@Column(length = 30, nullable = false)
	private String version; // 예: "17", "3.11"

	@Column(name = "docker_base_image", length = 100, nullable = false)
	private String dockerBaseImage; // 예: "openjdk:17", "python:3.11"

	@Column(name = "build_command", columnDefinition = "TEXT")
	private String buildCommand; // 예: "javac {filename}"

	@Column(name = "run_command", columnDefinition = "TEXT")
	private String runCommand; // 예: "java {mainClass}"

	@Column(name = "template_code", columnDefinition = "TEXT")
	private String templateCode;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Builder
	public Image(String imageName, String version, String dockerBaseImage, String buildCommand, String runCommand,
		String templateCode) {
		this.imageName = imageName;
		this.version = version;
		this.dockerBaseImage = dockerBaseImage;
		this.buildCommand = buildCommand;
		this.runCommand = runCommand;
		this.templateCode = templateCode;
	}
}
