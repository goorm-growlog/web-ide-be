package com.growlog.webide.domain.files.dto;

import com.growlog.webide.domain.projects.entity.MemberRole;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequestDto {
	private MemberRole role;

	@AssertTrue(message = "Permission cannot be changed to OWNER.")
	public boolean isChangingOwnerRole() {
		return role != MemberRole.OWNER;
	}
}
