package com.growlog.webide.global.common.jwt;

import java.util.Base64;
import java.util.Date;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.growlog.webide.global.security.CustomUserDetailService;
import com.growlog.webide.global.security.UserPrincipal;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private String secretKey;
	private long expiration;

	private final CustomUserDetailService customUserDetailsService;

	@PostConstruct
	public void init() {
		Dotenv dotenv = Dotenv.load();

		this.secretKey = Base64.getEncoder().encodeToString(dotenv.get("JWT_SECRET").getBytes());
		this.expiration = Long.parseLong(dotenv.get("JWT_EXPIRATION")); // JWT 만료 시간 (밀리초 단위)
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

	public String getUserId(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(secretKey)
			.build()
			.parseClaimsJws(token)
			.getBody()
			.getSubject();
	}

	public Authentication getAuthentication(String token) {
		UserPrincipal memberDetails = getUserPrincipal(token);
		return new UsernamePasswordAuthenticationToken(memberDetails, "", memberDetails.getAuthorities());
	}

	private UserPrincipal getUserPrincipal(String token) {
		UserPrincipal memberDetails = customUserDetailsService.loadUserByUsername(this.getUserId(token));
		return memberDetails;
	}
}
