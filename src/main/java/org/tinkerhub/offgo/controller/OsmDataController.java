package org.tinkerhub.offgo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tinkerhub.offgo.entity.MapLocation;
import org.tinkerhub.offgo.entity.MapRoad;
import org.tinkerhub.offgo.repository.MapLocationRepository;
import org.tinkerhub.offgo.repository.MapRoadRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/osm")
public class OsmDataController {

    @Autowired
    private MapLocationRepository locationRepository;

    @Autowired
    private MapRoadRepository roadRepository;

    // 清华大学经纬度范围
    private static final double MIN_LAT = 39.99;
    private static final double MAX_LAT = 40.01;
    private static final double MIN_LON = 116.31;
    private static final double MAX_LON = 116.34;

    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getOsmData() {
        try {
            // 获取所有位置点
            List<MapLocation> allLocations = locationRepository.findAll();
            
            // 过滤出清华大学范围内的位置点
            List<MapLocation> filteredLocations = allLocations.stream()
                .filter(location -> 
                    location.getLatitude() >= MIN_LAT && 
                    location.getLatitude() <= MAX_LAT && 
                    location.getLongitude() >= MIN_LON && 
                    location.getLongitude() <= MAX_LON)
                .collect(Collectors.toList());

            // 获取所有道路
            List<MapRoad> allRoads = roadRepository.findAll();
            
            // 过滤出起点和终点都在清华大学范围内的道路
            List<MapRoad> filteredRoads = allRoads.stream()
                .filter(road -> {
                    // 获取道路的起点和终点位置
                    MapLocation startPoint = allLocations.stream()
                        .filter(loc -> loc.getId().equals(road.getStartPointId()))
                        .findFirst()
                        .orElse(null);
                    MapLocation endPoint = allLocations.stream()
                        .filter(loc -> loc.getId().equals(road.getEndPointId()))
                        .findFirst()
                        .orElse(null);

                    // 检查起点和终点是否都在范围内
                    if (startPoint == null || endPoint == null) {
                        return false;
                    }

                    boolean startInRange = startPoint.getLatitude() >= MIN_LAT && 
                                         startPoint.getLatitude() <= MAX_LAT && 
                                         startPoint.getLongitude() >= MIN_LON && 
                                         startPoint.getLongitude() <= MAX_LON;
                    
                    boolean endInRange = endPoint.getLatitude() >= MIN_LAT && 
                                       endPoint.getLatitude() <= MAX_LAT && 
                                       endPoint.getLongitude() >= MIN_LON && 
                                       endPoint.getLongitude() <= MAX_LON;

                    return startInRange && endInRange;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("locations", filteredLocations);
            response.put("roads", filteredRoads);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch OSM data: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/updateStartPoint")
    public ResponseEntity<String> updateStartPoint(@RequestBody Map<String, Object> request) {
        try {
            Double latitude = (Double) request.get("latitude");
            Double longitude = (Double) request.get("longitude");
            Boolean isStartPoint = (Boolean) request.get("isStartPoint");
            String type = (String) request.get("type");

            // 查找指定位置的点
            List<MapLocation> locations = locationRepository.findByLatitudeAndLongitude(latitude, longitude);
            
            if (locations.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // 如果是删除操作且是专门创建的起始点，则删除记录
            if (!isStartPoint && "start_point".equals(type)) {
                locationRepository.deleteAll(locations);
                return ResponseEntity.ok("Start point deleted successfully");
            }

            // 否则更新 isStartPoint 属性
            for (MapLocation location : locations) {
                location.setIsStartPoint(isStartPoint);
            }
            
            locationRepository.saveAll(locations);
            return ResponseEntity.ok("Start point updated successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating start point: " + e.getMessage());
        }
    }

    @PostMapping("/createStartPoint")
    public ResponseEntity<String> createStartPoint(@RequestBody MapLocation location) {
        try {
            // 打印接收到的数据
            System.out.println("Received location data: " + location);
            
            // 设置默认值
            if (location.getCrowdLevel() == null) {
                location.setCrowdLevel(0);
            }
            if (location.getCategory() == null) {
                location.setCategory("服务设施");
            }
            if (location.getDescription() == null) {
                location.setDescription("用户选择的起始点");
            }
            if (location.getName() == null) {
                location.setName("起始点");
            }
            if (location.getType() == null) {
                location.setType("start_point");
            }
            location.setIsStartPoint(true);
            
            // 打印处理后的数据
            System.out.println("Processed location data: " + location);
            
            // 保存到数据库
            try {
                MapLocation savedLocation = locationRepository.save(location);
                System.out.println("Saved location: " + savedLocation);
                return ResponseEntity.ok("Start point created successfully");
            } catch (Exception e) {
                System.err.println("Error saving location: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.internalServerError().body("Database error: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error processing location: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error creating start point: " + e.getMessage());
        }
    }

    @PostMapping("/resetStartPoints")
    public ResponseEntity<String> resetStartPoints() {
        try {
            // 查找所有标记为起始点的位置
            List<MapLocation> startPoints = locationRepository.findByIsStartPointTrue();
            
            // 重置所有起始点的状态
            for (MapLocation location : startPoints) {
                location.setIsStartPoint(false);
                locationRepository.save(location);
            }
            
            return ResponseEntity.ok("Start points reset successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error resetting start points: " + e.getMessage());
        }
    }

    @PostMapping("/deleteAllStartPoints")
    public ResponseEntity<String> deleteAllStartPoints() {
        try {
            // 查找所有类型为 start_point 的位置
            List<MapLocation> startPoints = locationRepository.findByType("start_point");
            
            // 删除所有起始点
            locationRepository.deleteAll(startPoints);
            
            return ResponseEntity.ok("All start points deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting start points: " + e.getMessage());
        }
    }
} 