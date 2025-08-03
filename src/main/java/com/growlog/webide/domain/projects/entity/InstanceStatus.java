package com.growlog.webide.domain.projects.entity;

public enum InstanceStatus {
	ACTIVE, // 활성 상태 (접속 중)
	DISCONNECTING // 프로젝트 종료 후, 컨테이너 삭제 예약 상태
}
