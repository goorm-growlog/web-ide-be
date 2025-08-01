package com.growlog.webide.global.common.jwt;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.growlog.webide.domain.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
		WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

		// queryString 파싱
		URI uri = request.getURI(); // ex: /ws?token=xxx&projectId=1
		String query = uri.getQuery(); // token=xxx&projectId=1

		if (query == null) {
			return false;
		}

		Map<String, String> queryMap = Arrays.stream(query.split("&"))
			.map(s -> s.split("="))
			.filter(arr -> arr.length == 2)
			.collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));

		String token = queryMap.get("token");
		String projectIdStr = queryMap.get("projectId");

		if (token == null || projectIdStr == null) {
			return false;
		}

		Long userId = jwtTokenProvider.getUserId(token);
		Long projectId = Long.parseLong(projectIdStr);

		// 사용자, 프로젝트 ID 저장
		attributes.put("userId", userId);
		attributes.put("projectId", projectId);

		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
		WebSocketHandler wsHandler, Exception exception) {
		// no-op
	}
}
