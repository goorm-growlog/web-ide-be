package com.growlog.webide.global.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.growlog.webide.config.EmailProperties;
import com.growlog.webide.domain.auth.service.MailSender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmtpMailSender implements MailSender {

	private final JavaMailSender mailSender;
	private final EmailProperties emailProperties;

	@Override
	public void send(String to, String code) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(to);
		message.setFrom(emailProperties.getFrom());
		message.setSubject("[GrowLog - WEB IDE] 이메일 인증번호 안내");

		String content = """
			GrowLog WEB IDE 에 오신걸 환영합니다!

			---------------------------------------------------------
			인증번호: %s

			이 번호를 인증 페이지에 입력해주세요.
			---------------------------------------------------------
			""".formatted(code);

		message.setText(content);

		mailSender.send(message);
	}

	// 임시 비밀번호 발급 메소드
	public void sendTemporaryPassword(String to, String tempPassword) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(to);
		message.setFrom(emailProperties.getFrom());
		message.setSubject("[GrowLog - WEB IDE] 임시 비밀번호 발급");

		String content = """
			GrowLog WEB IDE 임시 비밀번호 발급 메일입니다!

			---------------------------------------------------------
			임시 비밀번호: %s

			로그인 후 반드시 비밀번호를 변경해주세요.
			---------------------------------------------------------
			""".formatted(tempPassword);

		message.setText(content);

		mailSender.send(message);
	}

}
