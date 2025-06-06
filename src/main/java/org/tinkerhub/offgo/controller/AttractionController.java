package org.tinkerhub.offgo.controller;

import org.tinkerhub.offgo.service.AttractionService;
import org.tinkerhub.offgo.entity.Attraction;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/attraction")
public class AttractionController {
    private static final Logger logger = LoggerFactory.getLogger(AttractionController.class);

    @Autowired
    private AttractionService attractionService;

    /**
     * API接口：生成景点数据并保存到数据库（每次调用都会覆盖现有数据）
     */
    @GetMapping("/generateCards")
    public String generateAttractions() {
        return attractionService.generateAttractions();
    }
    /**
     * API接口：获取景点卡片数据
     * 支持根据关键词、排序字段和排序方式进行筛选
     */
    @GetMapping("/getCards")
    public Map<String, Object> getAttractionCards(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "name") String sortField,
        @RequestParam(defaultValue = "asc") String sortOrder) {

        List<Attraction> items = attractionService.getAttractionCards(keyword, sortField, sortOrder);
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        return response;
    }

    /**
     * API接口：更新景点点击热度
     */
    @PostMapping("/click")
    public ResponseEntity<?> updateAttractionHot(@RequestBody Map<String, Long> request) {
        Long id = request.get("id");
        if (id == null) {
            return ResponseEntity.badRequest().body("Missing attraction ID");
        }
        
        try {
            attractionService.incrementHot(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update attraction hot: " + e.getMessage());
        }
    }

    /**
     * API接口：检查用户是否已对景点评分
     */
    @GetMapping("/{attractionId}/checkRating")
    public ResponseEntity<Map<String, Object>> checkRating(
            @PathVariable Long attractionId,
            @RequestParam(required = false) Integer userId) {
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null) {
            result.put("hasRated", false);
            result.put("rating", 0);
            return ResponseEntity.ok(result);
        }

        try {
            String existingRating = attractionService.checkRating(userId, attractionId);
            if (!existingRating.equals("False")) {
                result.put("hasRated", true);
                result.put("rating", Integer.parseInt(existingRating));
            } else {
                result.put("hasRated", false);
                result.put("rating", 0);
            }
        } catch (Exception e) {
            result.put("hasRated", false);
            result.put("rating", 0);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * API接口：对景点进行评分
     */
    @PostMapping("/rate")
    public ResponseEntity<?> rateAttraction(@RequestBody Map<String, Object> request) {
        logger.info("收到评分请求: {}", request);
        try {
            if (request == null) {
                logger.error("请求数据为空");
                return ResponseEntity.badRequest().body("请求数据不能为空");
            }

            // 验证必要的参数
            if (!request.containsKey("attractionId") || !request.containsKey("userId") || !request.containsKey("rating")) {
                logger.error("缺少必要参数。请求包含: {}", request.keySet());
                return ResponseEntity.badRequest().body("缺少必要的参数");
            }

            Long attractionId = Long.valueOf(request.get("attractionId").toString());
            Integer userId = Integer.valueOf(request.get("userId").toString());
            Integer rating = Integer.valueOf(request.get("rating").toString());
            logger.info("处理评分 - 景点ID: {}, 用户ID: {}, 评分: {}", attractionId, userId, rating);

            // 验证评分范围
            if (rating < 1 || rating > 5) {
                logger.error("无效的评分值: {}", rating);
                return ResponseEntity.badRequest().body("评分必须在1到5之间");
            }

            // 进行评分
            Attraction attraction = attractionService.rateAttraction(attractionId, userId, rating);
            logger.info("更新景点评分。新评分: {}", attraction.getRating());

            Map<String, Object> response = new HashMap<>();
            response.put("newRating", attraction.getRating());
            logger.info("发送响应: {}", response);
            return ResponseEntity.ok(response);
        } catch (ClassCastException e) {
            logger.error("类型转换错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body("参数类型错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("意外错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("评分失败: " + e.getMessage());
        }
    }
}
