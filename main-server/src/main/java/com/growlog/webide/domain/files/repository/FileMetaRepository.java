package com.growlog.webide.domain.files.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.files.entity.FileMeta;

public interface FileMetaRepository extends JpaRepository<FileMeta, Long> {
	Optional<FileMeta> findByProjectIdAndPath(Long projectId, String path);

	List<FileMeta> findByProjectIdAndNameContainingIgnoreCaseAndDeletedFalse(Long projectId, String name);

	List<FileMeta> findAllByProjectIdAndDeletedFalse(Long projectId);

	// 특정 경로로 시작하는 모든 파일/폴더 메타데이터를 조회하는 메서드
	List<FileMeta> findByProjectIdAndPathStartingWith(Long projectId, String path);

	// 삭제되지 않은 데이터 중에서 경로로 조회 (생성 시 중복 체크용)
	Optional<FileMeta> findByProjectIdAndPathAndDeletedFalse(Long projectId, String path);

	//여러 경로를 한 번에 조회하기 위한 메서드
	List<FileMeta> findByProjectIdAndPathInAndDeletedFalse(Long projectId, List<String> paths);
}
