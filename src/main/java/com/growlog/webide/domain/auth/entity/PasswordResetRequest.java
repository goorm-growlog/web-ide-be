package com.growlog.webide.domain.auth.entity;

import java.time.LocalDateTime;

import com.growlog.webide.domain.users.entity.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "password_reset_request")
public class PasswordResetRequest {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long requestId;

	@Column(nullable = false, length = 100)
	private String email;

	@Column(name = "temp_password", nullable = false)
	private String tempPassword;

	@Builder.Default
	private LocalDateTime issuedAt = LocalDateTime.now();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private Users user;
}
