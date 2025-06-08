package org.tinkerhub.offgo.model;

import java.util.List;
import org.tinkerhub.offgo.entity.MapRoad;

public class RouteResponse {
    private List<Point> route;
    private double totalDistance; // 单位：米
    private double estimatedTime; // 单位：分钟
    private double startToRoadDistance; // 从起点到最近道路的距离（米）
    private double endToRoadDistance; // 从终点到最近道路的距离（米）
    private List<MapRoad> roads; // 路径经过的道路列表
    private Point electricStartPoint;  // 电动车上车点
    private Point electricEndPoint;    // 电动车下车点

    public static class Point {
        private double latitude;
        private double longitude;

        public Point(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }

    public List<Point> getRoute() {
        return route;
    }

    public void setRoute(List<Point> route) {
        this.route = route;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public double getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(double estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public double getStartToRoadDistance() {
        return startToRoadDistance;
    }

    public void setStartToRoadDistance(double startToRoadDistance) {
        this.startToRoadDistance = startToRoadDistance;
    }

    public double getEndToRoadDistance() {
        return endToRoadDistance;
    }

    public void setEndToRoadDistance(double endToRoadDistance) {
        this.endToRoadDistance = endToRoadDistance;
    }

    public List<MapRoad> getRoads() {
        return roads;
    }

    public void setRoads(List<MapRoad> roads) {
        this.roads = roads;
    }

    public Point getElectricStartPoint() {
        return electricStartPoint;
    }

    public void setElectricStartPoint(Point electricStartPoint) {
        this.electricStartPoint = electricStartPoint;
    }

    public Point getElectricEndPoint() {
        return electricEndPoint;
    }

    public void setElectricEndPoint(Point electricEndPoint) {
        this.electricEndPoint = electricEndPoint;
    }
} 