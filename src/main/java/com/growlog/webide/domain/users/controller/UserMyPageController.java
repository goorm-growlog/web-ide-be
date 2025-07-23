package com.growlog.webide.domain.users.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.growlog.webide.domain.users.dto.DeleteRequestDto;
import com.growlog.webide.domain.users.dto.NameUpdateRequestDto;
import com.growlog.webide.domain.users.dto.PasswordUpdateRequestDto;
import com.growlog.webide.domain.users.dto.PasswordVerifyRequestDto;
import com.growlog.webide.domain.users.dto.ProfileImageResponseDto;
import com.growlog.webide.domain.users.dto.UserInfoDto;
import com.growlog.webide.domain.users.service.UserService;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users")
@Tag(name = "user API - 회원 마이페이지", description = "회원의 마이페이지 관련 기능을 제공.")
public class UserMyPageController {

	@Autowired
	private UserService userService;

	@Operation(summary = "회원 이름 변경")
	@PatchMapping("/me/name")
	public ApiResponse<String> updateName(@RequestBody NameUpdateRequestDto requestDto,
		@AuthenticationPrincipal UserPrincipal user) {
		userService.updateName(requestDto.getName(), user.getUserId());
		return ApiResponse.ok("이름이 변경되었습니다.");
	}

	@Operation(summary = "비밀번호 변경")
	@PatchMapping("/me/password")
	public ApiResponse<String> updatePassword(@RequestBody PasswordUpdateRequestDto dto,
		@AuthenticationPrincipal UserPrincipal user) {
		userService.updatePassword(dto.getCurrentPassword(), dto.getNewPassword(), user.getUserId());
		return ApiResponse.ok("비밀번호가 변경되었습니다.");
	}

	@Operation(summary = "비밀번호 검증")
	@PostMapping("/verify-password")
	public ApiResponse<Map<String, Boolean>> verifyPassword(@RequestBody PasswordVerifyRequestDto requestDto,
		@AuthenticationPrincipal UserPrincipal user) {
		boolean verified = userService.verifyPassword(requestDto.getPassword(), user.getUserId());
		return ApiResponse.ok(Map.of("verified", verified));
	}

	@Operation(summary = "회원 탈퇴")
	@DeleteMapping("/me")
	public ApiResponse<String> deleteAccount(@RequestBody DeleteRequestDto requestDto,
		@AuthenticationPrincipal UserPrincipal user) {
		userService.deleteAccount(requestDto.getPassword(), user.getUserId());
		return ApiResponse.ok("회원 탈퇴가 완료되었습니다.");
	}

	//마이페이지

	@Operation(summary = "내 정보 조회")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserInfoDto>> getMyInfo(@AuthenticationPrincipal UserPrincipal user) {
		UserInfoDto userInfo = userService.getMyInfo(user.getUserId());
		return ResponseEntity.ok(ApiResponse.ok(userInfo));
	}

	@Operation(
		summary = "프로필 이미지 변경",
		description = "multipart/form-data 형식으로 이미지를 업로드합니다."
	)
	@PatchMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ProfileImageResponseDto>> updateProfileImage(
		@AuthenticationPrincipal UserPrincipal user,
		@Parameter(
			description = "업로드할 프로필 이미지",
			content = @Content(
				mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
				schema = @Schema(type = "string", format = "binary")
			)
		)
		@RequestPart("profileImage") MultipartFile file) {

		String imageUrl = userService.updateProfileImage(user.getUserId(), file);

		return ResponseEntity.ok(ApiResponse.ok(new ProfileImageResponseDto(imageUrl)));
	}
}
