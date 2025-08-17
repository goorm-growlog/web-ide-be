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
		message.setSubject("[GrowLog - WEB IDE] Email Verification Code");

		String content = """
			Welcome to GrowLog WEB IDE!

			---------------------------------------------------------
			Verification code: %s

			Please enter this code on the verification page.
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
		message.setSubject("[GrowLog - WEB IDE] Password Reset Notification");

		String content = """
			This mail contains your temporary password for GrowLog WEB IDE!

			---------------------------------------------------------
			Your temporary password: %s

			For your security, please change your password immediately after logging in.
			---------------------------------------------------------
			""".formatted(tempPassword);

		message.setText(content);

		mailSender.send(message);
	}

}
