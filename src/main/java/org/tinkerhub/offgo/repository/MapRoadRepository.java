package org.tinkerhub.offgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tinkerhub.offgo.entity.MapRoad;

import java.util.List;

@Repository
public interface MapRoadRepository extends JpaRepository<MapRoad, Long> {
    List<MapRoad> findByStartPointIdOrEndPointId(Long startPointId, Long endPointId);
} 