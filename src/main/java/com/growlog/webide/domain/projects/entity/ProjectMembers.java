package com.growlog.webide.domain.projects.entity;

import com.growlog.webide.domain.users.entity.MemberRole;
import com.growlog.webide.domain.users.entity.Users;
import jakarta.persistence.*;
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
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private Users user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberRole role;

	@Builder
	public ProjectMembers(Project project, Users user, MemberRole role) {
		this.project = project;
		this.user = user;
		this.role = role;
	}
}
