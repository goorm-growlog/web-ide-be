package com.growlog.webide.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Entity
@Builder
@Table(name = "email_verification")
public class EmailVerification {
	@Id
	@Column(length = 100)
	private String email;

	@Column(length = 6, nullable = false)
	private String code;

	// 0: 미인증(false), 1: 인증됨(true)
	@Column(nullable = false)
	private boolean verified;

	public void updateCode(String code) {
		this.code = code;
		this.verified = false; // 인증번호가 갱신되면 인증 상태 초기화
	}

	public void markAsVerified() {
		this.verified = true;
	}
}
