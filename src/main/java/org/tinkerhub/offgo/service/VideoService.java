package org.tinkerhub.offgo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tinkerhub.offgo.entity.Video;
import org.tinkerhub.offgo.repository.VideoRepository;
import org.tinkerhub.offgo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.tinkerhub.offgo.entity.User;
import org.tinkerhub.offgo.service.ImgBBService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.List;

@Service
public class VideoService {
    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);
    
    @Value("${video.upload.dir}")
    private String uploadDir;

    @Value("${baidu.ai.api.key}")
    private String apiKey;

    @Value("${baidu.ai.secret.key}")
    private String secretKey;

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final ImgBBService imgBBService;

    private static final String API_KEY = "NZL7eFSQny8GKVIpfLTdTA9W";
    private static final String SECRET_KEY = "PvHkRLm6A4UXGmgdj1isvMp4aGE3pWdj";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder()
            .readTimeout(300, TimeUnit.SECONDS)
            .build();

    @Autowired
    public VideoService(VideoRepository videoRepository, UserRepository userRepository, UserService userService, ImgBBService imgBBService) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.imgBBService = imgBBService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String generateVideo(String title, String description, String style, Long userId, List<MultipartFile> files) {
        try {
            // 调用百度AI API生成视频
            String videoUrl = callBaiduAIApi(title, description, style, files, userId);
            
            // 保存视频记录
            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setVideoPath(videoUrl);
            video.setType("remote");
            video.setContentType("video/mp4");
            video.setMimeType("video/mp4");
            video.setFileSize(0L); // 设置默认文件大小
            video = videoRepository.save(video);
            
            // 更新用户的视频URL
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            user.setAiVideoUrl(videoUrl);
            userRepository.save(user);
            
            // 移除重复的成功日志，只保留在Controller中记录
            return videoUrl;
        } catch (Exception e) {
            logger.error("视频生成失败", e);
            throw new RuntimeException("视频生成失败: " + e.getMessage());
        }
    }

    private String callBaiduAIApi(String title, String description, String style, List<MultipartFile> files, Long userId) throws IOException {
        logger.info("Starting video generation process for user: {}", userId);
        
        try {
        // 创建上传目录
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成唯一文件名
        String fileName = UUID.randomUUID().toString() + ".mp4";
        Path filePath = uploadPath.resolve(fileName);

        // 保存上传的文件
        MultipartFile firstFile = null;
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                Files.copy(file.getInputStream(), filePath);
                firstFile = file;
                break; // 只保存第一个文件
            }
        }

        if (firstFile == null) {
            throw new IllegalArgumentException("No valid file provided");
        }

            // 调用百度AI视频生成API
            String videoUrl = generateVideoWithBaiduAI(filePath, style, description, firstFile, userId);

            // 创建视频记录
            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setStyle(style);
            video.setOriginalName(firstFile.getOriginalFilename());
            video.setVideoPath(videoUrl);
            video.setContentType("video/mp4");
            video.setMimeType("video/mp4");
            video.setFileSize(firstFile.getSize());
            video.setType("remote");

            // 保存到数据库
            video = videoRepository.save(video);
            
            // 更新用户的视频URL
            userService.updateUserVideoURL(userId, videoUrl);
            
            return videoUrl;
        } catch (Exception e) {
            logger.error("Video generation failed", e);
            // 创建一个基本的视频记录，表示请求已提交
            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setStyle(style);
            video.setVideoPath("pending");
            video.setContentType("video/mp4");
            video.setMimeType("video/mp4");
            video.setFileSize(0L); // 设置默认文件大小
            video.setType("remote");
            return videoRepository.save(video).getVideoPath();
        }
    }

    private String generateVideoWithBaiduAI(Path inputFile, String style, String description, MultipartFile firstFile, Long userId) throws IOException {
        logger.info("Calling Baidu AI API for video generation");
        
        // 验证并调整描述长度
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("描述不能为空");
        }
        description = description.trim();
        if (description.length() < 20) {
            throw new IllegalArgumentException("描述长度不能少于20个字符");
        }
        if (description.length() > 800) {
            description = description.substring(0, 800);
        }
        
        // 获取access token
        String accessToken = getAccessToken();
        logger.info("Got access token: {}", accessToken);

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        JSONObject source = new JSONObject();
        JSONObject config = new JSONObject();

        // 添加文本描述
        JSONObject textStruct = new JSONObject();
        textStruct.put("type", "text");
        textStruct.put("text", description);

        // 添加图片
        JSONObject imageStruct = new JSONObject();
        imageStruct.put("type", "image");
        JSONObject mediaSource = new JSONObject();
        String imageUrl = uploadImageToBaidu(firstFile);
        mediaSource.put("type", 3);
        mediaSource.put("url", imageUrl);
        imageStruct.put("mediaSource", mediaSource);

        // 设置配置
        config.put("productType", "video");
        config.put("resolution", new int[]{1920, 1080}); // 使用最高分辨率
        config.put("ttsPer", 4144); // 使用度姗姗-女音色
        
        // 添加字幕设置
        JSONObject caption = new JSONObject();
        caption.put("marginBottom", 70);
        caption.put("fontColor", "ffffff");
        caption.put("fontAlpha", 100);
        caption.put("bgColor", "927070");
        caption.put("bgAlpha", 32);
        config.put("caption", caption);
        
        // 根据风格选择模板
        String templateId = getTemplateIdByStyle(style);
        if (templateId != null) {
            config.put("templateId", templateId);
        }

        // 组装请求体
        source.put("structs", new Object[]{textStruct, imageStruct});
        requestBody.put("source", source);
        requestBody.put("config", config);

        // 发送请求
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, requestBody.toString());
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rpc/2.0/brain/creative/ttv/material?access_token=" + accessToken)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        Response response = HTTP_CLIENT.newCall(request).execute();
        String responseBody = response.body().string();
        
        // 处理响应
        JSONObject responseJson = new JSONObject(responseBody);
        
        if (responseJson.has("error_code")) {
            int errorCode = responseJson.getInt("error_code");
            String errorMsg = responseJson.has("error_msg") ? 
                responseJson.getString("error_msg") : "Unknown error";
                    throw new RuntimeException("Baidu AI API error: " + errorMsg);
        }
        
        JSONObject data = responseJson.getJSONObject("data");
        if (!data.has("jobId")) {
            throw new RuntimeException("No job ID in response");
        }
        
        Long jobId = data.getLong("jobId");
        logger.info("Got job ID: {} for user: {}", jobId, userId);
        
        return waitForVideoGeneration(String.valueOf(jobId), accessToken, userId);
    }

    private String waitForVideoGeneration(String jobId, String accessToken, Long userId) throws IOException {
        String queryUrl = String.format("https://aip.baidubce.com/rpc/2.0/brain/creative/ttv/query?access_token=%s", accessToken);
        
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("jobId", jobId);
        requestBody.put("includeTimeline", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

        int attempt = 0;
        int maxAttempts = 3;  // 最大重试次数
        int retryDelay = 10;  // 重试延迟（秒）
        
        logger.debug("开始轮询视频生成状态，jobId: {}, userId: {}", jobId, userId);
        
        while (attempt < maxAttempts) {
            attempt++;
            logger.debug("第 {} 次轮询视频生成状态 - jobId: {}, userId: {}", attempt, jobId, userId);
            
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    queryUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
                );

                JsonNode responseJson = objectMapper.readTree(response.getBody());
                logger.debug("轮询响应: {}", responseJson.toString());
                
                // 检查是否有错误码
                if (responseJson.has("error_code")) {
                    int errorCode = responseJson.get("error_code").asInt();
                    String errorMsg = responseJson.get("error_msg").asText();
                    
                    // 如果是服务器内部错误，尝试重试
                    if (errorCode == 282000) {
                        if (attempt < maxAttempts) {
                            logger.warn("服务器内部错误，准备第 {} 次重试 - jobId: {}, error: {}", attempt + 1, jobId, errorMsg);
                            Thread.sleep(retryDelay * 1000);
                            continue;
                        }
                    }
                    
                    logger.error("查询任务状态失败 - jobId: {}, error: {}", jobId, errorMsg);
                    throw new RuntimeException("查询任务状态失败: " + errorMsg);
                }

                JsonNode data = responseJson.get("data");
                String statusCode = data.get("statusCode").asText();
                logger.debug("当前任务状态码: {} - jobId: {}", statusCode, jobId);
                
                if ("1".equals(statusCode)) {
                    // 任务完成，获取视频URL
                    String videoUrl = data.get("videoAddr").asText();
                    logger.debug("视频生成成功！URL: {}, jobId: {}, userId: {}", videoUrl, jobId, userId);
                    // 更新用户的视频URL
                    userService.updateUserVideoURL(userId, videoUrl);
                    return videoUrl;
                } else if ("3".equals(statusCode)) {
                    // 任务执行中，继续等待
                    logger.debug("视频生成中，等待{}秒后继续查询... - jobId: {}, userId: {}", retryDelay, jobId, userId);
                    Thread.sleep(retryDelay * 1000);
                } else {
                    // 任务失败
                    String failReason = data.has("failReason") ? data.get("failReason").asText() : "未知原因";
                    logger.error("视频生成失败: {} - jobId: {}, userId: {}", failReason, jobId, userId);
                    throw new RuntimeException("视频生成失败: " + failReason);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("等待视频生成被中断 - jobId: {}, userId: {}", jobId, userId, e);
                throw new RuntimeException("等待视频生成被中断");
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    logger.warn("查询失败，准备第 {} 次重试 - jobId: {}, error: {}", attempt + 1, jobId, e.getMessage());
                    try {
                        Thread.sleep(retryDelay * 1000);
                        continue;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试等待被中断", ie);
                    }
                }
                logger.error("视频生成失败 - jobId: {}, userId: {}", jobId, userId, e);
                throw new RuntimeException("视频生成失败: " + e.getMessage());
            }
        }
        
        // 如果达到最大重试次数仍然失败
        logger.error("达到最大重试次数，视频生成失败 - jobId: {}, userId: {}", jobId, userId);
        throw new RuntimeException("视频生成失败：达到最大重试次数");
    }

    private String getAccessToken() throws IOException {
        logger.info("Getting Baidu AI access token");
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type=client_credentials&client_id=" + API_KEY
                + "&client_secret=" + SECRET_KEY);
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        return new JSONObject(response.body().string()).getString("access_token");
    }

    private String uploadImageToBaidu(MultipartFile file) throws IOException {
        try {
            // 使用 ImgBB 上传图片
            String imageUrl = imgBBService.uploadImage(file);
            logger.info("Image uploaded successfully to ImgBB, URL: {}", imageUrl);
            return imageUrl;
        } catch (Exception e) {
            logger.error("Failed to upload image to ImgBB", e);
            throw new IOException("Failed to upload image: " + e.getMessage());
        }
    }

    private String getTemplateIdByStyle(String style) {
        // 根据风格返回对应的模板ID
        switch (style.toLowerCase()) {
            case "travel":
                return "30"; // 旅行风格模板
            case "landscape":
                return "41"; // 风景风格模板
            case "cultural":
                return "42"; // 人文风格模板
            case "food":
                return "43"; // 美食风格模板
            default:
                return "30"; // 默认使用旅行风格模板
        }
    }

    public Video saveVideo(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file");
        }

        System.out.println("Upload directory: " + uploadDir);

        // 确保上传目录存在
        if (!Files.exists(Paths.get(uploadDir))) {
            System.out.println("Creating upload directory: " + Paths.get(uploadDir));
            Files.createDirectories(Paths.get(uploadDir));
        }

        // 生成唯一的文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID().toString() + extension;
        System.out.println("Generated filename: " + filename);

        // 保存文件
        Path targetLocation = Paths.get(uploadDir).resolve(filename);
        System.out.println("Target location: " + targetLocation);
        Files.copy(file.getInputStream(), targetLocation);

        // 保存视频信息到数据库
        Video video = new Video();
        video.setVideoPath(filename);  // 只存储文件名
        video.setContentType(file.getContentType());
        video.setFileSize(file.getSize());
        video.setMimeType(file.getContentType());
        video.setOriginalName(originalFilename);

        return videoRepository.save(video);
    }

    public Video getVideo(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
    }

    public Path getVideoPath(Long id) {
        Video video = getVideo(id);
        String videoPath = video.getVideoPath();
        
        // 如果是完整的URL，返回null，让控制器直接返回URL
        if (videoPath.startsWith("http")) {
            return null;
        }
        
        // 如果是本地文件路径，返回Path对象
        return Paths.get(uploadDir, videoPath);
    }

    public void deleteById(Long id) {
        videoRepository.deleteById(id);
    }
} 