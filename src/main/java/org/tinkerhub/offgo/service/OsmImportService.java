package org.tinkerhub.offgo.service;

import org.tinkerhub.offgo.entity.MapLocation;
import org.tinkerhub.offgo.entity.MapRoad;
import org.tinkerhub.offgo.util.OsmParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.List;

@Service
public class OsmImportService {
    private static final Logger logger = LoggerFactory.getLogger(OsmImportService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public void importOsmData(String osmFilePath) throws ParserConfigurationException, IOException, SAXException {
        try {
            logger.info("Starting OSM data import process");
            
            // 清空现有数据
            logger.info("Clearing existing data from tables");
            jdbcTemplate.execute("DELETE FROM map_road");
            jdbcTemplate.execute("DELETE FROM map_location");
            logger.info("Existing data cleared successfully");

            // 解析OSM文件
            logger.info("Parsing OSM file: {}", osmFilePath);
            OsmParser parser = new OsmParser();
            parser.parseOsmFile(osmFilePath);
            logger.info("OSM file parsed successfully");

            // 导入位置数据
            List<MapLocation> locations = parser.getLocations();
            logger.info("Found {} locations to import", locations.size());
            
            for (MapLocation location : locations) {
                try {
                    jdbcTemplate.update(
                        "INSERT INTO map_location (id, name, description, latitude, longitude, type) VALUES (?, ?, ?, ?, ?, ?)",
                        location.getId(),
                        location.getName(),
                        location.getDescription(),
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getType()
                    );
                } catch (Exception e) {
                    logger.error("Error inserting location: {}", location, e);
                    throw e;
                }
            }
            logger.info("Location data imported successfully");

            // 导入道路数据
            List<MapRoad> roads = parser.getRoads();
            logger.info("Found {} roads to import", roads.size());
            
            for (MapRoad road : roads) {
                try {
                    if (road.getRoadType() == null) {
                        jdbcTemplate.update(
                            "INSERT INTO map_road (id, name, road_type, start_point_id, end_point_id, path_points) VALUES (?, ?, ?, ?, ?, ?)",
                            road.getId(),
                            road.getName(),
                            road.getRoadType(),
                            road.getStartPointId(),
                            road.getEndPointId(),
                            road.getPathPoints()
                        );
                    }
                } catch (Exception e) {
                    logger.error("Error inserting road: {}", road, e);
                    throw e;
                }
            }
            logger.info("Road data imported successfully");
            
        } catch (Exception e) {
            logger.error("Error during OSM data import", e);
            throw e;
        }
    }
} 