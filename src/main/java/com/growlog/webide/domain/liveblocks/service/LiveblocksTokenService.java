package com.growlog.webide.domain.liveblocks.service;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class LiveblocksTokenService { //liveblock에서 요구하는 jwt token create.

	@Value("${liveblocks.secret-key}")
	private String secretKey;

	public String generateToken(String roomId, Long userId, String username) {
		Instant now = Instant.now();

		return Jwts.builder()
			.setIssuer("webide")
			.setSubject(userId.toString())
			.setAudience("https://liveblocks.io")
			.claim("room", roomId)
			.claim("userInfo", Map.of("name", username))
			.setIssuedAt(Date.from(now))
			.setExpiration(Date.from(now.plus(Duration.ofMinutes(60))))
			.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
			.compact();
	}
}
