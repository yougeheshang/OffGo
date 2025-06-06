package org.tinkerhub.offgo.repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tinkerhub.offgo.entity.ImageEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Integer> {
    ImageEntity findById(int id);
    @Query("SELECT MAX(i.id) FROM ImageEntity i")
    int findMaxId();
}
