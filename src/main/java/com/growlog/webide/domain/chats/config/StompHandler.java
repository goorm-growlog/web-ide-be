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

	private static boolean checkDuplicatedSubscribtion(StompHeaderAccessor accessor) {
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
		final StompHeaderAccessor accessor = MessageHeaderAccessor
			.getAccessor(message, StompHeaderAccessor.class);

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			handleValidToken(accessor);
			log.info("Connected successfully");
		}

		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			try {
				validateSubscription(accessor);

				if (checkDuplicatedSubscribtion(accessor)) {
					log.warn("Already Subscribed.");
					return null;
				}
			} catch (CustomException e) {
				log.warn("Subscription denied: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
				return null;
			}
		}
		log.info("Subscribed successfully");
		return message;
	}

	private void validateSubscription(StompHeaderAccessor accessor) {
		try {
			final Long userId = Long.parseLong(accessor.getSessionAttributes().get("userId").toString());
			final String destination = accessor.getDestination();
			final Long projectId = Long.parseLong(destination.split("/")[3]);

			if (projectMemberRepository.findByProject_IdAndUser_UserId(projectId, userId).isEmpty()) {
				log.warn("Required permissions are missing.");
				throw new CustomException(ErrorCode.NOT_A_MEMBER);
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
