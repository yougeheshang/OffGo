package org.tinkerhub.offgo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class RoutePath {
    private List<Point> points;
    private double totalDistance;
    private double estimatedTime;
    private List<RouteSegment> segments;
    private Point electricStartPoint;  // 电动车上车点
    private Point electricEndPoint;    // 电动车下车点

    public RoutePath(List<Point> points, double totalDistance, double estimatedTime, List<RouteSegment> segments) {
        this.points = points;
        this.totalDistance = totalDistance;
        this.estimatedTime = estimatedTime;
        this.segments = segments;
    }
} 