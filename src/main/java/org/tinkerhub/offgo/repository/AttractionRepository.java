package org.tinkerhub.offgo.repository;

import org.tinkerhub.offgo.entity.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttractionRepository extends JpaRepository<Attraction, Long> {
}
