package com.growlog.webide.domain.projects.entity;

public enum ProjectStatus {
	ACTIVE, // 활성 상태 (접속자가 1명 이상 있음)
	INACTIVE, // 비활성 상태 (접속자가 없음)
	DELETING // 삭제중 상태 (접근 불가)
}
