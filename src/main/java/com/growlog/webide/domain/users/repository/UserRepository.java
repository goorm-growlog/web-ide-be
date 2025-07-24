package com.growlog.webide.domain.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.users.entity.Users;

public interface UserRepository extends JpaRepository<Users, Long> {
}
