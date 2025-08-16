package com.growlog.webide.domain.chats.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.growlog.webide.domain.chats.entity.Chats;

public interface ChatRepository extends JpaRepository<Chats, Long> {
	@Query("SELECT c FROM Chats c JOIN FETCH c.user WHERE c.project.id = :projectId ORDER BY c.sentAt DESC")
	Page<Chats> findByProjectIdWithUser(@Param("projectId") Long projectId, Pageable pageable);

	@Query("SELECT c FROM Chats c JOIN FETCH c.user "
		+ "WHERE c.project.id = :projectId AND c.content LIKE %:keyword% "
		+ "ORDER BY c.sentAt DESC")
	Page<Chats> findByProjectIdAndKeywordWithUser(
		@Param("projectId") Long projectId,
		@Param("keyword") String keyword,
		Pageable pageable
	);
}
