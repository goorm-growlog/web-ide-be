package com.growlog.webide.domain.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationRequestDto {

	@Schema(description = "이메일 주소", example = "hong@gmail.com")
	@NotBlank(message = "Email is required.")
	@Email(message = "Invalid email format.")
	@Size(max = 100, message = "Email must be at most 100 characters long.")
	private String email;

	@Schema(description = "비밀번호", example = "securePassword123")
	@NotBlank(message = "Password is required.")
	private String password;

	@Schema(description = "사용자 이름", example = "홍길동")
	@NotBlank(message = "Username is required.")
	private String username;
}
