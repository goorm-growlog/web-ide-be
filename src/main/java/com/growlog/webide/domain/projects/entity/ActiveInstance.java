package com.growlog.webide.domain.projects.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.growlog.webide.domain.users.entity.User;

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

/* ActiveInstance
 *  어떤 사용자가 어떤 프로젝트에 어떤 컨테이너로 접속했는지
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "active_instance")
public class ActiveInstance {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, unique = true)
	private String containerId;

	@Column(nullable = false)
	private Integer webSocketPort;

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime connectedAt;

	@Builder
	public ActiveInstance(Project project, User user, String containerId, Integer webSocketPort) {
		this.project = project;
		this.user = user;
		this.containerId = containerId;
		this.webSocketPort = webSocketPort;
	}
}
