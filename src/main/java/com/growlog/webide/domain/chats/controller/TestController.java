package com.growlog.webide.domain.chats.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TestController {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;

	@GetMapping("/get-token")
	public String getToken() {
		Users testUser = userRepository.findByName("MyTestUser")
			.orElseThrow(() -> new RuntimeException("테스트 유저(MyTestUser)를 찾을 수 없습니다. DataInitializer를 확인하세요."));
		return jwtTokenProvider.createToken(testUser.getUserId());
	}

}
