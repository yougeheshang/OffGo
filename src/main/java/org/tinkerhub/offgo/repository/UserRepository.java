package org.tinkerhub.offgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tinkerhub.offgo.entity.User;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    Optional<User> findById(int id);
    @Query("SELECT MAX(u.id) FROM User u")
    int findMaxId();
    boolean existsByUsername(String username);
}
