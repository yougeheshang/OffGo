package org.tinkerhub.offgo.service;

import org.springframework.cache.annotation.Cacheable;
import org.tinkerhub.offgo.model.Road;
import org.tinkerhub.offgo.model.Intersection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LandmarkManager {
    private static final Logger logger = LoggerFactory.getLogger(LandmarkManager.class);
    private static final int NUM_LANDMARKS = 16; // 地标点数量
    private final Map<Long, Map<Long, Double>> landmarkDistances; // 地标点到其他点的距离
    private final List<Long> landmarks; // 地标点ID列表
    private final Map<Long, Intersection> intersections; // 所有交叉口
    private final Map<Long, Road> roads; // 所有道路
    private final Map<String, Double> heuristicCache;

    public LandmarkManager(Map<Long, Intersection> intersections, Map<Long, Road> roads) {
        this.intersections = intersections;
        this.roads = roads;
        this.landmarks = new ArrayList<>();
        this.landmarkDistances = new ConcurrentHashMap<>();
        this.heuristicCache = new ConcurrentHashMap<>();
        selectLandmarks();
        precomputeDistances();
        logger.info("Initialized LandmarkManager with {} landmarks", landmarks.size());
    }

    // 选择地标点（使用最大最小距离策略）
    private void selectLandmarks() {
        if (intersections.isEmpty()) return;

        // 随机选择第一个地标点
        List<Long> allNodes = new ArrayList<>(intersections.keySet());
        Random random = new Random();
        landmarks.add(allNodes.get(random.nextInt(allNodes.size())));

        // 使用最大最小距离策略选择剩余地标点
        while (landmarks.size() < NUM_LANDMARKS) {
            double maxMinDist = -1;
            Long bestLandmark = null;

            for (Long node : allNodes) {
                if (landmarks.contains(node)) continue;

                // 计算到已选地标点的最小距离
                double minDist = Double.MAX_VALUE;
                for (Long landmark : landmarks) {
                    double dist = calculateDirectDistance(node, landmark);
                    minDist = Math.min(minDist, dist);
                }

                if (minDist > maxMinDist) {
                    maxMinDist = minDist;
                    bestLandmark = node;
                }
            }

            if (bestLandmark != null) {
                landmarks.add(bestLandmark);
            }
        }
    }

    // 预计算所有地标点到其他点的最短路径
    private void precomputeDistances() {
        landmarks.parallelStream().forEach(landmark -> {
            Map<Long, Double> distances = new ConcurrentHashMap<>();
            Map<Long, Double> tempDistances = new HashMap<>();
            PriorityQueue<Map.Entry<Long, Double>> pq = new PriorityQueue<>(
                Map.Entry.comparingByValue()
            );

            // 初始化距离
            for (Long node : intersections.keySet()) {
                tempDistances.put(node, Double.MAX_VALUE);
            }
            tempDistances.put(landmark, 0.0);
            pq.offer(new AbstractMap.SimpleEntry<>(landmark, 0.0));

            // Dijkstra算法
            while (!pq.isEmpty()) {
                Map.Entry<Long, Double> current = pq.poll();
                Long node = current.getKey();
                double dist = current.getValue();

                if (dist > tempDistances.get(node)) continue;

                Intersection intersection = intersections.get(node);
                if (intersection == null) continue;

                // 遍历所有相邻节点
                for (Long roadId : intersection.getConnectedRoads()) {
                    Road road = roads.get(roadId);
                    if (road == null) continue;

                    // 获取道路连接的另一个交叉口
                    Long neighbor = road.getIntersections().stream()
                        .filter(id -> !id.equals(node))
                        .findFirst()
                        .orElse(null);
                    
                    if (neighbor == null) continue;

                    double newDist = dist + road.getLength();
                    if (newDist < tempDistances.get(neighbor)) {
                        tempDistances.put(neighbor, newDist);
                        pq.offer(new AbstractMap.SimpleEntry<>(neighbor, newDist));
                    }
                }
            }

            // 保存计算结果
            distances.putAll(tempDistances);
            landmarkDistances.put(landmark, distances);
        });
        
        logger.info("Precomputed distances for all landmarks");
    }

    // 计算两点间的直线距离（用于选择地标点）
    private double calculateDirectDistance(Long node1, Long node2) {
        Intersection i1 = intersections.get(node1);
        Intersection i2 = intersections.get(node2);
        if (i1 == null || i2 == null) return Double.MAX_VALUE;

        double dx = i1.getX() - i2.getX();
        double dy = i1.getY() - i2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // 获取Landmark启发式估计值，使用缓存
    @Cacheable(value = "heuristicCache", key = "#from + '->' + #to")
    public double getHeuristic(Long from, Long to) {
        String cacheKey = from + "->" + to;
        return heuristicCache.computeIfAbsent(cacheKey, k -> {
            double maxDist = 0;
            for (Long landmark : landmarks) {
                Map<Long, Double> distances = landmarkDistances.get(landmark);
                if (distances == null) continue;

                Double distToLandmark = distances.get(from);
                Double distFromLandmark = distances.get(to);
                if (distToLandmark == null || distFromLandmark == null) continue;

                // 使用三角不等式
                maxDist = Math.max(maxDist, Math.abs(distToLandmark - distFromLandmark));
            }
            logger.debug("Calculated new heuristic for {}: {}", cacheKey, maxDist);
            return maxDist;
        });
    }

    // 获取所有地标点
    public List<Long> getLandmarks() {
        return Collections.unmodifiableList(landmarks);
    }

    // 获取交叉口
    public Intersection getIntersection(Long id) {
        return intersections.get(id);
    }

    // 获取道路
    public Road getRoad(Long id) {
        return roads.get(id);
    }
} 