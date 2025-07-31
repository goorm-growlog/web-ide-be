package com.growlog.webide.domain.files.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.files.entity.FileMeta;

public interface FileMetaRepository extends JpaRepository<FileMeta, Long> {
	Optional<FileMeta> findByProjectIdAndPath(Long projectId, String path);

	List<FileMeta> findByProjectIdAndNameContainingIgnoreCaseAndDeletedFalse(Long projectId, String name);
}
