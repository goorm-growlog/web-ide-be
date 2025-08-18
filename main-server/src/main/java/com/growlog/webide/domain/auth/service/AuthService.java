package com.growlog.webide.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.auth.dto.LoginRequestDto;
import com.growlog.webide.domain.auth.dto.RotatedTokens;
import com.growlog.webide.domain.auth.entity.RefreshToken;
import com.growlog.webide.domain.auth.repository.RefreshTokenRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.common.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenRepository refreshTokenRepository;
	//private final UserLoginHistoryRepository userLoginHistoryRepository;

	// 로그인 + AccessToken + RefreshToken 발급
	public RotatedTokens login(LoginRequestDto request) {
		Users user = userRepository.findByEmail(request.getEmail())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (user.getDeletedAt() != null) {
			throw new CustomException(ErrorCode.DELETED_USER);
		}
		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new CustomException(ErrorCode.LOGIN_FAILED);
		}

		String accessToken = jwtTokenProvider.createToken(user.getUserId());
		String refreshToken = jwtTokenProvider.createToken(user.getUserId());

		refreshTokenRepository.save(new RefreshToken(user.getUserId(), refreshToken));
		return new RotatedTokens(user.getUserId(), user.getName(), accessToken, refreshToken);
	}

	// 로그인한 사용자 정보 조회용
	public Users getUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	// RTR + 재사용 탐지
	public RotatedTokens refresh(String presentedRt) {
		if (!jwtTokenProvider.validateToken(presentedRt)) {
			throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		Long userId = jwtTokenProvider.getUserId(presentedRt);

		RefreshToken savedRt = refreshTokenRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

		if (!savedRt.getRefreshToken().equals(presentedRt)) {
			// 재사용 탐지
			refreshTokenRepository.deleteById(userId);
			throw new CustomException(ErrorCode.REFRESH_TOKEN_REUSED);
		}

		String newAccessToken = jwtTokenProvider.createToken(userId);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
		refreshTokenRepository.save(new RefreshToken(userId, newAccessToken));

		return new RotatedTokens(userId, null, newAccessToken, newRefreshToken);
	}

	public void logout(Long userId) {
		refreshTokenRepository.deleteById(userId);
	}
}
