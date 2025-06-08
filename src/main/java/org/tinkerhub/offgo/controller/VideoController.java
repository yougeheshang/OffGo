package org.tinkerhub.offgo.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tinkerhub.offgo.entity.Video;
import org.tinkerhub.offgo.service.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*")
public class VideoController {
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateVideo(
            @RequestParam(value = "title", required = true) String title,
            @RequestParam(value = "description", required = true) String description,
            @RequestParam(value = "style", required = true) String style,
            @RequestParam(value = "userId", required = true) Long userId,
            @RequestParam(value = "files", required = true) List<MultipartFile> files
    ) {
        try {
            logger.info("Received video generation request - title: {}, description: {}, style: {}, userId: {}, files: {}", 
                title, description, style, userId, files.size());

            // 验证输入
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "标题不能为空"));
            }
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "描述不能为空"));
            }
            if (style == null || style.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "风格不能为空"));
            }
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "用户ID不能为空"));
            }
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "请上传文件"));
                }

            logger.info("开始生成视频 - 用户ID: {}, 标题: {}, 文件数量: {}", userId, title, files.size());
            String videoUrl = videoService.generateVideo(title, description, style, userId, files);
            
            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("videoUrl", videoUrl);
            
            logger.info("视频生成成功 - URL: {}", videoUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("视频生成失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    @PostMapping("/saveVideo")
    public ResponseEntity<Video> uploadVideo(@RequestParam("video") MultipartFile file) {
        try {
            Video video = videoService.saveVideo(file);
            return ResponseEntity.ok(video);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVideo(@PathVariable Long id) {
        try {
            System.out.println("Getting video with id: " + id);
            
            Video video = videoService.getVideo(id);
            System.out.println("Found video: " + video.getVideoPath());
            
            // 如果是远程URL，直接返回URL
            if (video.getVideoPath().startsWith("http")) {
                return ResponseEntity.ok(Map.of(
                    "url", video.getVideoPath(),
                    "type", "remote"
                ));
            }
            
            // 如果是本地文件，返回文件流
            Path videoPath = videoService.getVideoPath(id);
            if (videoPath == null) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(videoPath.toUri());
            if (!resource.exists()) {
                System.out.println("Video file not found at path: " + videoPath);
                return ResponseEntity.notFound().build();
            }

            String contentType = video.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "video/mp4";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + video.getOriginalName() + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        } catch (Exception e) {
            System.out.println("Error getting video: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 