package com.growlog.webide.domain.users.entity;

import com.growlog.webide.domain.projects.entity.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "project_members")
public class ProjectMembers {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long projectMemberId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id")
	private Project projectId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private Users userId;

	@Column(nullable = false)
	private MemberRole role;

	@Builder
	public ProjectMembers(Project projectId, Users userId, MemberRole role) {
		this.projectId = projectId;
		this.userId = userId;
		this.role = role;
	}
}
