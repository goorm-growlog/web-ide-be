package com.growlog.webide.domain.permissions.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponseDto {
	private String role;  // read, write, owner
}
