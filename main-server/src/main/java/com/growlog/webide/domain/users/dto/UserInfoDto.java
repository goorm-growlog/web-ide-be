package com.growlog.webide.domain.users.dto;

import com.growlog.webide.domain.users.entity.Users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {
	@Schema(description = "사용자 ID", example = "1")
	Long userId;

	@Schema(description = "사용자 이름", example = "홍길동")
	private String name;

	@Schema(description = "이메일 주소", example = "hong@gmail.com")
	private String email;

	@Schema(description = "프로필 이미지 URL", example = "https://example.com/images/profile.jpg")
	private String profileImage;

	public static UserInfoDto from(Users user) {
		return UserInfoDto.builder()
			.userId(user.getUserId())
			.name(user.getName())
			.email(user.getEmail())
			.profileImage(user.getProfileImageUrl())
			.build();
	}

}
