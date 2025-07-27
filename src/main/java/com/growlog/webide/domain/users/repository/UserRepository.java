package com.growlog.webide.domain.users.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.users.entity.Users;

public interface UserRepository extends JpaRepository<Users, Long> {
	Optional<Users> findById(Long id);

	Optional<Users> findByName(String name);
}
