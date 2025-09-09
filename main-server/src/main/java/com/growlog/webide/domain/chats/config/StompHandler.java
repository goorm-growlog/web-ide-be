package com.growlog.webide.domain.chats.config;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.util.Strings;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.global.common.exception.CustomException;
import com.growlog.webide.global.common.exception.ErrorCode;
import com.growlog.webide.global.common.jwt.JwtTokenProvider;
import com.growlog.webide.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class StompHandler implements ChannelInterceptor {

	public static final String AUTHORIZATION = "Authorization";
	public static final String BEARER = "Bearer ";
	private final JwtTokenProvider jwtTokenProvider;

	private final ProjectMemberRepository projectMemberRepository;

	private static boolean checkDuplicatedSubscription(StompHeaderAccessor accessor) {
		final Set<String> subscriptions = (Set<String>)accessor.getSessionAttributes()
			.getOrDefault("SUBSCRIPTIONS", new HashSet<>());
		final String destination = accessor.getDestination();
		if (subscriptions.contains(destination)) {
			return true;
		} else {
			subscriptions.add(destination);
			accessor.getSessionAttributes().put("SUBSCRIPTIONS", subscriptions);
		}
		return false;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			handleValidToken(accessor);
			log.info("Connected successfully");
		}

		// [수정] 모든 구독 요청에 대해 유효성 검사를 먼저 수행하도록 구조 변경
		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			try {
				validateSubscription(accessor); // 모든 구독 경로를 이 메서드에서 처리
				if (checkDuplicatedSubscription(accessor)) {
					log.warn("Already Subscribed to {}.", accessor.getDestination());
					return null; // 중복 구독 방지
				}
				log.info("Subscribed successfully to {}.", accessor.getDestination());
			} catch (CustomException e) {
				log.warn("Subscription denied: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
				return null; // 유효하지 않은 구독은 거부
			}
		}
		return message;
	}

	private void validateSubscription(StompHeaderAccessor accessor) {
		try {
			final Long userId = (Long)accessor.getSessionAttributes().get("userId");
			final String destination = accessor.getDestination();

			if (destination != null && destination.startsWith("/topic/projects/")) {
				// 채팅 관련 구독일 경우, projectId를 검증합니다.
				final Long projectId = Long.parseLong(destination.split("/")[3]);
				if (projectMemberRepository.findByProject_IdAndUser_UserId(projectId, userId).isEmpty()) {
					log.warn("Required permissions are missing for chat subscription.");
					throw new CustomException(ErrorCode.NOT_A_MEMBER);
				}
			} else if (destination != null && destination.startsWith("/user/queue/")) {
				// 개인 채널 구독은 별도의 projectId 검증 없이 통과시킵니다.
				log.info("User {} subscribed to their private log queue.", userId);
			} else {
				// 알 수 없는 구독 경로일 경우, 거부합니다.
				log.warn("Attempted to subscribe to an unknown or disallowed destination: {}", destination);
				throw new CustomException(ErrorCode.BAD_REQUEST);
			}
		} catch (Exception e) {
			log.warn("Failed to process subscription request.: {}", e.getMessage());
			throw new CustomException(ErrorCode.BAD_REQUEST);
		}
	}

	private void handleValidToken(StompHeaderAccessor accessor) {
		final String token = resolveToken(accessor);
		if (jwtTokenProvider.validateToken(token)) {
			final Authentication auth = jwtTokenProvider.getAuthentication(token);
			UserPrincipal principal = (UserPrincipal)auth.getPrincipal();
			Long userId = principal.getUserId();
			accessor.getSessionAttributes().put("userId", userId);
			accessor.setUser(auth);
		} else {
			CustomException authException = new CustomException(ErrorCode.INVALID_ACCESSTOKEN);
			log.error("WebSocket connection failed:", authException);
			throw authException;
		}
	}

	private String resolveToken(StompHeaderAccessor accessor) {
		final String bearerToken = accessor.getFirstNativeHeader(AUTHORIZATION);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER)) {
			return bearerToken.substring(7);
		}
		return Strings.EMPTY;
	}
}
