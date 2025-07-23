package com.growlog.webide.domain.projects.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.users.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "projects")
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User owner;

	@Column(nullable = false)
	private String projectName;

	@Column(nullable = false, unique = true)
	private String storageVolumeName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "image_id", nullable = false)
	private Image image;

	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProjectStatus status;

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime updatedAt;

	@Builder
	public Project(User owner, String projectName, String storageVolumeName, Image image, String description) {
		this.owner = owner;
		this.projectName = projectName;
		this.storageVolumeName = storageVolumeName;
		this.image = image;
		this.description = description;
		this.status = ProjectStatus.INACTIVE; // 최초 생성 시 PENDING 상태
	}

	//== 상태 변경 로직 ==//
	public void activate() {
		this.status = ProjectStatus.ACTIVE;
	}

	public void deactivate() {
		this.status = ProjectStatus.INACTIVE;
	}
}
