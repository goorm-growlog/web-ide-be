package com.growlog.webide.domain.chats.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.growlog.webide.domain.projects.entity.Project;
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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chats {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long chatId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private Users user;

	@Column(nullable = false)
	private String content;

	@CreatedDate
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	@Column(name = "sent_at", updatable = false)
	private Instant sentAt;

	public Chats(Project project, Users user, String content) {
		this.project = project;
		this.user = user;
		this.content = content;
	}

	public Long getChatId() {
		return chatId;
	}

	public Project getProject() {
		return project;
	}

	public Users getUser() {
		return user;
	}

	public String getContent() {
		return content;
	}

	public Instant getSentAt() {
		return sentAt;
	}

}
