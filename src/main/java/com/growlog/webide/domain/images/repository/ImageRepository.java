package com.growlog.webide.domain.images.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.growlog.webide.domain.images.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
