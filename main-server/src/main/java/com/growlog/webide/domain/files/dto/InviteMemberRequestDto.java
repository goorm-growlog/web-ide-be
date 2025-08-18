package com.growlog.webide.domain.files.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteMemberRequestDto {
	private List<@Email String> emails;
}
