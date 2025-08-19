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
}
