package org.tinkerhub.offgo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tinkerhub.offgo.entity.MapLocation;
import org.tinkerhub.offgo.entity.MapRoad;
import org.tinkerhub.offgo.repository.MapLocationRepository;
import org.tinkerhub.offgo.repository.MapRoadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@Service
public class PointOfInterestService {
    private static final Logger logger = LoggerFactory.getLogger(PointOfInterestService.class);

    @Autowired
    private MapLocationRepository mapLocationRepository;

    @Autowired
    private MapRoadRepository mapRoadRepository;

    @Transactional
    public void updatePointOfInterestNames() {
        logger.info("开始更新兴趣点名称...");
        
        // 定义需要更新的类型映射
        String[][] typeMappings = {
            {"vending_machine", "自动贩卖机", "自动贩卖机"},
            {"fountain", "喷泉", "景点"},
            {"toilets", "厕所", "厕所"},
            {"sports_centre", "体育馆", "体育馆"},
            {"parking", "停车场", "停车场"},
            {"charging_station", "充电桩", "充电桩"},
            {"restaurant", "食堂", "食堂"}
        };

        for (String[] mapping : typeMappings) {
            String oldType = mapping[0];
            String newName = mapping[1];
            String newType = mapping[2];

            // 查找所有指定类型的位置点
            List<MapLocation> locations = mapLocationRepository.findByType(oldType);
            logger.info("找到 {} 个类型为 {} 的位置点", locations.size(), oldType);

            for (MapLocation location : locations) {
                // 更新名称和类型
                location.setName(newName);
                location.setType(newType);
                // 设置类别
                if (newType.equals("景点")) {
                    location.setCategory("景点");
                } else {
                    location.setCategory("服务设施");
                }
            }

            // 保存更新后的位置点
            mapLocationRepository.saveAll(locations);
            logger.info("已更新 {} 个位置点的名称和类型", locations.size());
        }

        logger.info("兴趣点名称更新完成");
    }

    @Transactional
    public void deleteNamelessPoints() {
        logger.info("开始删除没有名称的兴趣点...");
        
        // 查找所有没有名称的位置点
        List<MapLocation> namelessLocations = mapLocationRepository.findByNameIsNull();
        logger.info("找到 {} 个没有名称的位置点", namelessLocations.size());

        if (!namelessLocations.isEmpty()) {
            List<MapLocation> locationsToDelete = new ArrayList<>();
            
            // 检查每个位置点是否被引用
            for (MapLocation location : namelessLocations) {
                // 检查是否被道路引用
                List<MapRoad> roads = mapRoadRepository.findByStartPointIdOrEndPointId(location.getId(), location.getId());
                if (roads.isEmpty()) {
                    // 如果没有被引用，添加到待删除列表
                    locationsToDelete.add(location);
                } else {
                    logger.info("位置点 ID: {} 被 {} 条道路引用，跳过删除", location.getId(), roads.size());
                }
            }

            if (!locationsToDelete.isEmpty()) {
                // 删除未被引用的位置点
                mapLocationRepository.deleteAll(locationsToDelete);
                logger.info("已删除 {} 个未被引用的没有名称的位置点", locationsToDelete.size());
            } else {
                logger.info("没有找到可以安全删除的位置点");
            }
        }

        logger.info("删除没有名称的兴趣点完成");
    }

    @Transactional
    public void deleteSpecificTypePoints() {
        logger.info("开始删除特定类型的兴趣点...");
        
        // 定义要删除的类型
        List<String> typesToDelete = Arrays.asList("bicycle_parking", "parking_entrance");
        
        for (String type : typesToDelete) {
            // 查找所有指定类型的位置点
            List<MapLocation> locations = mapLocationRepository.findByType(type);
            logger.info("找到 {} 个类型为 {} 的位置点", locations.size(), type);

            if (!locations.isEmpty()) {
                for (MapLocation location : locations) {
                    // 查找所有引用这个位置点的道路
                    List<MapRoad> roads = mapRoadRepository.findByStartPointIdOrEndPointId(location.getId(), location.getId());
                    
                    if (!roads.isEmpty()) {
                        logger.info("位置点 ID: {} 被 {} 条道路引用，正在处理道路依赖", location.getId(), roads.size());
                        
                        // 处理每条道路
                        for (MapRoad road : roads) {
                            // 如果这个位置点是起点
                            if (road.getStartPointId().equals(location.getId())) {
                                // 将起点设置为终点
                                road.setStartPointId(road.getEndPointId());
                                // 将终点设置为null
                                road.setEndPointId(null);
                                logger.info("修改道路 ID: {} 的起点", road.getId());
                            }
                            // 如果这个位置点是终点
                            else if (road.getEndPointId().equals(location.getId())) {
                                // 将终点设置为null
                                road.setEndPointId(null);
                                logger.info("修改道路 ID: {} 的终点", road.getId());
                            }
                        }
                        
                        // 保存修改后的道路
                        mapRoadRepository.saveAll(roads);
                    }
                }
                
                // 删除所有指定类型的位置点
                mapLocationRepository.deleteAll(locations);
                logger.info("已删除 {} 个类型为 {} 的位置点", locations.size(), type);
            }
        }

        logger.info("删除特定类型的兴趣点完成");
    }
} 