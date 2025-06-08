package org.tinkerhub.offgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tinkerhub.offgo.entity.MapLocation;

import java.util.List;

@Repository
public interface MapLocationRepository extends JpaRepository<MapLocation, Long> {
    List<MapLocation> findByNameIsNotNull();
    List<MapLocation> findByType(String type);
    List<MapLocation> findByNameIsNull();
    List<MapLocation> findByName(String name);
    List<MapLocation> findByLatitudeAndLongitude(Double latitude, Double longitude);
    List<MapLocation> findByIsStartPointTrue();
} 