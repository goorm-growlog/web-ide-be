package com.growlog.webide.domain.projects.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.growlog.webide.domain.users.entity.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

/* ActiveSession
 *  어떤 사용자가 어떤 프로젝트에 실시간으로 접속해 있는지 기록
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "active_sessions")
public class ActiveSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "session_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private Users user;

	@Column(name = "server_id", nullable = false)
	private String serverId;

	@Column(nullable = false)
	private String containerId;

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime connectedAt;

	@Builder
	public ActiveSession(Project project,
		Users user,
		String serverId,
		String containerId) {
		this.project = project;
		this.user = user;
		this.serverId = serverId;
		this.containerId = containerId;
	}

	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}
}
