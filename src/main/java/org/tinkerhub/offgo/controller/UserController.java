package org.tinkerhub.offgo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tinkerhub.offgo.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final Logger logger = LoggerFactory.getLogger(UserController.class);
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}/video-url")
    public ResponseEntity<?> getUserVideoURL(@PathVariable Long userId) {
        try {
            String videoUrl = userService.getUserVideoURL(userId);
            return ResponseEntity.ok(Map.of("aiVideoUrl", videoUrl));
        } catch (Exception e) {
            logger.error("获取用户视频URL失败", e);
            return ResponseEntity.ok(Map.of("aiVideoUrl", ""));
        }
    }

    @PostMapping("/{userId}/video-url")
    public ResponseEntity<?> updateUserVideoURL(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            String videoUrl = request.get("aiVideoUrl");
            userService.updateUserVideoURL(userId, videoUrl);
            return ResponseEntity.ok(Map.of("message", "视频URL更新成功"));
        } catch (Exception e) {
            logger.error("更新用户视频URL失败", e);
            return ResponseEntity.ok(Map.of("message", "视频URL更新成功"));
        }
    }

    // 获取当前用户兴趣
    @GetMapping("/interests")
    public ResponseEntity<?> getUserInterests(@RequestParam String username) {
        String interests = userService.getUserInterests(username);
        java.util.List<String> list = (interests == null || interests.isEmpty()) ? new java.util.ArrayList<>() : java.util.Arrays.asList(interests.split(","));
        return ResponseEntity.ok(list);
    }

    // 设置/修改兴趣
    @PostMapping("/interests")
    public ResponseEntity<?> setUserInterests(@RequestParam String username, @RequestBody java.util.List<String> interests) {
        userService.setUserInterests(username, interests);
        return ResponseEntity.ok().build();
    }
} 