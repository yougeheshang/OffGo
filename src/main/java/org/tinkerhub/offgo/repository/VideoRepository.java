package org.tinkerhub.offgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tinkerhub.offgo.entity.Video;

public interface VideoRepository extends JpaRepository<Video, Long> {
} 