package org.tinkerhub.offgo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.tinkerhub.offgo.model.MapNode;
import org.tinkerhub.offgo.entity.MapRoad;
import org.tinkerhub.offgo.model.RouteRequest;
import org.tinkerhub.offgo.model.RouteResponse;
import org.tinkerhub.offgo.repository.MapRoadRepository;
import org.tinkerhub.offgo.model.RoutePath;
import org.tinkerhub.offgo.model.RouteSegment;
import org.tinkerhub.offgo.util.DistanceUtil;
import org.tinkerhub.offgo.model.Point;
import org.tinkerhub.offgo.model.Intersection;
import org.tinkerhub.offgo.model.Road;
import org.tinkerhub.offgo.service.LandmarkManager;
import org.springframework.cache.annotation.Cacheable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

@Service
public class RouteService {
    
    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);
    
    private static final double WALKING_SPEED = 1.4; // 步行速度：1.4米/秒
    private static final double BICYCLE_SPEED = 4.2; // 自行车速度：4.2米/秒
    private static final double ELECTRIC_SPEED = 8.0; // 电动车速度：8米/秒
    
    // 电动车模式配置
    private static final double ELECTRIC_PRIMARY_ROAD_THRESHOLD = 300.0; // 电动车寻找主干道的最大距离（米）
    
    // 添加并行计算阈值
    private static final int PARALLEL_THRESHOLD = 5;
    
    @Autowired
    private MapRoadRepository mapRoadRepository;
    
    private LandmarkManager landmarkManager;
    private final Map<String, Double> sharedHeuristics = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 初始化LandmarkManager
        List<MapRoad> allRoads = mapRoadRepository.findAll();
        Map<Long, Intersection> intersections = new HashMap<>();
        Map<Long, Road> roads = new HashMap<>();
        
        // 从道路数据中提取交叉口和道路信息
        for (MapRoad road : allRoads) {
            String[] pathPoints = road.getPathPoints().split(";");
            if (pathPoints.length >= 2) {
                // 创建道路对象
                double length = calculateRoadLength(pathPoints);
                Road roadObj = new Road(road.getId(), length, road.getCrowdLevel());
                roads.put(road.getId(), roadObj);
                
                // 处理每个交叉口
                for (int i = 0; i < pathPoints.length; i++) {
                    String[] coords = pathPoints[i].split(",");
                    double lat = Double.parseDouble(coords[0]);
                    double lon = Double.parseDouble(coords[1]);
                    
                    // 创建或获取交叉口
                    String key = String.format("%.6f,%.6f", lat, lon);
                    Intersection intersection = intersections.computeIfAbsent(
                        (long)key.hashCode(),
                        k -> new Intersection(k, lat, lon)
                    );
                    
                    // 添加道路连接
                    intersection.addConnectedRoad(road.getId());
                    roadObj.addIntersection(intersection.getId());
                }
            }
        }
        
        // 初始化LandmarkManager
        landmarkManager = new LandmarkManager(intersections, roads);
        logger.info("Initialized LandmarkManager with {} intersections and {} roads", 
            intersections.size(), roads.size());
    }
    
    // 计算道路长度
    private double calculateRoadLength(String[] pathPoints) {
        double length = 0;
        for (int i = 0; i < pathPoints.length - 1; i++) {
            String[] coords1 = pathPoints[i].split(",");
            String[] coords2 = pathPoints[i + 1].split(",");
            
            double lat1 = Double.parseDouble(coords1[0]);
            double lon1 = Double.parseDouble(coords1[1]);
            double lat2 = Double.parseDouble(coords2[0]);
            double lon2 = Double.parseDouble(coords2[1]);
            
            length += calculateDistance(
                new MapNode(lat1, lon1),
                new MapNode(lat2, lon2)
            );
        }
        return length;
    }
    
    // 添加缓存键类
    private static class PathKey {
        private final MapNode start;
        private final MapNode end;
        private final String transportMode;

        public PathKey(MapNode start, MapNode end, String transportMode) {
            this.start = start;
            this.end = end;
            this.transportMode = transportMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PathKey pathKey = (PathKey) o;
            return Objects.equals(start, pathKey.start) &&
                   Objects.equals(end, pathKey.end) &&
                   Objects.equals(transportMode, pathKey.transportMode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end, transportMode);
        }
    }

    // 添加路径结果类
    private static class PathResult {
        private final List<MapNode> path;
        private final double time;
        private final List<MapRoad> roads;

        public PathResult(List<MapNode> path, double time, List<MapRoad> roads) {
            this.path = path;
            this.time = time;
            this.roads = roads;
        }

        public List<MapNode> getPath() {
            return path;
        }

        public double getTime() {
            return time;
        }

        public List<MapRoad> getRoads() {
            return roads;
        }
    }

    // 添加缓存相关字段
    private final Map<PathKey, PathResult> pathCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    // 添加缓存管理方法
    private PathResult getCachedPath(MapNode start, MapNode end, String transportMode) {
        PathKey key = new PathKey(start, end, transportMode);
        return pathCache.get(key);
    }

    private void cachePath(MapNode start, MapNode end, String transportMode, PathResult result) {
        if (pathCache.size() >= MAX_CACHE_SIZE) {
            // 简单的LRU实现：移除最早的条目
            pathCache.remove(pathCache.keySet().iterator().next());
        }
        PathKey key = new PathKey(start, end, transportMode);
        pathCache.put(key, result);
    }
    
    public RouteResponse planRoute(RouteRequest request) {
        try {
            // 创建地图节点
            List<MapNode> nodes = createMapNodes(request);
            logger.info("Created {} map nodes", nodes.size());
            
            if (nodes.isEmpty()) {
                logger.error("No map nodes created");
                throw new RuntimeException("No map nodes available for path planning");
            }
            
            // 获取起始点和所有路径点
            MapNode startNode = nodes.get(0);  // 第一个节点是起始点
            List<MapNode> pathNodes = nodes.subList(1, request.getPathPoints().size() + 1);
            
            // 使用动态规划算法寻找最优路径
            List<MapNode> path = findOptimalPath(startNode, pathNodes, request);
            logger.info("Found optimal path with {} nodes", path.size());
            
            // 转换为响应格式
            List<RouteResponse.Point> route = path.stream()
                    .map(node -> new RouteResponse.Point(node.getLatitude(), node.getLongitude()))
                    .collect(Collectors.toList());
            
            // 计算总距离
            double totalDistance = calculateTotalDistance(route);
            logger.info("Total distance: {} meters", String.format("%.2f", totalDistance));
            
            // 计算从起点到最近道路的距离
            double startToRoadDistance = calculateDistanceToRoad(startNode, findNearestRoad(startNode, mapRoadRepository.findAll()));
            logger.info("Start to road distance: {} meters", String.format("%.2f", startToRoadDistance));
            
            // 计算从最后一个路径点到最近道路的距离
            MapNode lastNode = pathNodes.get(pathNodes.size() - 1);
            double endToRoadDistance = calculateDistanceToRoad(lastNode, findNearestRoad(lastNode, mapRoadRepository.findAll()));
            logger.info("End to road distance: {} meters", String.format("%.2f", endToRoadDistance));
            
            // 获取路径经过的道路列表
            List<MapRoad> roads = getRoadsInPath(path);
            logger.info("Number of roads in path: {}", roads.size());
            
            // 根据交通方式计算时间
            double estimatedTime;
            RouteResponse.Point electricStartPoint = null;
            RouteResponse.Point electricEndPoint = null;
            
            if ("walking".equals(request.getTransportMode())) {
                // 步行速度：1.4米/秒
                estimatedTime = totalDistance / 1.4 / 60;
                logger.info("Walking mode - Total distance: {}m, Estimated time: {:.2f} minutes", totalDistance, String.format("%.2f", estimatedTime));
            } else if ("electric".equals(request.getTransportMode())) {
                // 电动车模式
                // 找到最近的 primary 类型道路入口点
                MapRoad nearestPrimaryRoad = roads.stream()
                    .filter(road -> "primary".equals(road.getRoadType()))
                    .min(Comparator.comparingDouble(road -> calculateDistanceToRoad(startNode, road)))
                    .orElse(null);
                
                if (nearestPrimaryRoad != null) {
                    MapNode startProj = findNearestPointOnRoad(startNode, nearestPrimaryRoad);
                    electricStartPoint = new RouteResponse.Point(startProj.getLatitude(), startProj.getLongitude());
                    MapNode endProj = findNearestPointOnRoad(lastNode, nearestPrimaryRoad);
                    electricEndPoint = new RouteResponse.Point(endProj.getLatitude(), endProj.getLongitude());
                    
                    // 步行段的时间（使用步行速度1.4米/秒）
                    double walkingTime = (startToRoadDistance + endToRoadDistance) / WALKING_SPEED / 60;
                    logger.info("Electric mode - Walking segments: startToRoad={}m, endToRoad={}m, walkingTime={} minutes", 
                        startToRoadDistance, endToRoadDistance, String.format("%.2f", walkingTime));
                    
                    // 道路段的时间（分段累加每段拥挤度）
                    double roadTime = 0.0;
                    for (int i = 0; i < path.size() - 1; i++) {
                        MapNode current = path.get(i);
                        MapNode next = path.get(i + 1);
                        double segmentDistance = calculateDistance(current, next);
                        // 找到当前段对应的道路，优先找主干道
                        MapRoad road = findNearestRoad(current, roads);
                        if (road != null && "primary".equals(road.getRoadType())) {
                            double crowdLevel = Math.max(0.05, Math.min(1.0, road.getCrowdLevel()));
                            double speed = ELECTRIC_SPEED * crowdLevel;
                            roadTime += segmentDistance / speed / 60;
                        } else {
                            // 非主干道或没有道路，使用步行速度
                            roadTime += segmentDistance / WALKING_SPEED / 60;
                        }
                    }
                    logger.info("Electric mode - Road time (分段): {} minutes", String.format("%.2f", roadTime));
                    // 总时间 = 步行段 + 道路段
                    estimatedTime = walkingTime + roadTime;
                    logger.info("Total estimated time: {} minutes (walking: {} + road: {})", 
                        String.format("%.2f", estimatedTime),
                        String.format("%.2f", walkingTime),
                        String.format("%.2f", roadTime));
                } else {
                    logger.warn("No primary roads found, using default walking speed");
                    estimatedTime = totalDistance / WALKING_SPEED / 60;
                }
            } else {
                // 自行车模式
                // 步行段的时间（使用步行速度1.4米/秒）
                double walkingTime = (startToRoadDistance + endToRoadDistance) / WALKING_SPEED / 60;
                
                // 道路段的时间（使用自行车速度4.2米/秒 * 道路拥挤度）
                double roadDistance = totalDistance - startToRoadDistance - endToRoadDistance;
                
                if (roads.isEmpty()) {
                    estimatedTime = totalDistance / WALKING_SPEED / 60;
                } else {
                    // 计算每段路径的实际时间
                    double totalTime = walkingTime;
                    for (int i = 0; i < path.size() - 1; i++) {
                        MapNode current = path.get(i);
                        MapNode next = path.get(i + 1);
                        double segmentDistance = calculateDistance(current, next);
                        
                        MapRoad road = findNearestRoad(current, roads);
                        if (road != null) {
                            double crowdLevel = Math.max(0.05, Math.min(1.0, road.getCrowdLevel()));
                            double speed = BICYCLE_SPEED * crowdLevel;
                            totalTime += segmentDistance / speed / 60;
                        } else {
                            totalTime += segmentDistance / WALKING_SPEED / 60;
                        }
                    }
                    estimatedTime = totalTime;
                }
            }
            
            RouteResponse response = new RouteResponse();
            response.setRoute(route);
            response.setTotalDistance(Double.parseDouble(String.format("%.2f", totalDistance))); // 保留2位小数
            response.setEstimatedTime(Double.parseDouble(String.format("%.2f", estimatedTime))); // 保留2位小数
            response.setStartToRoadDistance(Double.parseDouble(String.format("%.2f", startToRoadDistance))); // 保留2位小数
            response.setEndToRoadDistance(Double.parseDouble(String.format("%.2f", endToRoadDistance))); // 保留2位小数
            response.setRoads(roads);
            
            // 设置电动车上下车点
            if ("electric".equals(request.getTransportMode())) {
                response.setElectricStartPoint(electricStartPoint);
                response.setElectricEndPoint(electricEndPoint);
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error planning route", e);
            throw new RuntimeException("Failed to plan route: " + e.getMessage());
        }
    }
    
    private List<MapNode> createMapNodes(RouteRequest request) {
        List<MapNode> nodes = new ArrayList<>();
        Map<String, MapNode> nodeMap = new HashMap<>();
        
        // 获取所有道路
        List<MapRoad> roads = mapRoadRepository.findAll();
        logger.info("[Time] Found {} roads in database", roads.size());
        
        if (roads.isEmpty()) {
            logger.warn("[Time] No roads found in database");
            return nodes;
        }
        
        // 创建所有道路节点
        for (MapRoad road : roads) {
            try {
                // 解析道路的路径点
                String[] pathPoints = road.getPathPoints().split(";");
                if (pathPoints.length >= 2) {
                    // 只连接相邻的节点
                    for (int i = 0; i < pathPoints.length - 1; i++) {
                        String[] currentCoords = pathPoints[i].split(",");
                        String[] nextCoords = pathPoints[i + 1].split(",");
                        
                        double currentLat = Double.parseDouble(currentCoords[0]);
                        double currentLon = Double.parseDouble(currentCoords[1]);
                        double nextLat = Double.parseDouble(nextCoords[0]);
                        double nextLon = Double.parseDouble(nextCoords[1]);
                        
                        String currentKey = String.format("%.6f,%.6f", currentLat, currentLon);
                        String nextKey = String.format("%.6f,%.6f", nextLat, nextLon);
                        
                        MapNode currentNode = nodeMap.computeIfAbsent(currentKey,
                            k -> new MapNode(currentLat, currentLon));
                        MapNode nextNode = nodeMap.computeIfAbsent(nextKey,
                            k -> new MapNode(nextLat, nextLon));
                        
                        // 只添加相邻节点的连接
                        currentNode.addNeighbor(nextNode);
                        nextNode.addNeighbor(currentNode);
                    }
                }
            } catch (Exception e) {
                logger.error("[Time] Error processing road {}: {}", road.getId(), e.getMessage());
            }
        }
        
        // 添加起始点
        MapNode startNode = new MapNode(
            request.getStartPoint().getLatitude(),
            request.getStartPoint().getLongitude()
        );
        nodes.add(startNode);
        logger.info("[Time] Added start point: ({}, {})", startNode.getLatitude(), startNode.getLongitude());
        
        // 添加所有路径点
        List<MapNode> userPathNodes = new ArrayList<>();
        for (RouteRequest.Point pathPoint : request.getPathPoints()) {
            MapNode node = new MapNode(pathPoint.getLatitude(), pathPoint.getLongitude());
            nodes.add(node);
            userPathNodes.add(node);
            logger.info("[Time] Added path point: ({}, {})", node.getLatitude(), node.getLongitude());
        }
        
        // 将起始点和路径点连接到最近的道路段
        for (MapNode node : nodes) {
            // 找到最近的道路和最近的线段
            MapRoad nearestRoad = findNearestRoad(node, roads);
            if (nearestRoad != null) {
                String[] pathPoints = nearestRoad.getPathPoints().split(";");
                double minDist = Double.MAX_VALUE;
                MapNode proj = null;
                MapNode segStart = null, segEnd = null;
                for (int i = 0; i < pathPoints.length - 1; i++) {
                    String[] startCoords = pathPoints[i].split(",");
                    String[] endCoords = pathPoints[i + 1].split(",");
                    double startLat = Double.parseDouble(startCoords[0]);
                    double startLon = Double.parseDouble(startCoords[1]);
                    double endLat = Double.parseDouble(endCoords[0]);
                    double endLon = Double.parseDouble(endCoords[1]);
                    MapNode segStartNode = nodeMap.get(String.format("%.6f,%.6f", startLat, startLon));
                    MapNode segEndNode = nodeMap.get(String.format("%.6f,%.6f", endLat, endLon));
                    if (segStartNode == null || segEndNode == null) continue;
                    // 计算投影点
                    MapNode candidate = findNearestPointOnLineSegment(node, segStartNode, segEndNode);
                    double dist = calculateDistance(node, candidate);
                    if (dist < minDist) {
                        minDist = dist;
                        proj = candidate;
                        segStart = segStartNode;
                        segEnd = segEndNode;
                    }
                }
                if (proj != null && segStart != null && segEnd != null) {
                    // 将投影点加入nodeMap，避免重复
                    String projKey = String.format("%.6f,%.6f", proj.getLatitude(), proj.getLongitude());
                    MapNode projNode = nodeMap.getOrDefault(projKey, proj);
                    nodeMap.putIfAbsent(projKey, projNode);
                    // 兴趣点与投影点直接连线
                    node.addNeighbor(projNode);
                    projNode.addNeighbor(node);
                    // 投影点与线段两端连线
                    projNode.addNeighbor(segStart);
                    projNode.addNeighbor(segEnd);
                    segStart.addNeighbor(projNode);
                    segEnd.addNeighbor(projNode);
                    logger.info("[Time] Connected node ({}, {}) to road by projection ({}, {})", 
                        node.getLatitude(), node.getLongitude(), 
                        projNode.getLatitude(), projNode.getLongitude());
                }
            } else {
                logger.warn("[Time] No nearest road found for point ({}, {})", 
                    node.getLatitude(), node.getLongitude());
            }
        }
        
        // 将所有节点添加到结果列表
        nodes.addAll(nodeMap.values());
        
        logger.info("[Time] Created {} total nodes", nodes.size());
        logger.info("[Time] Request path points size: {}", request.getPathPoints().size());
        logger.info("[Time] Allow return: {}", request.isAllowReturn());
        return nodes;
    }
    
    private MapRoad findNearestRoad(MapNode node, List<MapRoad> roads) {
        MapRoad nearest = null;
        double minTotalDistance = Double.MAX_VALUE;
        
        for (MapRoad road : roads) {
            // 计算点到道路的距离
            double distanceToRoad = calculateDistanceToRoad(node, road);
            
            // 找到道路段上最近的点
            MapNode nearestPoint = findNearestPointOnRoad(node, road);
            if (nearestPoint == null) {
                continue;
            }
            
            // 计算从道路点到下一个目标点的估计距离
            double estimatedDistanceToTarget = calculateHeuristic(nearestPoint, node);
            
            // 计算总距离（当前点到道路点的距离 + 从道路点到目标的估计距离）
            double totalDistance = distanceToRoad + estimatedDistanceToTarget;
            
            if (totalDistance < minTotalDistance) {
                minTotalDistance = totalDistance;
                nearest = road;
            }
        }
        
        return nearest;
    }
    
    private double calculateDistanceToRoad(MapNode node, MapRoad road) {
        String[] pathPoints = road.getPathPoints().split(";");
        double minDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < pathPoints.length - 1; i++) {
            String[] startCoords = pathPoints[i].split(",");
            String[] endCoords = pathPoints[i + 1].split(",");
            
            double startLat = Double.parseDouble(startCoords[0]);
            double startLon = Double.parseDouble(startCoords[1]);
            double endLat = Double.parseDouble(endCoords[0]);
            double endLon = Double.parseDouble(endCoords[1]);
            
            MapNode startNode = new MapNode(startLat, startLon);
            MapNode endNode = new MapNode(endLat, endLon);
            
            // 计算点到线段的距离
            double distance = calculateDistanceToLineSegment(node, startNode, endNode);
            minDistance = Math.min(minDistance, distance);
            
            // 添加日志来调试距离计算
            logger.debug("Distance to road segment - Point: ({}, {}), Road segment: ({}, {}) to ({}, {}), Distance: {} meters",
                node.getLatitude(), node.getLongitude(),
                startLat, startLon, endLat, endLon,
                distance);
        }
        
        return minDistance;
    }
    
    private double calculateDistanceToLineSegment(MapNode point, MapNode lineStart, MapNode lineEnd) {
        double x = point.getLatitude();
        double y = point.getLongitude();
        double x1 = lineStart.getLatitude();
        double y1 = lineStart.getLongitude();
        double x2 = lineEnd.getLatitude();
        double y2 = lineEnd.getLongitude();
        
        double A = x - x1;
        double B = y - y1;
        double C = x2 - x1;
        double D = y2 - y1;
        
        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = -1;
        
        if (lenSq != 0) {
            param = dot / lenSq;
        }
        
        double xx, yy;
        
        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }
        
        double dx = x - xx;
        double dy = y - yy;
        
        // 使用Haversine公式计算实际距离
        final int R = 6371000; // 地球半径（米）
        double lat1 = Math.toRadians(x);
        double lat2 = Math.toRadians(xx);
        double deltaLat = Math.toRadians(xx - x);
        double deltaLon = Math.toRadians(yy - y);
        
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        double distance = R * c;
        
        // 添加日志来调试距离计算
        logger.debug("Distance calculation - Point: ({}, {}), Line: ({}, {}) to ({}, {}), Distance: {} meters",
            point.getLatitude(), point.getLongitude(),
            lineStart.getLatitude(), lineStart.getLongitude(),
            lineEnd.getLatitude(), lineEnd.getLongitude(),
            String.format("%.2f", distance));
            
        return distance;
    }
    
    private MapNode findNearestPointOnRoad(MapNode node, MapRoad road) {
        String[] pathPoints = road.getPathPoints().split(";");
        MapNode optimalPoint = null;
        double minDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < pathPoints.length - 1; i++) {
            String[] startCoords = pathPoints[i].split(",");
            String[] endCoords = pathPoints[i + 1].split(",");
            
            double startLat = Double.parseDouble(startCoords[0]);
            double startLon = Double.parseDouble(startCoords[1]);
            double endLat = Double.parseDouble(endCoords[0]);
            double endLon = Double.parseDouble(endCoords[1]);
            
            MapNode startNode = new MapNode(startLat, startLon);
            MapNode endNode = new MapNode(endLat, endLon);
            
            // 计算点到线段的最近点
            MapNode nearestOnSegment = findNearestPointOnLineSegment(node, startNode, endNode);
            
            // 只考虑实际距离，不使用启发式函数
            double distance = calculateDistance(node, nearestOnSegment);
            
            if (distance < minDistance) {
                minDistance = distance;
                optimalPoint = nearestOnSegment;
            }
        }
        
        return optimalPoint;
    }
    
    private MapNode findNearestPointOnLineSegment(MapNode point, MapNode lineStart, MapNode lineEnd) {
        double x = point.getLatitude();
        double y = point.getLongitude();
        double x1 = lineStart.getLatitude();
        double y1 = lineStart.getLongitude();
        double x2 = lineEnd.getLatitude();
        double y2 = lineEnd.getLongitude();
        
        double A = x - x1;
        double B = y - y1;
        double C = x2 - x1;
        double D = y2 - y1;
        
        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = -1;
        
        if (lenSq != 0) {
            param = dot / lenSq;
        }
        
        double xx, yy;
        
        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }
        
        return new MapNode(xx, yy);
    }
    
    private List<MapNode> findOptimalPath(MapNode startNode, List<MapNode> pathNodes, RouteRequest request) {
        if (pathNodes.isEmpty()) {
            return new ArrayList<>();
        }
        if (pathNodes.size() == 1 && !request.isAllowReturn()) {
            return findPath(startNode, pathNodes.get(0));
        }
        
        int n = pathNodes.size();
        List<Integer> idxList = new ArrayList<>();
        for (int i = 0; i < n; i++) idxList.add(i);
        List<MapNode> bestPath = null;
        double minDist = Double.MAX_VALUE;
        List<List<Integer>> allOrders = new ArrayList<>();
        permute(idxList, 0, allOrders);
        
        for (List<Integer> order : allOrders) {
            List<MapNode> seq = new ArrayList<>();
            seq.add(startNode);
            for (int idx : order) seq.add(pathNodes.get(idx));
            
            // 拼接完整路径
            List<MapNode> fullPath = new ArrayList<>();
            double totalDist = 0;
            boolean valid = true;
            
            for (int i = 0; i < seq.size() - 1; i++) {
                MapNode a = seq.get(i);
                MapNode b = seq.get(i + 1);
                
                // 使用A*算法找到两点间的最短路径
                List<MapNode> segment = findPath(a, b);
                if (segment.isEmpty()) {
                    valid = false;
                    break;
                }
                
                // 计算这段路径的实际距离
                double segmentDist = calculateTotalDistanceForNodes(segment);
                
                if (i > 0) segment.remove(0); // 去重重叠节点
                fullPath.addAll(segment);
                totalDist += segmentDist;
            }
            
            if (!valid) continue;
            
            // 更新最短距离路径
            if (totalDist < minDist) {
                minDist = totalDist;
                bestPath = fullPath;
            }
        }
        
        return bestPath != null ? bestPath : new ArrayList<>();
    }

    // 全排列工具
    private void permute(List<Integer> arr, int k, List<List<Integer>> result) {
        if (k == arr.size()) {
            result.add(new ArrayList<>(arr));
        } else {
            for (int i = k; i < arr.size(); i++) {
                Collections.swap(arr, i, k);
                permute(arr, k + 1, result);
                Collections.swap(arr, i, k);
            }
        }
    }
    
    private List<MapNode> findPath(MapNode start, MapNode end) {
        // 初始化开放列表和关闭列表
        PriorityQueue<MapNode> openList = new PriorityQueue<>(
            Comparator.comparingDouble(MapNode::getF)
        );
        Set<MapNode> closedList = new HashSet<>();
        Map<MapNode, Double> gScore = new HashMap<>();
        Map<MapNode, MapNode> cameFrom = new HashMap<>();
        
        // 初始化起始节点
        gScore.put(start, 0.0);
        start.setF(calculateHeuristic(start, end));
        openList.add(start);
        
        while (!openList.isEmpty()) {
            // 获取F值最小的节点
            MapNode current = openList.poll();
            
            // 如果到达终点，重建路径并返回
            if (current.equals(end)) {
                return reconstructPath(cameFrom, current);
            }
            
            closedList.add(current);
            
            // 检查所有邻居节点
            for (MapNode neighbor : current.getNeighbors()) {
                if (closedList.contains(neighbor)) {
                    continue;
                }
                
                // 计算从起点经过当前节点到邻居节点的代价
                double tentativeG = gScore.get(current) + calculateDistance(current, neighbor);
                
                // 如果找到更好的路径
                if (!gScore.containsKey(neighbor) || tentativeG < gScore.get(neighbor)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    neighbor.setF(tentativeG + calculateHeuristic(neighbor, end));
                    
                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }
        
        // 如果没有找到路径，尝试使用直接路径
        logger.warn("No path found between ({}, {}) and ({}, {}), using direct path",
            start.getLatitude(), start.getLongitude(),
            end.getLatitude(), end.getLongitude());
            
        // 创建直接路径
        List<MapNode> directPath = new ArrayList<>();
        directPath.add(start);
        directPath.add(end);
        return directPath;
    }
    
    private List<MapNode> reconstructPath(Map<MapNode, MapNode> cameFrom, MapNode current) {
        List<MapNode> path = new ArrayList<>();
        path.add(current);
        
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current);
        }
        
        return path;
    }
    
    private double calculateHeuristic(MapNode from, MapNode to) {
        // 使用Landmark启发式
        return landmarkManager.getHeuristic(
            (long)String.format("%.6f,%.6f", from.getLatitude(), from.getLongitude()).hashCode(),
            (long)String.format("%.6f,%.6f", to.getLatitude(), to.getLongitude()).hashCode()
        );
    }
    
    private double calculateDistance(MapNode from, MapNode to) {
        // 使用Haversine公式计算两点之间的距离
        final int R = 6371000; // 地球半径（米）
        
        double lat1 = Math.toRadians(from.getLatitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double deltaLat = Math.toRadians(to.getLatitude() - from.getLatitude());
        double deltaLon = Math.toRadians(to.getLongitude() - from.getLongitude());
        
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        double distance = R * c;
        
        // 添加日志来调试距离计算
        logger.debug("Distance calculation - From: ({}, {}), To: ({}, {}), Distance: {} meters",
            from.getLatitude(), from.getLongitude(),
            to.getLatitude(), to.getLongitude(),
            distance);
            
        return distance;
    }
    
    private double calculateTotalDistance(List<RouteResponse.Point> points) {
        double totalDistance = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            totalDistance += calculateDistance(
                new MapNode(points.get(i).getLatitude(), points.get(i).getLongitude()),
                new MapNode(points.get(i + 1).getLatitude(), points.get(i + 1).getLongitude())
            );
        }
        return totalDistance;
    }
    
    private double calculateTotalDistanceForNodes(List<MapNode> nodes) {
        double totalDistance = 0;
        for (int i = 0; i < nodes.size() - 1; i++) {
            totalDistance += calculateDistance(nodes.get(i), nodes.get(i + 1));
        }
        return totalDistance;
    }
    
    // 添加获取路径经过的道路的方法
    private List<MapRoad> getRoadsInPath(List<MapNode> path) {
        List<MapRoad> roads = new ArrayList<>();
        List<MapRoad> allRoads = mapRoadRepository.findAll();
        
        logger.info("Finding roads in path with {} nodes", path.size());
        logger.info("Total roads in database: {}", allRoads.size());
        
        // 进一步放宽坐标比较的精度到 1e-3
        double tolerance = 1e-3;
        
        for (int i = 0; i < path.size() - 1; i++) {
            MapNode current = path.get(i);
            MapNode next = path.get(i + 1);
            
            logger.debug("Checking path segment {}: ({}, {}) to ({}, {})", 
                i, current.getLatitude(), current.getLongitude(), 
                next.getLatitude(), next.getLongitude());
            
            // 查找连接这两个节点的道路
            for (MapRoad road : allRoads) {
                String[] pathPoints = road.getPathPoints().split(";");
                for (int j = 0; j < pathPoints.length - 1; j++) {
                    String[] currentCoords = pathPoints[j].split(",");
                    String[] nextCoords = pathPoints[j + 1].split(",");
                    
                    double currentLat = Double.parseDouble(currentCoords[0]);
                    double currentLon = Double.parseDouble(currentCoords[1]);
                    double nextLat = Double.parseDouble(nextCoords[0]);
                    double nextLon = Double.parseDouble(nextCoords[1]);
                    
                    // 检查当前路径段是否与道路段匹配
                    boolean isMatch = (Math.abs(current.getLatitude() - currentLat) < tolerance &&
                                     Math.abs(current.getLongitude() - currentLon) < tolerance &&
                                     Math.abs(next.getLatitude() - nextLat) < tolerance &&
                                     Math.abs(next.getLongitude() - nextLon) < tolerance) ||
                                    (Math.abs(current.getLatitude() - nextLat) < tolerance &&
                                     Math.abs(current.getLongitude() - nextLon) < tolerance &&
                                     Math.abs(next.getLatitude() - currentLat) < tolerance &&
                                     Math.abs(next.getLongitude() - currentLon) < tolerance);
                    
                    if (isMatch) {
                        logger.debug("Found matching road segment in road {}: ({}, {}) to ({}, {})", 
                            road.getId(), currentLat, currentLon, nextLat, nextLon);
                        if (!roads.contains(road)) {
                            roads.add(road);
                            logger.info("Added road {} to path", road.getId());
                        }
                        break;
                    }
                }
            }
        }
        
        logger.info("Found {} unique roads in path", roads.size());
        if (roads.isEmpty()) {
            logger.warn("No roads found in path. This might indicate a problem with road matching.");
            // 打印一些路径点的信息以帮助调试
            for (int i = 0; i < Math.min(path.size(), 5); i++) {
                logger.warn("Path point {}: ({}, {})", 
                    i, path.get(i).getLatitude(), path.get(i).getLongitude());
            }
            // 打印一些道路点的信息以帮助调试
            if (!allRoads.isEmpty()) {
                MapRoad sampleRoad = allRoads.get(0);
                String[] samplePoints = sampleRoad.getPathPoints().split(";");
                logger.warn("Sample road points from road {}:", sampleRoad.getId());
                for (int i = 0; i < Math.min(samplePoints.length, 3); i++) {
                    String[] coords = samplePoints[i].split(",");
                    logger.warn("Road point {}: ({}, {})", 
                        i, coords[0], coords[1]);
                }
            }
        }
        return roads;
    }

    // 新增：用时最短路径规划
    public RouteResponse planRouteByTime(RouteRequest request) {
        try {
            // 如果是步行模式，直接使用距离最短路径
            if ("walking".equals(request.getTransportMode())) {
                logger.info("[Time] Walking mode detected, using distance-based path planning");
                return planRoute(request);
            }

            // 使用距离最短路径的结果
            RouteResponse distanceResponse = planRoute(request);
            logger.info("[Time] Using distance-based path planning result");

            // 重新计算时间
            double estimatedTime;
            if ("electric".equals(request.getTransportMode())) {
                // 电动车模式
                // 找到最近的 primary 类型道路入口点
                List<MapRoad> roads = distanceResponse.getRoads();
                MapRoad nearestPrimaryRoad = roads.stream()
                    .filter(road -> "primary".equals(road.getRoadType()))
                    .min(Comparator.comparingDouble(road -> calculateDistanceToRoad(
                        new MapNode(distanceResponse.getRoute().get(0).getLatitude(), 
                                  distanceResponse.getRoute().get(0).getLongitude()), 
                        road)))
                    .orElse(null);
                
                if (nearestPrimaryRoad != null) {
                    // 找到主干道上最近的点
                    MapNode startNode = new MapNode(
                        distanceResponse.getRoute().get(0).getLatitude(),
                        distanceResponse.getRoute().get(0).getLongitude()
                    );
                    MapNode lastNode = new MapNode(
                        distanceResponse.getRoute().get(distanceResponse.getRoute().size() - 1).getLatitude(),
                        distanceResponse.getRoute().get(distanceResponse.getRoute().size() - 1).getLongitude()
                    );
                    
                    MapNode startProj = findNearestPointOnRoad(startNode, nearestPrimaryRoad);
                    MapNode endProj = findNearestPointOnRoad(lastNode, nearestPrimaryRoad);
                    
                    // 设置电动车上下车点
                    distanceResponse.setElectricStartPoint(new RouteResponse.Point(
                        startProj.getLatitude(), startProj.getLongitude()));
                    distanceResponse.setElectricEndPoint(new RouteResponse.Point(
                        endProj.getLatitude(), endProj.getLongitude()));
                    
                    // 步行段的时间（使用步行速度1.4米/秒）
                    double walkingTime = (distanceResponse.getStartToRoadDistance() + 
                                        distanceResponse.getEndToRoadDistance()) / WALKING_SPEED / 60;
                    
                    // 道路段的时间（分段累加每段拥挤度）
                    double roadTime = 0.0;
                    List<MapNode> path = distanceResponse.getRoute().stream()
                        .map(p -> new MapNode(p.getLatitude(), p.getLongitude()))
                        .collect(Collectors.toList());
                    
                    for (int i = 0; i < path.size() - 1; i++) {
                        MapNode current = path.get(i);
                        MapNode next = path.get(i + 1);
                        double segmentDistance = calculateDistance(current, next);
                        // 找到当前段对应的道路，优先找主干道
                        MapRoad road = findNearestRoad(current, roads);
                        if (road != null && "primary".equals(road.getRoadType())) {
                            double crowdLevel = Math.max(0.05, Math.min(1.0, road.getCrowdLevel()));
                            double speed = ELECTRIC_SPEED * crowdLevel;
                            roadTime += segmentDistance / speed / 60;
                        } else {
                            // 非主干道或没有道路，使用步行速度
                            roadTime += segmentDistance / WALKING_SPEED / 60;
                        }
                    }
                    
                    // 总时间 = 步行段 + 道路段
                    estimatedTime = walkingTime + roadTime;
                } else {
                    logger.warn("[Time] No primary roads found, using default walking speed");
                    estimatedTime = distanceResponse.getTotalDistance() / WALKING_SPEED / 60;
                }
            } else {
                // 自行车模式
                // 步行段的时间（使用步行速度1.4米/秒）
                double walkingTime = (distanceResponse.getStartToRoadDistance() + 
                                    distanceResponse.getEndToRoadDistance()) / WALKING_SPEED / 60;
                
                // 道路段的时间（使用自行车速度4.2米/秒 * 道路拥挤度）
                double roadDistance = distanceResponse.getTotalDistance() - 
                                    distanceResponse.getStartToRoadDistance() - 
                                    distanceResponse.getEndToRoadDistance();
                
                List<MapRoad> roads = distanceResponse.getRoads();
                if (roads.isEmpty()) {
                    estimatedTime = distanceResponse.getTotalDistance() / WALKING_SPEED / 60;
                } else {
                    double avgCrowdLevel = roads.stream()
                        .mapToDouble(MapRoad::getCrowdLevel)
                        .average()
                        .orElse(1.0);
                    avgCrowdLevel = Math.max(0.05, Math.min(1.0, avgCrowdLevel));
                    double bikeSpeed = BICYCLE_SPEED * avgCrowdLevel;
                    double roadTime = roadDistance / bikeSpeed / 60;
                    estimatedTime = walkingTime + roadTime;
                }
            }

            // 更新时间
            distanceResponse.setEstimatedTime(Double.parseDouble(String.format("%.2f", estimatedTime)));
            return distanceResponse;
        } catch (Exception e) {
            logger.error("[Time] Error planning route", e);
            throw new RuntimeException("Failed to plan time-optimal route: " + e.getMessage());
        }
    }

    // 用时最短路径：全排列所有路径点顺序，拼接完整用时最短路径，计算实际总用时，选最短
    private List<MapNode> findOptimalPathByTime(MapNode startNode, List<MapNode> pathNodes, RouteRequest request) {
        if (pathNodes.isEmpty()) {
            logger.warn("[Time] Path nodes list is empty");
            return new ArrayList<>();
        }
        if (pathNodes.size() == 1 && !request.isAllowReturn()) {
            logger.info("[Time] Single path node, finding direct path");
            List<MapNode> path = findPathByTime(startNode, pathNodes.get(0), request);
            if (path.isEmpty()) {
                logger.error("[Time] Failed to find path between start node and single path node");
            }
            return path;
        }
        
        logger.info("[Time] Finding optimal path with {} path nodes", pathNodes.size());
        
        // 使用与距离最短算法相同的全排列方法
        int n = pathNodes.size();
        List<Integer> idxList = new ArrayList<>();
        for (int i = 0; i < n; i++) idxList.add(i);
        List<MapNode> bestPath = null;
        double minTime = Double.MAX_VALUE;
        List<List<Integer>> allOrders = new ArrayList<>();
        permute(idxList, 0, allOrders);
        
        for (List<Integer> order : allOrders) {
            List<MapNode> seq = new ArrayList<>();
            seq.add(startNode);
            for (int idx : order) seq.add(pathNodes.get(idx));
            
            // 拼接完整路径
            List<MapNode> fullPath = new ArrayList<>();
            double totalTime = 0;
            boolean valid = true;
            
            for (int i = 0; i < seq.size() - 1; i++) {
                MapNode a = seq.get(i);
                MapNode b = seq.get(i + 1);
                
                // 使用时间最短路径算法找到两点间的路径
                List<MapNode> segment = findPathByTime(a, b, request);
                if (segment.isEmpty()) {
                    valid = false;
                    break;
                }
                
                // 验证路径段是否都在道路上
                List<MapRoad> segmentRoads = getRoadsInPath(segment);
                if (segmentRoads.isEmpty()) {
                    valid = false;
                    break;
                }
                
                // 计算这段路径的实际时间
                double segmentTime = calculateTotalTimeForPath(segment, segmentRoads, request);
                
                if (i > 0) segment.remove(0); // 去重重叠节点
                fullPath.addAll(segment);
                totalTime += segmentTime;
            }
            
            if (!valid) continue;
            
            // 更新最短时间路径
            if (totalTime < minTime) {
                minTime = totalTime;
                bestPath = fullPath;
                logger.info("[Time] Found new best path with time: {} minutes", String.format("%.2f", minTime));
            }
        }
        
        if (bestPath == null) {
            logger.error("[Time] Failed to find valid path for all waypoints");
            return new ArrayList<>();
        }
        
        // 验证最终路径是否都在道路上
        List<MapRoad> finalRoads = getRoadsInPath(bestPath);
        if (finalRoads.isEmpty()) {
            logger.error("[Time] Final path not on roads");
            return new ArrayList<>();
        }
        
        logger.info("[Time] Found optimal path with {} nodes and {} roads, total time: {} minutes", 
            bestPath.size(), finalRoads.size(), String.format("%.2f", minTime));
        return bestPath;
    }

    private List<MapNode> findOptimalPathByBranchAndBound(MapNode startNode, List<MapNode> pathNodes, RouteRequest request) {
        // 初始化优先队列，按总估计时间排序
        PriorityQueue<SearchState> queue = new PriorityQueue<>(
            Comparator.comparingDouble(SearchState::getTotalEstimate)
        );

        // 初始化起始状态
        List<MapNode> initialStatePath = new ArrayList<>();
        initialStatePath.add(startNode);
        Set<MapNode> initialVisited = new HashSet<>();
        initialVisited.add(startNode);
        double initialLowerBound = calculateLowerBound(startNode, pathNodes, initialVisited, request);
        
        queue.add(new SearchState(initialStatePath, initialVisited, 0.0, initialLowerBound));

        // 记录最优解
        List<MapNode> bestPath = null;
        double bestTime = Double.MAX_VALUE;
        int iterations = 0;
        final int MAX_ITERATIONS = 10000;

        // 分支限界搜索
        while (!queue.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            SearchState current = queue.poll();

            // 剪枝：如果当前估计值已经超过最优解，跳过
            if (current.getTotalEstimate() >= bestTime) {
                continue;
            }

            // 如果已经访问了所有节点
            if (current.getVisited().size() == pathNodes.size() + 1) {
                if (current.getCurrentTime() < bestTime) {
                    bestTime = current.getCurrentTime();
                    bestPath = new ArrayList<>(current.getCurrentPath());
                    logger.info("[BranchAndBound] Found new best path with time: {} min", 
                        String.format("%.2f", bestTime));
                }
                continue;
            }

            // 扩展当前状态
            for (MapNode next : pathNodes) {
                if (next == null || current.getVisited().contains(next)) {
                    continue;
                }

                // 计算到下一个节点的时间
                MapNode lastNode = current.getCurrentPath().get(current.getCurrentPath().size() - 1);
                if (lastNode == null) {
                    continue;
                }

                // 使用改进的路径查找方法
                List<MapNode> segment = findPathByTime(lastNode, next, request);
                if (segment.isEmpty()) {
                    logger.warn("[BranchAndBound] No valid path found between nodes");
                    continue;
                }

                // 验证路径段是否都在道路上
                List<MapRoad> segmentRoads = getRoadsInPath(segment);
                if (segmentRoads.isEmpty()) {
                    logger.warn("[BranchAndBound] Path segment not on roads");
                    continue;
                }

                double segmentTime = calculateTotalTimeForPath(segment, segmentRoads, request);
                if (segmentTime < 0) {
                    continue;
                }

                double newTime = current.getCurrentTime() + segmentTime;

                // 如果新路径的时间已经超过最优解，跳过
                if (newTime >= bestTime) {
                    continue;
                }

                // 创建新状态
                List<MapNode> newPath = new ArrayList<>(current.getCurrentPath());
                newPath.add(next);
                Set<MapNode> newVisited = new HashSet<>(current.getVisited());
                newVisited.add(next);
                double newLowerBound = calculateLowerBound(next, pathNodes, newVisited, request);

                queue.add(new SearchState(newPath, newVisited, newTime, newLowerBound));
            }
        }

        if (iterations >= MAX_ITERATIONS) {
            logger.warn("[BranchAndBound] Reached maximum iterations limit");
        }
        
        if (bestPath == null) {
            logger.warn("[BranchAndBound] No valid path found");
            return new ArrayList<>();
        }
        
        // 验证最终路径是否都在道路上
        List<MapRoad> finalRoads = getRoadsInPath(bestPath);
        if (finalRoads.isEmpty()) {
            logger.error("[BranchAndBound] Final path not on roads");
            return new ArrayList<>();
        }
        
        logger.info("[BranchAndBound] Found optimal path with total time: {} min after {} iterations", 
            String.format("%.2f", bestTime), iterations);
        return bestPath;
    }

    private double calculateLowerBound(MapNode current, List<MapNode> allNodes, 
        Set<MapNode> visited, RouteRequest request) {
        
        if (current == null || allNodes == null || visited == null || request == null) {
            return Double.MAX_VALUE;
        }

        double lowerBound = 0.0;
        List<MapNode> unvisited = allNodes.stream()
            .filter(node -> node != null && !visited.contains(node))
            .collect(Collectors.toList());

        if (unvisited.isEmpty()) {
            return 0.0;
        }

        // 1. 计算到最近未访问节点的最小时间
        double minTimeToNext = Double.MAX_VALUE;
        for (MapNode next : unvisited) {
            double time = estimateTime(current, next, request);
            if (time >= 0) {
                minTimeToNext = Math.min(minTimeToNext, time);
            }
        }
        lowerBound += minTimeToNext;

        // 2. 计算未访问节点之间的最小生成树时间
        if (unvisited.size() > 1) {
            double mstTime = calculateMSTTime(unvisited, request);
            if (mstTime >= 0) {
                lowerBound += mstTime;
            }
        }

        // 3. 根据交通方式添加额外的时间估计
        switch (request.getTransportMode()) {
            case "electric":
                // 电动车模式：考虑主干道优先
                lowerBound *= 0.8; // 假设主干道可以节省20%的时间
                break;
            case "bicycle":
                // 自行车模式：考虑道路拥挤度
                lowerBound *= 0.9; // 假设平均拥挤度为0.9
                break;
            case "walking":
                // 步行模式：考虑地形因素
                lowerBound *= 1.1; // 假设地形因素增加10%的时间
                break;
        }

        return lowerBound;
    }

    private double calculateSegmentTime(MapNode from, MapNode to, RouteRequest request) {
        double distance = calculateDistance(from, to);
        
        switch (request.getTransportMode()) {
            case "walking":
                // 步行模式：考虑地形因素
                double terrainFactor = 1.0;
                MapRoad road = findNearestRoad(from, mapRoadRepository.findAll());
                if (road != null) {
                    // 根据道路类型调整地形因子
                    switch (road.getRoadType()) {
                        case "primary":
                            terrainFactor = 0.9; // 主干道更容易行走
                            break;
                        case "secondary":
                            terrainFactor = 0.95;
                            break;
                        default:
                            terrainFactor = 1.0;
                    }
                }
                return distance / (WALKING_SPEED * terrainFactor) / 60;
                
            case "electric":
                // 电动车模式：优先使用主干道
                road = findNearestRoad(from, mapRoadRepository.findAll());
                if (road != null) {
                    if ("primary".equals(road.getRoadType())) {
                        // 主干道上使用电动车速度
                        double crowdLevel = Math.max(0.05, Math.min(1.0, road.getCrowdLevel()));
                        double speed = ELECTRIC_SPEED * crowdLevel;
                        return distance / speed / 60;
                    } else {
                        // 非主干道使用较低速度
                        double crowdLevel = Math.max(0.05, Math.min(1.0, road.getCrowdLevel()));
                        double speed = ELECTRIC_SPEED * 0.7 * crowdLevel; // 非主干道速度降低30%
                        return distance / speed / 60;
                    }
                }
                // 没有找到道路，使用步行速度
                return distance / WALKING_SPEED / 60;
                
            case "bicycle":
                // 自行车模式：考虑道路类型和拥挤度
                road = findNearestRoad(from, mapRoadRepository.findAll());
                if (road != null) {
                    double crowdLevel = Math.max(0.05, Math.min(1.0, road.getCrowdLevel()));
                    double speed = BICYCLE_SPEED * crowdLevel;
                    // 根据道路类型调整速度
                    switch (road.getRoadType()) {
                        case "primary":
                            speed *= 1.1; // 主干道速度提高10%
                            break;
                        case "secondary":
                            speed *= 1.05; // 次干道速度提高5%
                            break;
                    }
                    return distance / speed / 60;
                }
                // 没有找到道路，使用步行速度
                return distance / WALKING_SPEED / 60;
                
            default:
                return distance / WALKING_SPEED / 60;
        }
    }

    private double calculateMSTTime(List<MapNode> nodes, RouteRequest request) {
        if (nodes == null || request == null || nodes.size() <= 1) {
            return 0.0;
        }

        // 使用Prim算法计算最小生成树
        Set<MapNode> mstNodes = new HashSet<>();
        mstNodes.add(nodes.get(0));
        double totalTime = 0.0;

        while (mstNodes.size() < nodes.size()) {
            double minTime = Double.MAX_VALUE;
            MapNode nextNode = null;

            for (MapNode node : mstNodes) {
                if (node == null) continue;
                for (MapNode candidate : nodes) {
                    if (candidate == null || mstNodes.contains(candidate)) continue;
                    double time = estimateTime(node, candidate, request);
                    if (time >= 0 && time < minTime) {
                        minTime = time;
                        nextNode = candidate;
                    }
                }
            }

            if (nextNode != null) {
                mstNodes.add(nextNode);
                totalTime += minTime;
            } else {
                break; // 无法找到下一个节点，退出循环
            }
        }

        return totalTime;
    }

    private List<MapNode> findPathByTime(MapNode start, MapNode end, RouteRequest request) {
        // 检查缓存
        PathResult cachedResult = getCachedPath(start, end, request.getTransportMode());
        if (cachedResult != null) {
            logger.debug("[Time] Cache hit for path from ({}, {}) to ({}, {})", 
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude());
            return new ArrayList<>(cachedResult.getPath());
        }

        logger.info("[Time] Finding path from ({}, {}) to ({}, {})", 
            start.getLatitude(), start.getLongitude(),
            end.getLatitude(), end.getLongitude());

        // 找到起点和终点各自最近的道路
        List<MapRoad> allRoads = mapRoadRepository.findAll();
        logger.info("[Time] Found {} roads in database", allRoads.size());

        MapRoad startRoad = findNearestRoad(start, allRoads);
        MapRoad endRoad = findNearestRoad(end, allRoads);
        
        if (startRoad == null || endRoad == null) {
            logger.warn("[Time] No nearest road found for start or end point");
            return new ArrayList<>();
        }

        logger.info("[Time] Found nearest roads - Start road: {}, End road: {}", 
            startRoad.getId(), endRoad.getId());

        // 找到起点到最近道路的投影点
        MapNode startProj = findNearestPointOnRoad(start, startRoad);
        // 找到终点到最近道路的投影点
        MapNode endProj = findNearestPointOnRoad(end, endRoad);
        
        if (startProj == null || endProj == null) {
            logger.warn("[Time] Failed to project points onto roads");
            return new ArrayList<>();
        }

        logger.info("[Time] Projected points - Start: ({}, {}), End: ({}, {})", 
            startProj.getLatitude(), startProj.getLongitude(),
            endProj.getLatitude(), endProj.getLongitude());

        // 构建三段路径：起点->起点投影点->终点投影点->终点
        List<MapNode> path = new ArrayList<>();
        path.add(start);
        path.add(startProj);
        
        // 在道路网络上寻找从起点投影点到终点投影点的路径
        List<MapNode> roadPath = findPathOnRoads(startProj, endProj, request);
        if (!roadPath.isEmpty()) {
            // 验证路径段是否都在道路上
            List<MapRoad> roadPathRoads = getRoadsInPath(roadPath);
            if (!roadPathRoads.isEmpty()) {
                path.addAll(roadPath);
                path.add(endProj);
                path.add(end);
                
                // 验证完整路径是否都在道路上
                List<MapRoad> fullPathRoads = getRoadsInPath(path);
                if (!fullPathRoads.isEmpty()) {
                    // 缓存结果
                    double time = calculateTotalTimeForPath(path, fullPathRoads, request);
                    cachePath(start, end, request.getTransportMode(), new PathResult(path, time, fullPathRoads));
                    logger.info("[Time] Found valid path with {} nodes and {} roads", path.size(), fullPathRoads.size());
                    return path;
                } else {
                    logger.warn("[Time] Full path not on roads");
                }
            } else {
                logger.warn("[Time] Road path segment not on roads");
            }
        } else {
            logger.warn("[Time] No road path found between projected points");
        }
        
        logger.error("[Time] Failed to find valid path on road network between ({}, {}) and ({}, {})", 
            startProj.getLatitude(), startProj.getLongitude(), 
            endProj.getLatitude(), endProj.getLongitude());
        return new ArrayList<>();
    }

    private List<MapNode> findPathOnRoads(MapNode start, MapNode end, RouteRequest request) {
        // 初始化开放列表和关闭列表
        PriorityQueue<MapNode> openList = new PriorityQueue<>(Comparator.comparingDouble(MapNode::getF));
        Set<MapNode> closedList = new HashSet<>();
        Map<MapNode, Double> gScore = new HashMap<>();
        Map<MapNode, MapNode> cameFrom = new HashMap<>();
        
        gScore.put(start, 0.0);
        start.setF(estimateTime(start, end, request));
        openList.add(start);
        
        logger.info("[Time] Starting road network search from ({}, {}) to ({}, {})", 
            start.getLatitude(), start.getLongitude(),
            end.getLatitude(), end.getLongitude());
        
        while (!openList.isEmpty()) {
            MapNode current = openList.poll();
            
            if (current.equals(end)) {
                List<MapNode> path = reconstructPath(cameFrom, current);
                logger.info("[Time] Found path with {} nodes", path.size());
                return path;
            }
            
            closedList.add(current);
            
            // 获取所有邻居节点
            List<MapNode> neighbors = current.getNeighbors();
            logger.debug("[Time] Current node has {} neighbors", neighbors.size());
            
            for (MapNode neighbor : neighbors) {
                if (closedList.contains(neighbor)) {
                    continue;
                }
                
                // 计算从起点经过当前节点到邻居节点的代价
                double tentativeG = gScore.get(current) + calculateSegmentTime(current, neighbor, request);
                
                if (!gScore.containsKey(neighbor) || tentativeG < gScore.get(neighbor)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    neighbor.setF(tentativeG + estimateTime(neighbor, end, request));
                    
                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }
        
        logger.error("[Time] Failed to find path on road network between ({}, {}) and ({}, {})", 
            start.getLatitude(), start.getLongitude(),
            end.getLatitude(), end.getLongitude());
        return new ArrayList<>();
    }

    private double estimateTime(MapNode from, MapNode to, RouteRequest request) {
        // 使用距离作为基础，然后根据交通方式调整
        double distance = calculateDistance(from, to);
        switch (request.getTransportMode()) {
            case "bicycle":
                // 使用平均拥挤度估计
                return distance / (BICYCLE_SPEED * 0.7); // 假设平均拥挤度为0.7
            case "electric":
                // 电动车模式使用更保守的估计
                return distance / (ELECTRIC_SPEED * 0.5); // 假设平均拥挤度为0.5
            default:
                return distance / WALKING_SPEED;
        }
    }

    private MapRoad findNearestPrimaryRoad(MapNode node, List<MapRoad> roads) {
        MapRoad nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        logger.debug("[Electric] Finding nearest primary road for node ({}, {})", 
            node.getLatitude(), node.getLongitude());
        
        for (MapRoad road : roads) {
            if (!"primary".equals(road.getRoadType())) {
                continue;
            }
            
            double distance = calculateDistanceToRoad(node, road);
            logger.debug("[Electric] Checking primary road {} at distance {} meters", 
                road.getId(), String.format("%.2f", distance));
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = road;
                logger.debug("[Electric] Found closer primary road {} at distance {} meters", 
                    road.getId(), String.format("%.2f", distance));
                }
            }
        
        if (nearest != null) {
            logger.info("[Electric] Found nearest primary road {} at distance {} meters", 
                nearest.getId(), String.format("%.2f", minDistance));
        } else {
            logger.warn("[Electric] No primary road found within {} meters", 
                ELECTRIC_PRIMARY_ROAD_THRESHOLD);
        }
        
        return nearest;
    }

    // 主接口：同时返回距离最短和用时最短路径
    public Map<String, Object> planRouteMulti(RouteRequest request) {
        RouteResponse distancePath = planRoute(request);
        RouteResponse timePath = planRouteByTime(request);
        boolean same = distancePath.getRoute().size() == timePath.getRoute().size();
        if (same) {
            same = true;
            for (int i = 0; i < distancePath.getRoute().size(); i++) {
                RouteResponse.Point a = distancePath.getRoute().get(i);
                RouteResponse.Point b = timePath.getRoute().get(i);
                if (Math.abs(a.getLatitude() - b.getLatitude()) > 1e-6 || Math.abs(a.getLongitude() - b.getLongitude()) > 1e-6) {
                    same = false;
                    break;
                }
            }
        }

        // 如果是电动车模式，确保两条路径都有相同的上下车点
        if ("electric".equals(request.getTransportMode())) {
            RouteResponse.Point start = null;
            RouteResponse.Point end = null;
            if (distancePath.getElectricStartPoint() != null) {
                start = new RouteResponse.Point(distancePath.getElectricStartPoint().getLatitude(), distancePath.getElectricStartPoint().getLongitude());
            }
            if (distancePath.getElectricEndPoint() != null) {
                end = new RouteResponse.Point(distancePath.getElectricEndPoint().getLatitude(), distancePath.getElectricEndPoint().getLongitude());
            }
            timePath.setElectricStartPoint(start);
            timePath.setElectricEndPoint(end);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("distancePath", distancePath);
        result.put("timePath", timePath);
        result.put("samePath", same);
        return result;
    }

    public RoutePath planElectricRoute(Point start, Point end, List<Point> waypoints) {
        // 1. 找到最近的 primary 类型道路入口点
        Point primaryRoadEntry = findNearestPrimaryRoadEntry(start);
        
        // 2. 找到最近的 primary 类型道路出口点
        Point primaryRoadExit = findNearestPrimaryRoadExit(end);
        
        // 3. 规划三段路径
        // 步行段1：起点到主干道入口
        RouteSegment walkingSegment1 = planWalkingRoute(start, primaryRoadEntry);
        
        // 电动车段：从主干道入口开始，可以在任何道路上行驶
        RouteSegment electricSegment = planElectricRouteFromPrimary(primaryRoadEntry, primaryRoadExit);
        
        // 步行段2：主干道出口到终点
        RouteSegment walkingSegment2 = planWalkingRoute(primaryRoadExit, end);
        
        // 4. 合并路径段
        List<RouteSegment> segments = new ArrayList<>();
        segments.add(walkingSegment1);
        segments.add(electricSegment);
        segments.add(walkingSegment2);
        
        // 5. 合并所有点
        List<Point> allPoints = new ArrayList<>();
        allPoints.addAll(walkingSegment1.getPoints());
        allPoints.addAll(electricSegment.getPoints());
        allPoints.addAll(walkingSegment2.getPoints());
        
        // 6. 计算总距离和时间
        double totalDistance = walkingSegment1.getDistance() + 
                             electricSegment.getDistance() + 
                             walkingSegment2.getDistance();
        
        double totalTime = walkingSegment1.getTime() + 
                          electricSegment.getTime() + 
                          walkingSegment2.getTime();
        
        // 7. 创建路径对象并设置电动车上下车点
        RoutePath path = new RoutePath(allPoints, totalDistance, totalTime, segments);
        path.setElectricStartPoint(primaryRoadEntry);
        path.setElectricEndPoint(primaryRoadExit);
        return path;
    }
    
    private Point findNearestPrimaryRoadEntry(Point start) {
        logger.info("[Electric] Finding nearest primary road entry point for start point ({}, {})", 
            start.getLatitude(), start.getLongitude());
        
        List<MapRoad> primaryRoads = mapRoadRepository.findAll().stream()
            .filter(road -> "primary".equals(road.getRoadType()))
            .collect(Collectors.toList());
        
        if (primaryRoads.isEmpty()) {
            logger.warn("[Electric] No primary roads found in database");
            return start;
        }
        
        MapRoad nearestRoad = null;
        double minDistance = Double.MAX_VALUE;
        Point nearestPoint = start;
        
        for (MapRoad road : primaryRoads) {
            String[] points = road.getPathPoints().split(";");
            for (int i = 0; i < points.length - 1; i++) {
                String[] startCoords = points[i].split(",");
                String[] endCoords = points[i + 1].split(",");
                
                double startLat = Double.parseDouble(startCoords[0]);
                double startLon = Double.parseDouble(startCoords[1]);
                double endLat = Double.parseDouble(endCoords[0]);
                double endLon = Double.parseDouble(endCoords[1]);
                
                // 计算点到线段的距离和最近点
                Point segmentPoint = findNearestPointOnLineSegment(
                    start,
                    new Point(startLat, startLon),
                    new Point(endLat, endLon)
                );
                
                double distance = DistanceUtil.calculateDistance(start, segmentPoint);
                if (distance < minDistance && distance <= ELECTRIC_PRIMARY_ROAD_THRESHOLD) {
                    minDistance = distance;
                    nearestRoad = road;
                    nearestPoint = segmentPoint;
                }
            }
        }
        
        if (nearestRoad != null) {
            logger.info("[Electric] Found nearest primary road entry point at ({}, {}), distance: {} meters", 
                nearestPoint.getLatitude(), nearestPoint.getLongitude(), 
                String.format("%.2f", minDistance));
        } else {
            logger.warn("[Electric] No primary road found within {} meters, using original start point", 
                ELECTRIC_PRIMARY_ROAD_THRESHOLD);
        }
        
        return nearestPoint;
    }
    
    private Point findNearestPrimaryRoadExit(Point end) {
        logger.info("[Electric] Finding nearest primary road exit point for end point ({}, {})", 
            end.getLatitude(), end.getLongitude());
        
        List<MapRoad> primaryRoads = mapRoadRepository.findAll().stream()
            .filter(road -> "primary".equals(road.getRoadType()))
            .collect(Collectors.toList());
        
        if (primaryRoads.isEmpty()) {
            logger.warn("[Electric] No primary roads found in database");
            return end;
        }
        
        MapRoad nearestRoad = null;
        double minDistance = Double.MAX_VALUE;
        Point nearestPoint = end;
        
        for (MapRoad road : primaryRoads) {
            String[] points = road.getPathPoints().split(";");
            for (int i = 0; i < points.length - 1; i++) {
                String[] startCoords = points[i].split(",");
                String[] endCoords = points[i + 1].split(",");
                
                double startLat = Double.parseDouble(startCoords[0]);
                double startLon = Double.parseDouble(startCoords[1]);
                double endLat = Double.parseDouble(endCoords[0]);
                double endLon = Double.parseDouble(endCoords[1]);
                
                // 计算点到线段的距离和最近点
                Point segmentPoint = findNearestPointOnLineSegment(
                    end,
                    new Point(startLat, startLon),
                    new Point(endLat, endLon)
                );
                
                double distance = DistanceUtil.calculateDistance(end, segmentPoint);
                if (distance < minDistance && distance <= ELECTRIC_PRIMARY_ROAD_THRESHOLD) {
                    minDistance = distance;
                    nearestRoad = road;
                    nearestPoint = segmentPoint;
                }
            }
        }
        
        if (nearestRoad != null) {
            logger.info("[Electric] Found nearest primary road exit point at ({}, {}), distance: {} meters", 
                nearestPoint.getLatitude(), nearestPoint.getLongitude(), 
                String.format("%.2f", minDistance));
        } else {
            logger.warn("[Electric] No primary road found within {} meters, using original end point", 
                ELECTRIC_PRIMARY_ROAD_THRESHOLD);
        }
        
        return nearestPoint;
    }
    
    private RouteSegment planWalkingRoute(Point start, Point end) {
        // TODO: 实现步行路径规划逻辑
        List<Point> points = new ArrayList<>();
        points.add(start);
        points.add(end);
        
        double distance = DistanceUtil.calculateDistance(start, end);
        double time = distance / WALKING_SPEED;
        
        return new RouteSegment(points, "walking", distance, time);
    }
    
    private RouteSegment planElectricRouteFromPrimary(Point start, Point end) {
        logger.info("[Electric] Planning route from primary road entry ({}, {}) to ({}, {})", 
            start.getLatitude(), start.getLongitude(),
            end.getLatitude(), end.getLongitude());
        
        // 获取所有道路
        List<MapRoad> allRoads = mapRoadRepository.findAll();
        
        if (allRoads.isEmpty()) {
            logger.warn("[Electric] No roads found, using direct path");
            return planWalkingRoute(start, end);
        }
        
        // 找到包含起点和终点的道路
        MapRoad startRoad = findRoadContainingPoint(start, allRoads);
        MapRoad endRoad = findRoadContainingPoint(end, allRoads);
        
        if (startRoad == null || endRoad == null) {
            logger.warn("[Electric] Start or end point not on road, using direct path");
            return planWalkingRoute(start, end);
        }
        
        // 如果起点和终点在同一条道路上
        if (startRoad.getId().equals(endRoad.getId())) {
        List<Point> points = new ArrayList<>();
        points.add(start);
        points.add(end);
        
        double distance = DistanceUtil.calculateDistance(start, end);
            double crowdLevel = Math.max(0.05, Math.min(1.0, startRoad.getCrowdLevel()));
        double electricSpeed = ELECTRIC_SPEED * crowdLevel;
        double time = distance / electricSpeed;
        
            logger.info("[Electric] Start and end points on same road, distance: {}m, time: {}min", 
                String.format("%.2f", distance),
                String.format("%.2f", time / 60));
            
        return new RouteSegment(points, "electric", distance, time);
        }
        
        // 如果不在同一条道路上，需要找到连接的道路
        List<Point> points = new ArrayList<>();
        points.add(start);
        
        // 获取道路连接点
        Point connectionPoint = findRoadConnectionPoint(startRoad, endRoad);
        if (connectionPoint != null) {
            points.add(connectionPoint);
        }
        
        points.add(end);
        
        // 计算总距离和时间
        double totalDistance = 0;
        double totalTime = 0;
        
        for (int i = 0; i < points.size() - 1; i++) {
            Point current = points.get(i);
            Point next = points.get(i + 1);
            
            double segmentDistance = DistanceUtil.calculateDistance(current, next);
            totalDistance += segmentDistance;
            
            // 获取当前段的道路
            MapRoad road = findRoadContainingPoint(current, allRoads);
            if (road != null) {
                double crowdLevel = Math.max(0.05, Math.min(1.0, road.getCrowdLevel()));
                double electricSpeed = ELECTRIC_SPEED * crowdLevel;
                totalTime += segmentDistance / electricSpeed;
            } else {
                totalTime += segmentDistance / WALKING_SPEED;
            }
        }
        
        logger.info("[Electric] Route planned from primary road, total distance: {}m, total time: {}min", 
            String.format("%.2f", totalDistance),
            String.format("%.2f", totalTime / 60));
        
        return new RouteSegment(points, "electric", totalDistance, totalTime);
    }
    
    private MapRoad findRoadContainingPoint(Point point, List<MapRoad> roads) {
        for (MapRoad road : roads) {
            String[] points = road.getPathPoints().split(";");
            for (String pointStr : points) {
                if (pointStr.trim().isEmpty()) continue;
                
                String[] coords = pointStr.split(",");
                double lat = Double.parseDouble(coords[0]);
                double lon = Double.parseDouble(coords[1]);
                
                double distance = DistanceUtil.calculateDistance(
                    new Point(lat, lon),
                    point
                );
                
                if (distance <= ELECTRIC_PRIMARY_ROAD_THRESHOLD) {
                    return road;
                }
            }
        }
        return null;
    }
    
    private Point findRoadConnectionPoint(MapRoad road1, MapRoad road2) {
        String[] points1 = road1.getPathPoints().split(";");
        String[] points2 = road2.getPathPoints().split(";");
        
        double minDistance = Double.MAX_VALUE;
        Point connectionPoint = null;
        
        for (String point1Str : points1) {
            if (point1Str.trim().isEmpty()) continue;
            
            String[] coords1 = point1Str.split(",");
            double lat1 = Double.parseDouble(coords1[0]);
            double lon1 = Double.parseDouble(coords1[1]);
            Point point1 = new Point(lat1, lon1);
            
            for (String point2Str : points2) {
                if (point2Str.trim().isEmpty()) continue;
                
                String[] coords2 = point2Str.split(",");
                double lat2 = Double.parseDouble(coords2[0]);
                double lon2 = Double.parseDouble(coords2[1]);
                Point point2 = new Point(lat2, lon2);
                
                double distance = DistanceUtil.calculateDistance(point1, point2);
                if (distance < minDistance && distance <= ELECTRIC_PRIMARY_ROAD_THRESHOLD) {
                    minDistance = distance;
                    connectionPoint = new Point(
                        (lat1 + lat2) / 2,
                        (lon1 + lon2) / 2
                    );
                }
            }
        }
        
        return connectionPoint;
    }

    private double calculateTotalTimeForPath(List<MapNode> path, List<MapRoad> roads, RouteRequest request) {
        double totalTime = 0;
        
        if ("walking".equals(request.getTransportMode())) {
            double totalDistance = calculateTotalDistanceForNodes(path);
            totalTime = totalDistance / WALKING_SPEED / 60;
        } else if ("electric".equals(request.getTransportMode())) {
            // 电动车模式：分段计算时间
            for (int i = 0; i < path.size() - 1; i++) {
                MapNode current = path.get(i);
                MapNode next = path.get(i + 1);
                double segmentDistance = calculateDistance(current, next);
                
                // 检查阈值内是否有primary道路
                MapRoad primaryRoad = findNearestPrimaryRoad(current, roads);
                if (primaryRoad != null && calculateDistanceToRoad(current, primaryRoad) <= ELECTRIC_PRIMARY_ROAD_THRESHOLD) {
                    // 使用primary道路，考虑拥挤度
                    double crowdLevel = Math.max(0.05, Math.min(1.0, primaryRoad.getCrowdLevel()));
                    double speed = ELECTRIC_SPEED * crowdLevel;
                    totalTime += segmentDistance / speed / 60;
                } else {
                    // 没有primary道路，使用步行速度
                    totalTime += segmentDistance / WALKING_SPEED / 60;
                }
            }
        } else if ("bicycle".equals(request.getTransportMode())) {
            // 自行车模式：分段计算实际总时间
            for (int i = 0; i < path.size() - 1; i++) {
                MapNode current = path.get(i);
                MapNode next = path.get(i + 1);
                double segmentDistance = calculateDistance(current, next);
                
                MapRoad road = findNearestRoad(current, roads);
                if (road != null) {
                    double crowdLevel = Math.max(0.05, Math.min(1.0, road.getCrowdLevel()));
                    double speed = BICYCLE_SPEED * crowdLevel;
                    // 移除道路类型调整，只考虑拥挤度
                    double segmentTime = segmentDistance / speed / 60;
                    totalTime += segmentTime;
                    
                    logger.debug("[Bicycle] Path segment {}: distance={}m, crowdLevel={}, speed={}m/s, time={}min", 
                        i,
                        String.format("%.2f", segmentDistance),
                        String.format("%.2f", crowdLevel),
                        String.format("%.2f", speed),
                        String.format("%.2f", segmentTime));
                } else {
                    // 没有找到道路，使用步行速度
                    double segmentTime = segmentDistance / WALKING_SPEED / 60;
                    totalTime += segmentTime;
                    logger.debug("[Bicycle] Path segment {}: no road found, using walking speed, distance={}m, time={}min", 
                        i,
                        String.format("%.2f", segmentDistance),
                        String.format("%.2f", segmentTime));
                }
            }
            logger.info("[Bicycle] Total path time: {} minutes", String.format("%.2f", totalTime));
        }
        
        return totalTime;
    }

    private Point findNearestPointOnLineSegment(Point point, Point lineStart, Point lineEnd) {
        double x = point.getLatitude();
        double y = point.getLongitude();
        double x1 = lineStart.getLatitude();
        double y1 = lineStart.getLongitude();
        double x2 = lineEnd.getLatitude();
        double y2 = lineEnd.getLongitude();
        
        double A = x - x1;
        double B = y - y1;
        double C = x2 - x1;
        double D = y2 - y1;
        
        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = -1;
        
        if (lenSq != 0) {
            param = dot / lenSq;
        }
        
        double xx, yy;
        
        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }
        
        return new Point(xx, yy);
    }

    // 添加搜索状态类
    private static class SearchState {
        private final List<MapNode> currentPath;
        private final Set<MapNode> visited;
        private final double currentTime;
        private final double lowerBound;

        public SearchState(List<MapNode> currentPath, Set<MapNode> visited, double currentTime, double lowerBound) {
            this.currentPath = new ArrayList<>(currentPath);
            this.visited = new HashSet<>(visited);
            this.currentTime = currentTime;
            this.lowerBound = lowerBound;
        }

        public List<MapNode> getCurrentPath() {
            return currentPath;
        }

        public Set<MapNode> getVisited() {
            return visited;
        }

        public double getCurrentTime() {
            return currentTime;
        }

        public double getLowerBound() {
            return lowerBound;
        }

        public double getTotalEstimate() {
            return currentTime + lowerBound;
        }
    }
} 