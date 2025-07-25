package com.growlog.webide.domain.liveblocks.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.growlog.webide.domain.liveblocks.RoomIdGenerator;
import com.growlog.webide.domain.liveblocks.dto.LiveblocksTokenResponseDto;
import com.growlog.webide.domain.liveblocks.service.LiveblocksTokenService;
import com.growlog.webide.global.common.ApiResponse;
import com.growlog.webide.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/liveblocks")
public class LiveblocksController {

	private final LiveblocksTokenService liveblocksTokenService;
	private final RoomIdGenerator roomIdGenerator;

	@PostMapping("/auth")
	public ResponseEntity<ApiResponse<LiveblocksTokenResponseDto>> authenticate(
		@RequestParam String projectId,
		@RequestParam String filePath,
		@AuthenticationPrincipal UserPrincipal user
	) {
		String roomId = roomIdGenerator.generateRoomId(projectId, filePath);
		String token = liveblocksTokenService.generateToken(roomId, user.getUserId(),user.getName());

		LiveblocksTokenResponseDto tokenResponseDto = new LiveblocksTokenResponseDto(token);
		return ResponseEntity.ok(ApiResponse.ok(tokenResponseDto));
	}
}
