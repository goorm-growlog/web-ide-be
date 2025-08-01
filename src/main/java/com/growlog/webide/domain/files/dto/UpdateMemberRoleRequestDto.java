package com.growlog.webide.domain.files.dto;

import com.growlog.webide.domain.projects.entity.MemberRole;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequestDto {
	private MemberRole role;
}
