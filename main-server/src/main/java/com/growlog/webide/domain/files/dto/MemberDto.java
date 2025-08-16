package com.growlog.webide.domain.files.dto;

import com.growlog.webide.domain.projects.entity.MemberRole;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MemberDto {
	private Long userId;
	private String name;
	private String email;
	private String profileImageUrl;
	private MemberRole role;

	public MemberDto(Long userId, String name, String email, String profileImageUrl, MemberRole role) {
		this.userId = userId;
		this.name = name;
		this.email = email;
		this.profileImageUrl = profileImageUrl;
		this.role = role;
	}
}
