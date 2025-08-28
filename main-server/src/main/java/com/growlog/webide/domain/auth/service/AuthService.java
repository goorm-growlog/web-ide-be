package com.growlog.webide.domain.auth.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.growlog.webide.domain.auth.dto.KakaoDto;
import com.growlog.webide.domain.auth.dto.LoginRequestDto;
import com.growlog.webide.domain.auth.dto.RotatedTokens;
import com.growlog.webide.domain.auth.entity.RefreshToken;
import com.growlog.webide.domain.auth.repository.RefreshTokenRepository;
import com.growlog.webide.domain.auth.util.KakaoOAuth;
import com.growlog.webide.domain.users.dto.UserRegistrationRequestDto;
import com.growlog.webide.domain.users.entity.Provider;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.common.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final UserRepository userRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenRepository refreshTokenRepository;
	private final KakaoOAuth kakaoOAuth;
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
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

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
		if (!jwtTokenProvider.validateRefreshToken(presentedRt)) {
			throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		Long userId = jwtTokenProvider.getUserIdFromRefreshToken(presentedRt);

		RefreshToken savedRt = refreshTokenRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

		if (!savedRt.getRefreshToken().equals(presentedRt)) {
			// 재사용 탐지
			refreshTokenRepository.deleteById(userId);
			throw new CustomException(ErrorCode.REFRESH_TOKEN_REUSED);
		}

		String newAccessToken = jwtTokenProvider.createToken(userId);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
		refreshTokenRepository.save(new RefreshToken(userId, newRefreshToken));

		return new RotatedTokens(userId, null, newAccessToken, newRefreshToken);
	}

	public void logout(Long userId) {
		refreshTokenRepository.deleteById(userId);
	}

	public RotatedTokens kakaoLogin(String code) {
		// 1. 토큰 발급 요청
		KakaoDto.OAuthTokenDto oAuthToken = kakaoOAuth.requestAccessToken(code);

		// 2. 토큰으로 사용자 정보 요청
		KakaoDto.KakaoProfile profile = kakaoOAuth.requestProfile(oAuthToken);

		// 3. 기존 회원이면 로그인, 기존 회원이 아니면 회원가입
		String email = profile.getKakaoAccount().getEmail();
		Users user = userRepository.findByEmail(email)
			.orElseGet(() -> createNewUser(profile));

		String accessToken = jwtTokenProvider.createToken(user.getUserId());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

		refreshTokenRepository.save(new RefreshToken(user.getUserId(), refreshToken));

		return new RotatedTokens(user.getUserId(), user.getName(), accessToken, refreshToken);
	}

	// 카카오 계정으로 회원가입
	public Users createNewUser(KakaoDto.KakaoProfile profile) {
		UserRegistrationRequestDto request = new UserRegistrationRequestDto();
		request.setEmail(profile.getKakaoAccount().getEmail());
		request.setUsername(profile.getKakaoAccount().getProfile().getNickname());
		request.setPassword("kakao" + profile.getId() + "_" + UUID.randomUUID().toString());

		Users user = Users.builder()
			.name(request.getUsername())
			.email(request.getEmail())
			.password(passwordEncoder.encode(request.getPassword())) // 비밀번호 암호화
			.createdAt(LocalDateTime.now())
			.profileImageUrl(profile.getProperties().getThumbnailImage())
			.provider(Provider.KAKAO)
			.build();

		return userRepository.save(user);
	}
}
