package com.growlog.webide.domain.auth.service;

import org.springframework.stereotype.Service;

import com.growlog.webide.domain.auth.entity.EmailVerification;
import com.growlog.webide.domain.auth.repository.EmailVerificationRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.util.VerificationCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	private final EmailVerificationRepository emailVerificationRepository;
	private final MailSender mailSender;

	public void sendVerificationEmail(String email) {
		String code = VerificationCodeGenerator.generateCode(); //6자리 랜덤 숫자 생성

		EmailVerification verification = EmailVerification.builder()
			.email(email)
			.code(code)
			.verified(false)
			.build();

		verification.updateCode(code); //코드 업데이트
		emailVerificationRepository.save(verification);

		mailSender.send(email, code); //이메일 전송

		log.info("Verification email sent to {} with code: {}", email, code);
	}

	//이메일 인증숫자 매치 확인
	public boolean verifyEmail(String email, String code) {

		//DB에서 이메일과 코드 조회
		EmailVerification verification = emailVerificationRepository.findByEmail(email)
			.orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND));

		//인증되지 않은 이메일인지 확인, 불일치 시 false
		if (!verification.getCode().equals(code)) {
			return false;
		}

		//일치 시 verified = true 상태로 업데이트 후 저장
		verification.markAsVerified();
		emailVerificationRepository.save(verification);
		return true;

	}
}
