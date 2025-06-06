package org.tinkerhub.offgo.repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tinkerhub.offgo.entity.ContentEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends JpaRepository<ContentEntity, Integer> {
    ContentEntity findById(int id);
    @Query("SELECT MAX(c.id) FROM ContentEntity c")
    int findMaxId();
}
