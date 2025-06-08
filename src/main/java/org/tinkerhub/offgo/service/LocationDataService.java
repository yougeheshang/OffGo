package org.tinkerhub.offgo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tinkerhub.offgo.entity.MapLocation;
import org.tinkerhub.offgo.repository.MapLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

@Service
public class LocationDataService {
    private static final Logger logger = LoggerFactory.getLogger(LocationDataService.class);
    private static final String[] COLORS = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEEAD"};
    private static final Random random = new Random();

    @Autowired
    private MapLocationRepository locationRepository;

    private String generateVectorIcon(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "未知";  // 使用默认值
        }
        String firstChar = name.substring(0, 1).toUpperCase();
        String color = COLORS[random.nextInt(COLORS.length)];
        
        return String.format(
            "<svg viewBox=\"0 0 100 100\" width=\"100\" height=\"100\">" +
            "<circle cx=\"50\" cy=\"50\" r=\"45\" fill=\"%s\" opacity=\"0.2\"/>" +
            "<text x=\"50\" y=\"65\" font-size=\"50\" text-anchor=\"middle\" fill=\"%s\">%s</text>" +
            "</svg>",
            color, color, firstChar
        );
    }

    private String generateDescription(String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            name = "未知地点";  // 使用默认值
        }
        if (type == null || type.trim().isEmpty()) {
            type = "景点";  // 使用默认值
        }

        String[] templates = {
            "%s是一个%s，这里环境优美，是游客们喜爱的景点之一。",
            "来到%s，您可以感受到独特的%s氛围，这里值得一游。",
            "%s作为%s，展现了独特的魅力，是必访之地。",
            "在%s，您可以体验到%s的独特魅力，这里风景宜人。"
        };
        
        return String.format(
            templates[random.nextInt(templates.length)],
            name,
            type
        );
    }

    @Transactional
    public void updateLocationData() {
        List<MapLocation> locations = locationRepository.findAll();
        
        for (MapLocation location : locations) {
            try {
                boolean needsUpdate = false;
                
                // 检查并设置默认值
                if (location.getName() == null || location.getName().trim().isEmpty()) {
                    location.setName("未知地点");
                    needsUpdate = true;
                }
                
                if (location.getDescription() == null || location.getDescription().trim().isEmpty()) {
                    location.setDescription(generateDescription(location.getName(), location.getType()));
                    needsUpdate = true;
                }
                
                if (location.getVectorIcon() == null || location.getVectorIcon().trim().isEmpty()) {
                    location.setVectorIcon(generateVectorIcon(location.getName()));
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    locationRepository.save(location);
                    logger.info("Updated location data for: {}", location.getName());
                }
            } catch (Exception e) {
                logger.error("Error updating location: {}", location.getId(), e);
                // 继续处理下一个位置
            }
        }
    }

    @Transactional
    public void updateSingleLocation(Long locationId) {
        MapLocation location = locationRepository.findById(locationId)
            .orElseThrow(() -> new RuntimeException("Location not found"));
            
        try {
            // 检查并设置默认值
            if (location.getName() == null || location.getName().trim().isEmpty()) {
                location.setName("未知地点");
            }
            
            if (location.getType() == null || location.getType().trim().isEmpty()) {
                location.setType("景点");
            }
            
            if (location.getDescription() == null || location.getDescription().trim().isEmpty()) {
                location.setDescription(generateDescription(location.getName(), location.getType()));
            }
            
            if (location.getVectorIcon() == null || location.getVectorIcon().trim().isEmpty()) {
                location.setVectorIcon(generateVectorIcon(location.getName()));
            }
            
            locationRepository.save(location);
            logger.info("Updated location data for: {}", location.getName());
        } catch (Exception e) {
            logger.error("Error updating location: {}", locationId, e);
            throw new RuntimeException("Failed to update location: " + e.getMessage());
        }
    }

    @Transactional
    public void updateUnknownLocationsToRoadNodes() {
        // 将所有名称为“道路节点”的兴趣点的类型改为空
        List<MapLocation> roadNodeLocations = locationRepository.findByName("道路节点");
        for (MapLocation location : roadNodeLocations) {
            location.setType("");  // 类型设为空
            locationRepository.save(location);
        }
        logger.info("Updated {} locations named '道路节点' to have empty type", roadNodeLocations.size());
    }
} 