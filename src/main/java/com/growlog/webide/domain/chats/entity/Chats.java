package com.growlog.webide.domain.chats.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.users.entity.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class Chats {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long chatId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project projectId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private Users userId;

	@Column(nullable = false)
	private String content;

	@CreatedDate
	@Column(name = "sent_at", updatable = false)
	private LocalDateTime sentAt;


}
