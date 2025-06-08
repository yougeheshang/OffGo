package org.tinkerhub.offgo.model;

import java.util.HashSet;
import java.util.Set;

public class Road {
    private final Long id;
    private final double length;
    private final double crowdLevel;
    private final Set<Long> intersections;

    public Road(Long id, double length, double crowdLevel) {
        this.id = id;
        this.length = length;
        this.crowdLevel = crowdLevel;
        this.intersections = new HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public double getLength() {
        return length;
    }

    public double getCrowdLevel() {
        return crowdLevel;
    }

    public Set<Long> getIntersections() {
        return new HashSet<>(intersections);
    }

    public void addIntersection(Long intersectionId) {
        intersections.add(intersectionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Road road = (Road) o;
        return id.equals(road.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
} 