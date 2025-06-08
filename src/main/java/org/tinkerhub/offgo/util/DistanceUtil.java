package org.tinkerhub.offgo.util;

import org.tinkerhub.offgo.model.Point;

public class DistanceUtil {
    private static final int EARTH_RADIUS = 6371000; // 地球半径（米）
    
    /**
     * 使用 Haversine 公式计算两点之间的球面距离
     * @param p1 第一个点
     * @param p2 第二个点
     * @return 两点之间的距离，单位：米
     */
    public static double calculateDistance(Point p1, Point p2) {
        double lat1 = Math.toRadians(p1.getLatitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double deltaLat = Math.toRadians(p2.getLatitude() - p1.getLatitude());
        double deltaLon = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return EARTH_RADIUS * c;
    }
} 