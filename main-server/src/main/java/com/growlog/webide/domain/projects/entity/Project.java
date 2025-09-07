package com.growlog.webide.domain.projects.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.users.entity.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "projects")
public class Project {

	@OneToMany(mappedBy = "project")
	private final List<ProjectMembers> members = new ArrayList<>();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "project_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "create_user_id", nullable = false)
	private Users owner;

	@Column(nullable = false)
	private String projectName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "image_id", nullable = false)
	private Image image;

	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProjectStatus status;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Builder
	public Project(Users owner, String projectName, String storageVolumeName, Image image, String description) {
		this.owner = owner;
		this.projectName = projectName;
		this.image = image;
		this.description = description;
		this.status = ProjectStatus.INACTIVE; // 최초 생성 시 PENDING 상태
	}

	//== 상태 변경 로직 ==//
	public void activate() {
		this.status = ProjectStatus.ACTIVE;
	}

	public void inactivate() {
		this.status = ProjectStatus.INACTIVE;
	}

	public void deleting() {
		this.status = ProjectStatus.DELETING;
	}

	//== 정보 수정 ==//
	public void updateDetails(String projectName, String description) {
		if (projectName != null && !projectName.isBlank()) {
			this.projectName = projectName;
		}
		if (description != null) {
			this.description = description;
		}
	}

	//== 프로젝트 멤버 메소드 ==//
	public void addProjectMember(ProjectMembers member) {
		this.members.add(member);
		member.setProject(this);
	}
}
