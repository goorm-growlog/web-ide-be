package com.growlog.webide.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class GithubDto {

	@NoArgsConstructor
	@Getter
	@Setter
	public static class UserDto {
		private String id;
		private String name;
		private String email;
		private String avatarUrl;
		private String provider;
	}
}
