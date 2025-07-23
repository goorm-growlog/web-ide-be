package com.growlog.webide.global.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.growlog.webide.domain.users.entity.Users;

import lombok.Getter;

@Getter
public class UserPrincipal implements UserDetails {
	private final Long userId;
	private final String email;
	private final String password;

	public UserPrincipal(Users user) {
		this.userId = user.getUserId();
		this.email = user.getEmail();
		this.password = user.getPassword();
	}

	public Long getUserId() {
		return userId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return null; // 권한이 필요하다면 여기에 추가
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
