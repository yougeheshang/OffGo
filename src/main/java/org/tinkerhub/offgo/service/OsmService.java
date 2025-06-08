package org.tinkerhub.offgo.service;

import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public class OsmService {

    private static final Logger logger = LoggerFactory.getLogger(OsmService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<Map<String, Object>> findNearbyServices(double latitude, double longitude, double radius) {
        // 定义服务类型
        List<String> serviceTypes = Arrays.asList(
            "洗手间", "理发店", "快餐", "食堂", "药店", "咖啡厅",
            "超市", "自动贩卖机", "小卖铺", "商店", "花店"
        );

        logger.info("开始查询周边服务设施 - 位置: ({}, {}), 半径: {}米", latitude, longitude, radius);
        logger.info("服务类型: {}", serviceTypes);

        // 构建SQL查询（MySQL版本，使用Haversine公式计算距离）
        String sql = """
            SELECT 
                id,
                name,
                type,
                latitude,
                longitude,
                (
                    6371000 * 2 * ASIN(
                        SQRT(
                            POWER(SIN((latitude - :latitude) * PI() / 180 / 2), 2) +
                            COS(latitude * PI() / 180) * COS(:latitude * PI() / 180) *
                            POWER(SIN((longitude - :longitude) * PI() / 180 / 2), 2)
                        )
                    )
                ) AS distance
            FROM map_location
            WHERE type IN (:serviceTypes)
            HAVING distance <= :radius
            ORDER BY distance
        """;
        logger.info("SQL查询语句: {}", sql);
        // 设置参数
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("latitude", latitude)
            .addValue("longitude", longitude)
            .addValue("serviceTypes", serviceTypes)
            .addValue("radius", radius);
        logger.info("查询参数: {}", params.getValues());
        try {
            // 执行查询
            List<Map<String, Object>> results = namedParameterJdbcTemplate.queryForList(sql, params);
            logger.info("查询成功，找到 {} 个服务设施", results.size());
            for (Map<String, Object> service : results) {
                String type = (String) service.get("type");
                service.put("icon", generateServiceIcon(type));
            }
            return results;
        } catch (Exception e) {
            logger.error("使用命名参数查询失败，错误信息: {}", e.getMessage());
            logger.error("尝试使用传统方式查询");
            // 如果使用命名参数失败，尝试使用传统方式
            String placeholders = String.join(",", Collections.nCopies(serviceTypes.size(), "?"));
            String fallbackSql = """
                SELECT 
                    id,
                    name,
                    type,
                    latitude,
                    longitude,
                    (
                        6371000 * 2 * ASIN(
                            SQRT(
                                POWER(SIN((latitude - ?) * PI() / 180 / 2), 2) +
                                COS(latitude * PI() / 180) * COS(? * PI() / 180) *
                                POWER(SIN((longitude - ?) * PI() / 180 / 2), 2)
                            )
                        )
                    ) AS distance
                FROM map_location
                WHERE type IN (%s)
                HAVING distance <= ?
                ORDER BY distance
            """.formatted(placeholders);
            logger.info("备用SQL查询语句: {}", fallbackSql);
            logger.info("备用查询参数: latitude={}, longitude={}, serviceTypes={}, radius={}", latitude, longitude, serviceTypes, radius);
            try {
                // 构建参数数组，顺序：latitude, longitude, longitude, [types...], latitude, longitude, radius
                Object[] queryParams = new Object[3 + serviceTypes.size() + 1];
                int paramIndex = 0;
                queryParams[paramIndex++] = latitude;
                queryParams[paramIndex++] = longitude;
                queryParams[paramIndex++] = longitude;
                for (String type : serviceTypes) {
                    queryParams[paramIndex++] = type;
                }
                queryParams[paramIndex] = radius;
                List<Map<String, Object>> results = jdbcTemplate.queryForList(fallbackSql, queryParams);
                logger.info("备用查询成功，找到 {} 个服务设施", results.size());
            for (Map<String, Object> service : results) {
                String type = (String) service.get("type");
                service.put("icon", generateServiceIcon(type));
            }
            return results;
            } catch (Exception ex) {
                logger.error("备用查询也失败，错误信息: {}", ex.getMessage());
                throw ex;
            }
        }
    }

    private String generateServiceIcon(String type) {
        // 根据服务类型生成对应的SVG图标
        Map<String, String> iconMap = new HashMap<>();
        iconMap.put("洗手间", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#4CAF50'/></svg>");
        iconMap.put("理发店", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#2196F3'/></svg>");
        iconMap.put("快餐", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#FF9800'/></svg>");
        iconMap.put("食堂", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#F44336'/></svg>");
        iconMap.put("药店", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#9C27B0'/></svg>");
        iconMap.put("咖啡厅", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#795548'/></svg>");
        iconMap.put("超市", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#E91E63'/></svg>");
        iconMap.put("自动贩卖机", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#607D8B'/></svg>");
        iconMap.put("小卖铺", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#FFC107'/></svg>");
        iconMap.put("商店", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#00BCD4'/></svg>");
        iconMap.put("花店", "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#8BC34A'/></svg>");

        return iconMap.getOrDefault(type, "<svg viewBox='0 0 24 24'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z' fill='#9E9E9E'/></svg>");
    }
} 