package org.tinkerhub.offgo.model;

import java.util.HashSet;
import java.util.Set;

public class Intersection {
    private final Long id;
    private final double x; // 纬度
    private final double y; // 经度
    private final Set<Long> connectedRoads;

    public Intersection(Long id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.connectedRoads = new HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Set<Long> getConnectedRoads() {
        return new HashSet<>(connectedRoads);
    }

    public void addConnectedRoad(Long roadId) {
        connectedRoads.add(roadId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Intersection that = (Intersection) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
} 