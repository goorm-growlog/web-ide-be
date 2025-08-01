package com.growlog.webide.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {
	@Value("${MAIL_USERNAME}")
	private String username;

	@Value("${MAIL_PASSWORD}")
	private String rawPassword;

	@Bean
	public JavaMailSender javaMailSender() {
		// 따옴표가 포함된 비밀번호 제거
		String password = rawPassword.replaceAll("^\"|\"$", ""); // 양 끝의 따옴표 제거

		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("smtp.gmail.com");
		mailSender.setPort(587);
		mailSender.setUsername(username);
		mailSender.setPassword(password);

		Properties props = mailSender.getJavaMailProperties();
		props.put("mail.smtp.auth", true);
		props.put("mail.smtp.starttls.enable", true);

		return mailSender;
	}
}
