package com.growlog.webide.domain.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.auth.entity.EmailVerification;

import jakarta.transaction.Transactional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {
	boolean existsByEmailAndVerifiedTrue(String email);

	@Transactional
	Optional<EmailVerification> findByEmail(String email);
}
