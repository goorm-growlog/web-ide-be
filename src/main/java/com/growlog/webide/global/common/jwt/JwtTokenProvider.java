package com.growlog.webide.global.common.jwt;

import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.growlog.webide.global.security.CustomUserDetailService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtTokenProvider {

	private String secretKey;
	private long expiration;

	private final CustomUserDetailService customUserDetailsService;

	public JwtTokenProvider(
		@Value("${jwt.secret}") final String secretKey,
		@Value("${jwt.expiration}") final long expiration,
		final CustomUserDetailService customUserDetailsService) {
		this.secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
		this.expiration = expiration;
		this.customUserDetailsService = customUserDetailsService;
	}

	public String createToken(Long userId) {
		Claims claims = Jwts.claims().setSubject(String.valueOf(userId));

		Date date = new Date();
		Date expirationDate = new Date(date.getTime() + expiration);

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(date)
			.setExpiration(expirationDate)
			.signWith(SignatureAlgorithm.HS256, secretKey)
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.setSigningKey(secretKey)
				.parseClaimsJws(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public Long getUserId(String token) {
		return Long.parseLong(
			Jwts.parser()
				.setSigningKey(secretKey)
				.parseClaimsJws(token)
				.getBody()
				.getSubject()
		);
	}

	public Authentication getAuthentication(String token) {
		UserDetails userPrincipal = getUserPrincipal(token);
		return new UsernamePasswordAuthenticationToken(userPrincipal, "", userPrincipal.getAuthorities());
	}

	private UserDetails getUserPrincipal(String token) {
		return customUserDetailsService.loadUserById(this.getUserId(token));
	}
}
