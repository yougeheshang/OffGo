package org.tinkerhub.offgo.model;

import java.util.ArrayList;
import java.util.List;

public class MapNode {
    private double latitude;
    private double longitude;
    private List<MapNode> neighbors;
    private double g; // 从起点到当前节点的实际代价
    private double h; // 从当前节点到终点的估计代价
    private double f; // f = g + h
    private MapNode parent; // 用于重建路径

    public MapNode(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.neighbors = new ArrayList<>();
        this.g = Double.MAX_VALUE;
        this.h = 0;
        this.f = Double.MAX_VALUE;
        this.parent = null;
    }

    public void addNeighbor(MapNode neighbor) {
        if (!neighbors.contains(neighbor)) {
            neighbors.add(neighbor);
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public List<MapNode> getNeighbors() {
        return neighbors;
    }

    public double getG() {
        return g;
    }

    public void setG(double g) {
        this.g = g;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    public double getF() {
        return f;
    }

    public void setF(double f) {
        this.f = f;
    }

    public MapNode getParent() {
        return parent;
    }

    public void setParent(MapNode parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapNode mapNode = (MapNode) o;
        return Double.compare(mapNode.latitude, latitude) == 0 &&
                Double.compare(mapNode.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(latitude) * 31 + Double.hashCode(longitude);
    }
} 