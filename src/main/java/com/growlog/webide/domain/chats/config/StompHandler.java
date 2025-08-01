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

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		final StompHeaderAccessor accessor = MessageHeaderAccessor
			.getAccessor(message, StompHeaderAccessor.class);

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			handleValidToken(accessor);
		}

		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			if (isDuplicateSubscription(accessor)) {
				log.warn("중복 구독 시도, 요청을 차단합니다.");
				return null;
			}
		}
		return message;
	}

	private void handleValidToken(StompHeaderAccessor accessor) {
		final String token = resolveToken(accessor);
		if (jwtTokenProvider.validateToken(token)) {
			final Authentication auth = jwtTokenProvider.getAuthentication(token);
			UserPrincipal principal = (UserPrincipal)auth.getPrincipal();
			Long userId = principal.getUserId();
			accessor.getSessionAttributes().put("AUTHENTICATED", userId);
			accessor.setUser(auth);
		}
	}

	private String resolveToken(StompHeaderAccessor accessor) {
		final String bearerToken = accessor.getFirstNativeHeader(AUTHORIZATION);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER)) {
			return bearerToken.substring(7);
		}
		return Strings.EMPTY;
	}

	private boolean isDuplicateSubscription(StompHeaderAccessor accessor) {
		final Set<String> subscriptions = (Set<String>)accessor.getSessionAttributes()
			.getOrDefault("SUBSCRIPTIONS", new HashSet<>());
		final String destination = accessor.getDestination();
		if (subscriptions.contains(destination)) {
			return true;
		} else {
			subscriptions.add(destination);
			accessor.getSessionAttributes().put("SUBSCRIPTIONS", subscriptions);
			return false;
		}
	}
}
