package com.growlog.webide.domain.users.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.growlog.webide.domain.users.entity.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
	Optional<Users> findById(Long id);

	Optional<Users> findByName(String name);

	Optional<Users> findByEmail(String email);

	boolean existsByEmail(String email);
}
