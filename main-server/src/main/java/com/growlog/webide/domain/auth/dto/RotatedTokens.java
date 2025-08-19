package com.growlog.webide.domain.auth.dto;

// 내부 전달용
public record RotatedTokens(Long userId, String name, String accessToken, String refreshToken) {
}
