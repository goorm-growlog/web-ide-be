package com.growlog.webide.domain.chats.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.chats.entity.Chats;

public interface ChatRepository extends JpaRepository<Chats, Long> {
}
