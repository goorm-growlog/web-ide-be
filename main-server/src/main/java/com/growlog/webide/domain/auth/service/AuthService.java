package com.growlog.webide.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.auth.dto.LoginRequestDto;
import com.growlog.webide.domain.auth.dto.LoginResponseDto;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.common.jwt.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final PasswordEncoder passwordEncoder;
	//private final UserLoginHistoryRepository userLoginHistoryRepository;

	//로그인
	public LoginResponseDto login(LoginRequestDto requestDto, HttpServletRequest request) {
		Users user = userRepository.findByEmail(requestDto.getEmail())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		// 사용자 삭제 여부 확인(deletedAt이 null이 아닌 경우)
		if (user.getDeletedAt() != null) {
			throw new CustomException(ErrorCode.DELETED_USER); // 아래 ErrorCode 추가 필요
		}

		if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
			throw new CustomException(ErrorCode.LOGIN_FAILED);
		}

		/*// 로그인 성공 시 사용자 로그인 기록 저장
		String ip = request.getRemoteAddr(); // 클라이언트 IP 주소
		String userAgent = request.getHeader("User-Agent"); // 클라이언트 User-Agent 정보

		UserLoginHistory loginHistory = UserLoginHistory.builder()
			.user(user)
			.loginTime(LocalDateTime.now())
			.ipAddress(ip)
			.userAgent(userAgent)
			.build();

		userLoginHistoryRepository.save(loginHistory);*/

		String accessToken = jwtTokenProvider.createToken(user.getUserId());

		return new LoginResponseDto(user.getUserId(), user.getName(), accessToken);
	}

	// 로그인한 사용자 정보 조회용
	public Users getUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}
}
