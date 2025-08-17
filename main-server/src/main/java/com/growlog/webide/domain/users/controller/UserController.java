package com.growlog.webide.domain.users.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.users.dto.UserRegistrationRequestDto;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.service.UserService;
import com.growlog.webide.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@Tag(name = "user API - 회원", description = "회원가입을 지원하는 API")
public class UserController {

	@Autowired
	private UserService userService;

	@Operation(
		summary = "회원가입 API",
		description = "사용자로부터 이름, 이메일, 비밀번호 등을 입력받아 회원으로 등록."
	)
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<Users>> registerMember(
		@Valid
		@RequestBody
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "회원가입 요청 정보",
			required = true
		)

		UserRegistrationRequestDto requestDto) {
		Users user = userService.registerMember(requestDto);
		return ResponseEntity.ok(ApiResponse.ok(user));
	}

}
