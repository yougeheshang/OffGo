package org.tinkerhub.offgo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteSegment {
    private List<Point> points;
    private String transportMode;
    private double distance;
    private double time;
} 