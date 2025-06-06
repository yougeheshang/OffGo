package org.tinkerhub.offgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.tinkerhub.offgo.entity.Diary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Integer>, JpaSpecificationExecutor<Diary> {
    List<Diary> findByUserID(int userID);
    @Query("SELECT MAX(u.id) FROM Diary u")
    int findMaxID();
}
