package org.tinkerhub.offgo.Repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tinkerhub.offgo.entity.ContentEntity;

@Configuration
public interface ContentRepository extends JpaRepository<ContentEntity, Long> {
    ContentEntity findById(int id);
    @Query("SELECT MAX(u.ID) FROM User u")
    int findMaxID();


}
