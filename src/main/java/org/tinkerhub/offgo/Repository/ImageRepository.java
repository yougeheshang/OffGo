package org.tinkerhub.offgo.Repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tinkerhub.offgo.entity.ImageEntity;
@Configuration
public interface ImageRepository extends JpaRepository<ImageEntity, Integer> {
    ImageEntity findById(int id);
    @Query("SELECT MAX(u.id) FROM ImageEntity u")
    int findMaxID();
}
