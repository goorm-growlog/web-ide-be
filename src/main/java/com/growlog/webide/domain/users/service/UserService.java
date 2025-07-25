package com.growlog.webide.domain.users.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.growlog.webide.domain.auth.repository.EmailVerificationRepository;
import com.growlog.webide.domain.users.dto.UserInfoDto;
import com.growlog.webide.domain.users.dto.UserRegistrationRequestDto;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.image.ImageUploadService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
	@Autowired
	private final UserRepository userRepository;

	private final ImageUploadService imageUploadService;

	@Autowired
	private EmailVerificationRepository emailVerificationRepository;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public Users registerMember(UserRegistrationRequestDto requestDto) {

		//email 중복 체크
		if (userRepository.existsByEmail(requestDto.getEmail())) {
			throw new CustomException(ErrorCode.EMAIL_CONFLICT);
		}

		//인증 확인
		if (!emailVerificationRepository.existsByEmailAndVerifiedTrue(requestDto.getEmail())) {
			throw new CustomException(ErrorCode.EMAIL_NOT_FOUND);
		}

		Users user = Users.builder()
			.name(requestDto.getUsername())
			.email(requestDto.getEmail())
			.password(passwordEncoder.encode(requestDto.getPassword())) // 비밀번호 암호화
			.createdAt(LocalDateTime.now())
			.build();
		return userRepository.save(user);
	}

	public void updateName(String newName, Long userId) {
		Users user = findUserById(userId);
		user.setName(newName);
		userRepository.save(user);
	}

	public void updatePassword(String currentPassword, String newPassword, Long userId) {
		Users user = findUserById(userId);
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}

	public boolean verifyPassword(String password, Long userId) {
		Users user = findUserById(userId);
		return passwordEncoder.matches(password, user.getPassword());
	}

	public void deleteAccount(String password, Long userId) {
		Users user = findUserById(userId);
		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}
		user.setDeletedAt(LocalDateTime.now());
		userRepository.save(user);
	}

	private Users findUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	public UserInfoDto getMyInfo(Long userId) {
		Users user = findUserById(userId);
		return UserInfoDto.from(user);
	}

	@Transactional
	public String updateProfileImage(Long userId, MultipartFile file) {
		Users user = findUserById(userId);
		String imageUrl = imageUploadService.uploadProfileImage(file);
		user.setProfileImageUrl(imageUrl);
		return imageUrl;
	}

}
