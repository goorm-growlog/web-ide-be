package com.growlog.webide.domain.projects.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/* ActiveInstance
 *  어떤 사용자가 어떤 프로젝트에 어떤 컨테이너로 접속했는지
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "active_instances")
public class ActiveInstance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "instance_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private Users user;

	@Column(nullable = false, unique = true)
	private String containerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InstanceStatus status;

	@Column(nullable = false)
	private Integer webSocketPort;

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime connectedAt;

	@Builder
	public ActiveInstance(Project project, Users user, String containerId, InstanceStatus status,
		Integer webSocketPort) {
		this.project = project;
		this.user = user;
		this.containerId = containerId;
		this.status = status;
		this.webSocketPort = webSocketPort;
	}

	//== 상태 변경 ==/
	public void activate() {

		this.status = InstanceStatus.ACTIVE;
	}

	public void disconnect() {
		this.status = InstanceStatus.PENDING;
	}
}
