package com.growlog.webide.domain.images.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.images.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {
	Optional<Image> findById(Long id);
	Optional<Image> findByImageNameIgnoreCase(String imageName);
}
