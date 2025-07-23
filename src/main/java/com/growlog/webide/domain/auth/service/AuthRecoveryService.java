package com.growlog.webide.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.auth.dto.ResetPasswordRequestDto;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.util.TemporaryPasswordGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRecoveryService {

	private final UserRepository userRepository;

	private final MailSender mailSender; // 임시 비밀번호 발송을 위한 메일 서비스
	private final PasswordEncoder passwordEncoder; // 비밀번호 암호화를 위한 인코더

	public void resetPassword(ResetPasswordRequestDto requestDto) {
		// 1. 사용자 조회
		Users user = userRepository.findByEmail(requestDto.getEmail())
			.filter(
				u -> u.getName().equals(requestDto.getName()))
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		// 2. 임시 비밀번호 생성
		String tempPassword = TemporaryPasswordGenerator.generateTemporaryPassword();

		// 3. 암호화 후 저장
		user.setPassword(passwordEncoder.encode(tempPassword));
		userRepository.save(user);

		// 4. 이메일 발송
		mailSender.sendTemporaryPassword(user.getEmail(), tempPassword);
		log.info("{}의 임시 비밀번호: {}", user.getEmail(), tempPassword);

	}
}
