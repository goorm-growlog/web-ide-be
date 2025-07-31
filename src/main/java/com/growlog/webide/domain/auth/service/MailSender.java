package com.growlog.webide.domain.auth.service;

public interface MailSender {
	void send(String to, String code);

	void sendTemporaryPassword(String to, String tempPassword); // 임시 비밀번호 발급 메소드
}
