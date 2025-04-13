package org.tinkerhub.offgo.Repository;
import java.sql.Connection;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.tinkerhub.offgo.entity.diary;
@Configuration
public interface DiaryRepository extends JpaRepository<diary, Integer> {
    diary findById(int id);
    @Query("SELECT MAX(u.ID) FROM User u")
    int findMaxID();
}
