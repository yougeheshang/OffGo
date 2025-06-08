package org.tinkerhub.offgo.model;

import java.util.List;

public class RouteRequest {
    private Point startPoint;
    private List<Point> pathPoints;
    private boolean allowReturn; // 是否允许路径折返
    private String transportMode; // 交通方式：walking 或 bicycle

    public static class Point {
        private double latitude;
        private double longitude;

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

    public Point getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public List<Point> getPathPoints() {
        return pathPoints;
    }

    public void setPathPoints(List<Point> pathPoints) {
        this.pathPoints = pathPoints;
    }

    public boolean isAllowReturn() {
        return allowReturn;
    }

    public void setAllowReturn(boolean allowReturn) {
        this.allowReturn = allowReturn;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }
} 