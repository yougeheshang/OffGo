package org.tinkerhub.offgo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tinkerhub.offgo.entity.MapLocation;
import org.tinkerhub.offgo.repository.MapLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.tinkerhub.offgo.entity.MapRoad;
import org.tinkerhub.offgo.repository.MapRoadRepository;

import java.util.List;
import java.util.Random;

@Service
public class CrowdLevelService {
    private static final Logger logger = LoggerFactory.getLogger(CrowdLevelService.class);

    @Autowired
    private MapLocationRepository mapLocationRepository;

    @Autowired
    private MapRoadRepository mapRoadRepository;

    @Autowired
    private LocationClassificationService locationClassificationService;

    private final Random random = new Random();

    // 在应用启动时初始化
    @Bean
    public CommandLineRunner init() {
        return args -> {
            logger.info("应用启动，开始初始化...");
            // 先进行位置点分类
            locationClassificationService.classifyLocations();
            // 然后初始化拥挤度
            initializeCrowdLevels();
            // 初始化道路拥挤度
            initializeRoadCrowdLevels();
        };
    }

    // 初始化所有有名称的位置点的拥挤度
    @Transactional
    public void initializeCrowdLevels() {
        logger.info("开始初始化拥挤度...");
        List<MapLocation> locations = mapLocationRepository.findByNameIsNotNull();
        logger.info("找到 {} 个命名位置点", locations.size());

        for (MapLocation location : locations) {
            // 随机初始化拥挤度（0-2）
            int crowdLevel = random.nextInt(3);
            location.setCrowdLevel(crowdLevel);
            logger.info("位置点 {} 初始化拥挤度为: {}", location.getName(), crowdLevel);
        }

        mapLocationRepository.saveAll(locations);
        logger.info("拥挤度初始化完成");
    }

    // 修改 updateCrowdLevels 方法，增加道路拥挤度的刷新逻辑
    @Transactional
    public void updateCrowdLevels() {
        logger.info("开始更新拥挤度...");
        List<MapLocation> locations = mapLocationRepository.findByNameIsNotNull();
        logger.info("找到 {} 个命名位置点", locations.size());

        for (MapLocation location : locations) {
            int currentLevel = location.getCrowdLevel();
            // 随机决定是否改变拥挤度（50%概率）
            if (random.nextBoolean()) {
                // 只允许变化1个等级
                int change = random.nextBoolean() ? 1 : -1;
                int newLevel = currentLevel + change;
                
                // 确保新等级在有效范围内（0-2）
                newLevel = Math.max(0, Math.min(2, newLevel));
                
                // 只有当变化后的等级与当前等级不同时才更新
                if (newLevel != currentLevel) {
                    location.setCrowdLevel(newLevel);
                    logger.info("位置点 {} 拥挤度从 {} 更新为 {}", 
                        location.getName(), currentLevel, newLevel);
                }
            } else {
                logger.info("位置点 {} 拥挤度保持不变: {}", location.getName(), currentLevel);
            }
        }

        mapLocationRepository.saveAll(locations);
        logger.info("拥挤度更新完成");

        // 更新道路拥挤度
        List<MapRoad> roads = mapRoadRepository.findAll();
        logger.info("找到 {} 条道路", roads.size());

        for (MapRoad road : roads) {
            double currentLevel = road.getCrowdLevel();
            // 每次都会改变拥挤度
            // 随机变化0.5（更大的变化幅度）
            double change = random.nextDouble() * 1.0 - 0.5;
            double newLevel = currentLevel + change;
            
            // 确保新等级在有效范围内（0-1），0表示最拥挤，1表示最不拥挤
            newLevel = Math.max(0, Math.min(1, newLevel));
            
            road.setCrowdLevel(newLevel);
            logger.info("道路 {} 拥挤度从 {} 更新为 {}", 
                road.getName(), currentLevel, newLevel);
        }

        mapRoadRepository.saveAll(roads);
        logger.info("道路拥挤度更新完成");
    }

    // 初始化道路拥挤度的方法
    @Transactional
    public void initializeRoadCrowdLevels() {
        logger.info("开始初始化道路拥挤度...");
        List<MapRoad> roads = mapRoadRepository.findAll();
        logger.info("找到 {} 条道路", roads.size());

        for (MapRoad road : roads) {
            // 随机初始化拥挤度（0-1），0表示最拥挤，1表示最不拥挤
            double crowdLevel = random.nextDouble();
            road.setCrowdLevel(crowdLevel);
            logger.info("道路 {} 初始化拥挤度为: {}", road.getName(), crowdLevel);
        }

        mapRoadRepository.saveAll(roads);
        logger.info("道路拥挤度初始化完成");
    }
} 