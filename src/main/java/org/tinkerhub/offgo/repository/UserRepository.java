package org.tinkerhub.offgo.Repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tinkerhub.offgo.entity.User;
@Configuration
public interface UserRepository extends JpaRepository<User, Long> {

    User findByID(Integer ID);
    @Query("SELECT u FROM User u WHERE u.username = ?1")
    User findByName(String name);
    @Query("SELECT MAX(u.ID) FROM User u")
    int findMaxID();
    @Query("DELETE FROM User u")
    void deleteall();


}
