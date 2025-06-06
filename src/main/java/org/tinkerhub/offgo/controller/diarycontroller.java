package org.tinkerhub.offgo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tinkerhub.offgo.entity.*;
import org.tinkerhub.offgo.service.DiaryService;
import org.tinkerhub.offgo.service.VideoService;
import org.tinkerhub.offgo.repository.DiaryRepository;
import org.tinkerhub.offgo.repository.ContentRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.tinkerhub.offgo.service.UserService;
import java.io.*;
import java.util.*;
import java.util.Base64;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {
    private static final Logger logger = LoggerFactory.getLogger(DiaryController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private VideoService videoService;
    @Autowired
    private DiaryService diaryService;
    @Autowired
    private DiaryRepository diaryRepository;
    @Autowired
    private ContentRepository contentRepository;

    @GetMapping("/health")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> healthCheck() {
        logger.info("健康检查请求");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", new Date().toString()
        ));
    }

    private static final String IMAGE_STORAGE_DIR = "src/main/resources/static/images/diary/";
    private static final String DEFAULT_IMAGE_PATH = "src/main/resources/static/images/default-image.jpg";

    @PostMapping("/savediary_withoutimage")
    public ResponseEntity<?> savediary_without(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("content") String content,
            @RequestParam("userID") Integer userID,
            @RequestParam("destination") String destination,
            @RequestParam(value = "video_id", required = false) String video_id,
            @RequestParam(value = "tags", required = false) String tags
    ) {
        try {
            // 1. 先保存内容
            ContentEntity contentEntity = new ContentEntity();
            contentEntity.setContent(content);
            contentEntity = contentRepository.save(contentEntity);
            if (contentEntity == null || contentEntity.getId() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("保存内容失败");
            }

            // 2. 创建并保存日记
            Diary diary = new Diary();
            diary.setTitle(title);
            diary.setDescription(description);
            diary.setContentId(contentEntity.getId());  // 使用保存后的内容ID
            diary.setUserID(userID);
            diary.setDestination(destination);
            diary.setHot(0);
            diary.setRate_counts(0);
            diary.setRate_sum(0);
            diary.setRating(0.0);
            if (tags != null && !tags.isEmpty()) {
                diary.setTags(tags);
            }

            // 3. 处理视频ID（如果有）
            if (video_id != null && !video_id.isEmpty()) {
                try {
                    Long videoId = Long.parseLong(video_id);
                    Video video = videoService.getVideo(videoId);
                    if (video == null) {
                        throw new RuntimeException("Video not found with id: " + videoId);
                    }
                    diary.setVideoId(videoId);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid video_id format: " + video_id);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to verify video existence: " + e.getMessage());
                }
            }

            diary = diaryRepository.save(diary);
            return ResponseEntity.ok(diary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("保存日记失败: " + e.getMessage());
        }
    }

    @RequestMapping("/savediary_withimage")
    @Transactional
    public boolean savediary_with(@RequestParam String title, @RequestParam String description, @RequestParam String content, @RequestParam int userID, @RequestParam int image_num, @RequestParam String destination, @RequestParam(required = false) String video_id, @RequestParam(value = "tags", required = false) String tags) {
        try {
            // 1. 先保存内容
            ContentEntity contentEntity = new ContentEntity();
            contentEntity.setContent(content);
            contentEntity = diaryService.saveContent(contentEntity);
            if (contentEntity == null || contentEntity.getId() == null) {
                throw new RuntimeException("Failed to save content");
            }
            
            // 2. 准备图片ID数组
            int diaryID = diaryService.findImageMaxId() + 1;
            int imageID = diaryService.findImageMaxId() + 1;
        int[] arr = new int[image_num];
        imageID -= image_num;
        for (int i = 0; i < image_num; i++) {
            arr[i] = imageID;
            imageID++;
        }
            
            // 3. 创建并保存日记
        Diary diary = new Diary();
        diary.setId(diaryID);
        diary.setTitle(title);
        diary.setUserID(userID);
        diary.setDescription(description);
            diary.setContentId(contentEntity.getId());  // 使用保存后的 contentEntity 的 ID
            diary.setImage(Arrays.stream(arr).mapToObj(String::valueOf).collect(Collectors.joining(",")));
        diary.setDestination(destination);
            diary.setHot(0);
            diary.setRate_counts(0);
            diary.setRate_sum(0);
            diary.setRating(0.0);
            if (tags != null && !tags.isEmpty()) {
                diary.setTags(tags);
            }
            
            // 4. 处理视频ID（如果有）
            if (video_id != null && !video_id.isEmpty()) {
                try {
                    Long videoId = Long.parseLong(video_id);
                    Video video = videoService.getVideo(videoId);
                    if (video == null) {
                        throw new RuntimeException("Video not found with id: " + videoId);
                    }
                    diary.setVideoId(videoId);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid video_id format: " + video_id);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to verify video existence: " + e.getMessage());
                }
            }
            
            // 5. 保存日记
        diaryService.saveDiary(diary);
        return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save diary: " + e.getMessage());
        }
    }

    @RequestMapping("/getdiary/{ID}")
    public Diary getdiary(@PathVariable int ID) {
        return diaryService.findById(ID);
    }

    @RequestMapping("/getimage/{ID}")
    public ResponseEntity<byte[]> getimage(@PathVariable int ID) {
        try {
            logger.info("Attempting to get image with ID: {}", ID);
            
            ImageEntity imageEntity = diaryService.findImageById(ID);
            if (imageEntity == null) {
                logger.warn("Image not found in database for ID: {}", ID);
                return getDefaultImage();
            }
            logger.info("Found image entity in database: {}", imageEntity.getImagepath());

            String imagePath = imageEntity.getImagepath();
            File file = new File(imagePath);
            logger.info("Looking for image file at: {}", file.getAbsolutePath());
            
            if (!file.exists()) {
                logger.warn("Image file not found at path: {}", imagePath);
                return getDefaultImage();
            }
            
            if (!file.canRead()) {
                logger.error("Cannot read image file at path: {}", imagePath);
                return getDefaultImage();
            }

            logger.info("Successfully found and can read image file");
            byte[] buffer = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(buffer);
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(buffer);
                
        } catch (FileNotFoundException e) {
            logger.error("File not found for image ID {}: {}", ID, e.getMessage());
            return getDefaultImage();
        } catch (IOException e) {
            logger.error("Error reading image file for ID {}: {}", ID, e.getMessage());
            return getDefaultImage();
        } catch (Exception e) {
            logger.error("Unexpected error for image ID {}: {}", ID, e.getMessage(), e);
            return getDefaultImage();
        }
    }

    private ResponseEntity<byte[]> getDefaultImage() {
        try {
            logger.info("Attempting to get default image from: {}", DEFAULT_IMAGE_PATH);
            File defaultImage = new File(DEFAULT_IMAGE_PATH);
            if (!defaultImage.exists()) {
                logger.error("Default image not found at: {}", DEFAULT_IMAGE_PATH);
                return ResponseEntity.notFound().build();
            }
            
            byte[] buffer = new byte[(int) defaultImage.length()];
            try (FileInputStream fis = new FileInputStream(defaultImage)) {
                fis.read(buffer);
            }
            
            logger.info("Successfully loaded default image");
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(buffer);
        } catch (IOException e) {
            logger.error("Error reading default image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping("/getcontent/{ID}")
    public ContentEntity getcontent(@PathVariable int ID) {
        return diaryService.findContentById(ID);
    }

    @RequestMapping("/saveImage")
    public int saveImage(@RequestParam String imageData) {
        try {
            if (imageData == null || imageData.isEmpty()) {
                throw new RuntimeException("图片数据为空");
            }

            // 解码Base64数据
            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            int imageID = diaryService.findImageMaxId() + 1;
            String imagePath = IMAGE_STORAGE_DIR + imageID + ".jpg";
            
            // 确保目录存在
            File directory = new File(IMAGE_STORAGE_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(imagePath)) {
                fos.write(imageBytes);
            }

            // 保存到数据库
            ImageEntity imageEntity = new ImageEntity(imageID, imagePath);
            diaryService.saveImage(imageEntity);

            return imageID;
        } catch (Exception e) {
            throw new RuntimeException("保存图片失败: " + e.getMessage());
        }
    }

    @RequestMapping("/test_add")
    public String test_add() {
        int image_num = 1;
        String image_path = "src/main/resources/static/images/1.jpg";
        String title = "巴黎";
        String description = "巴黎";
        int userID = 1;
        String content = "实验内容";
        int contentID = diaryService.findContentMaxId() + 1;
        ContentEntity contentEntity = new ContentEntity(contentID, content);
        diaryService.saveContent(contentEntity);
        int diaryID = diaryService.findImageMaxId() + 1;
        int imageID = diaryService.findImageMaxId() + 1;
        for (int i = 0; i < image_num; i++) {
            ImageEntity imageEntity = new ImageEntity(imageID, image_path);
            diaryService.saveImage(imageEntity);
            imageID++;
        }
        int[] arr = new int[image_num];
        imageID -= image_num;
        for (int i = 0; i < image_num; i++) {
            arr[i] = imageID;
            imageID++;
        }
        Diary diary = new Diary();
        diary.setId(diaryID);
        diary.setTitle(title);
        diary.setUserID(userID);
        diary.setDescription(description);
        diary.setContentId(contentID);
        diary.setImage(Arrays.stream(arr).mapToObj(String::valueOf).collect(Collectors.joining(",")));
        diary.setDestination("Hongko");
        diaryService.saveDiary(diary);
        return "success";
    }

    @RequestMapping("/user/check/{userID}/{diaryID}")
    public String check(int userID, int diaryID) {
        return "5";
    }

    public static byte[] convertImageToByteArray(String imagePath) {
        File imageFile = new File(imagePath);
        try (FileInputStream fis = new FileInputStream(imageFile);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String generateRandomContent() {
        String[] sentences = {
                "今天天气真好，阳光明媚，心情格外舒畅。",
                "这次旅行真是一次难忘的经历，看到了许多美丽的风景。",
                "和朋友们一起度过了愉快的时光，留下了美好的回忆。",
                "这个地方的美食太好吃了，让人回味无穷。"
        };
        Random random = new Random();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            content.append(sentences[random.nextInt(sentences.length)]).append(" ");
        }
        return content.toString();
    }

    private String generateRandomDestination() {
        String[] destinations = {
                "巴黎", "纽约", "东京", "伦敦", "悉尼"
        };
        Random random = new Random();
        return destinations[random.nextInt(destinations.length)];
    }

    @GetMapping("/generateDiariesForImages")
    @Transactional
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> generateDiariesForImages() {
        try {
            logger.info("开始为图片生成日记");
            
            // 获取所有图片文件
            File directory = new File(IMAGE_STORAGE_DIR);
            if (!directory.exists() || !directory.isDirectory()) {
                logger.error("图片目录不存在: {}", IMAGE_STORAGE_DIR);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("图片目录不存在");
            }

            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg"));
            if (files == null || files.length == 0) {
                logger.warn("图片目录为空: {}", IMAGE_STORAGE_DIR);
                return ResponseEntity.ok(Collections.emptyList());
            }

            // 获取所有已存在的图片记录
            List<ImageEntity> existingImages = diaryService.findAllImages();
            Set<String> existingImagePaths = existingImages.stream()
                .map(ImageEntity::getImagepath)
                .collect(Collectors.toSet());

            // 获取所有日记记录
            List<Diary> existingDiaries = diaryRepository.findAll();
            Set<String> usedImageIds = existingDiaries.stream()
                .map(Diary::getImage)
                .filter(images -> images != null && !images.isEmpty())
                .flatMap(images -> Arrays.stream(images.split(",")))
                .collect(Collectors.toSet());

            List<Diary> generatedDiaries = new ArrayList<>();
            int newImageId = diaryService.findImageMaxId() + 1;

            // 按文件名排序处理图片
            Arrays.sort(files, Comparator.comparing(File::getName));
            
            for (File file : files) {
                try {
                    String filePath = file.getAbsolutePath();
                    
                    // 检查图片是否已经被使用
                    if (existingImagePaths.contains(filePath)) {
                        logger.info("跳过已存在的图片: {}", file.getName());
                        continue;
                    }

                    // 生成新的图片记录
                    ImageEntity imageEntity = new ImageEntity(newImageId, filePath);
                    imageEntity = diaryService.saveImage(imageEntity);
                    if (imageEntity == null) {
                        logger.error("保存图片记录失败: {}", file.getName());
                        continue;
                    }

                    // 生成日记内容
                    String fileName = file.getName();
                    String title = generateTitleFromFileName(fileName);
                    String description = generateDescriptionFromFileName(fileName);
                    String content = generateContentFromFileName(fileName);
                    String destination = generateDestinationFromFileName(fileName);

                    // 保存内容
                    ContentEntity contentEntity = new ContentEntity();
                    contentEntity.setContent(content);
                    contentEntity = diaryService.saveContent(contentEntity);
                    if (contentEntity == null || contentEntity.getId() == null) {
                        logger.error("保存内容失败: {}", file.getName());
                        continue;
                    }

                    // 创建并保存日记
                    Diary diary = new Diary();
                    diary.setTitle(title);
                    diary.setDescription(description);
                    diary.setContentId(contentEntity.getId());
                    diary.setUserID(1); // 使用默认用户ID
                    diary.setImage(String.valueOf(newImageId));
                    diary.setDestination(destination);
                    diary.setHot(0);
                    diary.setRate_counts(0);
                    diary.setRate_sum(0);
                    diary.setRating(0.0);

                    diary = diaryService.saveDiary(diary);
                    if (diary != null) {
                        generatedDiaries.add(diary);
                        logger.info("成功生成日记 - 标题: {}, 图片: {}", title, fileName);
                    }

                    newImageId++;
                } catch (Exception e) {
                    logger.error("处理图片 {} 时发生错误: {}", file.getName(), e.getMessage());
                }
            }

            if (generatedDiaries.isEmpty()) {
                logger.warn("没有生成新的日记");
                return ResponseEntity.ok(Map.of(
                    "message", "没有新的图片需要生成日记",
                    "count", 0
                ));
            }

            logger.info("成功生成 {} 篇新日记", generatedDiaries.size());
            return ResponseEntity.ok(Map.of(
                "message", "成功生成 " + generatedDiaries.size() + " 篇新日记",
                "count", generatedDiaries.size(),
                "diaries", generatedDiaries
            ));

        } catch (Exception e) {
            logger.error("生成日记时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("生成日记失败: " + e.getMessage());
        }
    }

    private String generateTitleFromFileName(String fileName) {
        // 从文件名生成更有意义的标题
        String baseName = fileName.replace(".jpg", "");
        int imageNumber = Integer.parseInt(baseName);
        String[] destinations = {
            "巴黎", "东京", "纽约", "伦敦", "悉尼", "罗马", "威尼斯", "巴厘岛", 
            "马尔代夫", "圣托里尼", "普罗旺斯", "瑞士", "北海道", "京都", "首尔"
        };
        String[] adjectives = {
            "难忘的", "浪漫的", "精彩的", "美妙的", "难忘的", "独特的", "梦幻的", 
            "令人难忘的", "难忘的", "难忘的", "难忘的", "难忘的", "难忘的", "难忘的", "难忘的"
        };
        String[] nouns = {
            "旅行", "假期", "冒险", "探索", "体验", "回忆", "故事", 
            "时光", "旅程", "足迹", "足迹", "足迹", "足迹", "足迹", "足迹"
        };
        
        int index = (imageNumber - 1) % destinations.length;
        return destinations[index] + "的" + adjectives[index] + nouns[index];
    }

    private String generateDescriptionFromFileName(String fileName) {
        String baseName = fileName.replace(".jpg", "");
        int imageNumber = Integer.parseInt(baseName);
        String[] descriptions = {
            "记录下这段美好的旅行时光",
            "捕捉旅途中的精彩瞬间",
            "珍藏这份独特的旅行记忆",
            "分享这段难忘的旅程",
            "定格这美好的旅行时刻",
            "记录下这段精彩的旅行故事",
            "珍藏这份珍贵的旅行回忆",
            "分享这段难忘的旅行体验",
            "记录下这段美好的旅行足迹",
            "捕捉旅途中的精彩瞬间",
            "珍藏这份独特的旅行记忆",
            "分享这段难忘的旅程",
            "定格这美好的旅行时刻",
            "记录下这段精彩的旅行故事",
            "珍藏这份珍贵的旅行回忆"
        };
        return descriptions[(imageNumber - 1) % descriptions.length];
    }

    private String generateContentFromFileName(String fileName) {
        String baseName = fileName.replace(".jpg", "");
        int imageNumber = Integer.parseInt(baseName);
        String[] destinations = {
            "巴黎", "东京", "纽约", "伦敦", "悉尼", "罗马", "威尼斯", "巴厘岛", 
            "马尔代夫", "圣托里尼", "普罗旺斯", "瑞士", "北海道", "京都", "首尔"
        };
        String[] contents = {
            "漫步在浪漫的街道上，感受这座城市的独特魅力。每一处风景都让人流连忘返，每一个瞬间都值得珍藏。",
            "探索这座充满活力的城市，体验不一样的文化风情。美食、购物、景点，每一刻都充满惊喜。",
            "在这座不夜城中，感受现代都市的繁华与活力。高楼大厦、霓虹闪烁，处处展现着城市的魅力。",
            "漫步在历史悠久的街道上，感受英伦文化的独特魅力。古老的建筑、优雅的公园，让人沉醉其中。",
            "在这座阳光之城，享受悠闲的海滨生活。美丽的海滩、清新的空气，让人心旷神怡。",
            "探索这座永恒之城的历史遗迹，感受古罗马文明的辉煌。每一块石头都在诉说着历史的故事。",
            "在这座水城，乘坐贡多拉穿梭于古老的运河之间。浪漫的氛围、独特的建筑，让人流连忘返。",
            "在这座热带天堂，享受阳光、沙滩和碧海。美丽的自然风光，让人忘却一切烦恼。",
            "在这片印度洋上的珍珠，享受奢华的海岛度假。清澈的海水、洁白的沙滩，让人心旷神怡。",
            "在这座爱琴海上的明珠，欣赏美丽的日落和蓝顶白墙。浪漫的氛围，让人沉醉其中。",
            "漫步在薰衣草田间，感受普罗旺斯的浪漫气息。美丽的自然风光，让人心旷神怡。",
            "在这片阿尔卑斯山脉的怀抱中，享受清新的空气和美丽的自然风光。",
            "在这座日本北方的城市，体验独特的雪国文化。美食、温泉，让人流连忘返。",
            "漫步在这座古都的街道上，感受日本传统文化的魅力。古老的寺庙、优雅的庭院，让人沉醉其中。",
            "在这座充满活力的城市，体验现代韩国的魅力。美食、购物、文化，处处充满惊喜。"
        };
        return contents[(imageNumber - 1) % contents.length];
    }

    private String generateDestinationFromFileName(String fileName) {
        String baseName = fileName.replace(".jpg", "");
        int imageNumber = Integer.parseInt(baseName);
        String[] destinations = {
            "巴黎", "东京", "纽约", "伦敦", "悉尼", "罗马", "威尼斯", "巴厘岛", 
            "马尔代夫", "圣托里尼", "普罗旺斯", "瑞士", "北海道", "京都", "首尔"
        };
        return destinations[(imageNumber - 1) % destinations.length];
    }

    @GetMapping("/getdiarys")
    public ResponseEntity<?> getdiarys(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String searchType,
        @RequestParam(required = false) Integer sortField,
        @RequestParam(required = false) String sortOrder,
        @RequestParam(required = false) Integer userId
    ) {
        try {
            logger.info("Received request for diaries with parameters: keyword={}, searchType={}, sortField={}, sortOrder={}, userId={}",
                keyword, searchType, sortField, sortOrder, userId);

            // 设置默认值
            sortField = sortField != null ? sortField : 1;
            sortOrder = sortOrder != null ? sortOrder : "desc";
            
            // 创建最终变量用于lambda表达式
            final String finalKeyword = keyword != null ? keyword : "";
            final String finalSearchType = searchType != null ? searchType : "name";
            final String finalUserRole;

            // 获取用户角色
            if (userId != null) {
                User user = userService.getUserById(userId);
                finalUserRole = user != null ? user.getRole() : null;
                logger.info("User role: {}", finalUserRole);
            } else {
                finalUserRole = null;
            }

            // 创建排序对象
            Sort sort;
            switch (sortField) {
                case 1: // 热度排序
                    sort = Sort.by(sortOrder.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, "hot");
                    break;
                case 2: // 评分排序
                    sort = Sort.by(sortOrder.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, "rating");
                    break;
                case 3: // 时间排序
                    sort = Sort.by(sortOrder.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, "id");
                    break;
                default:
                    sort = Sort.by(Sort.Direction.DESC, "hot");
            }

            // 创建查询条件
            Specification<Diary> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                // 添加搜索条件
                if (finalKeyword != null && !finalKeyword.trim().isEmpty()) {
                    switch (finalSearchType) {
                        case "name":
                            predicates.add(cb.like(root.get("title"), "%" + finalKeyword + "%"));
                            break;
                        case "destination":
                            predicates.add(cb.like(root.get("destination"), "%" + finalKeyword + "%"));
                            break;
                        case "full_text":
                            // 搜索标题
                            Predicate titlePredicate = cb.like(root.get("title"), "%" + finalKeyword + "%");
                            // 搜索简介
                            Predicate descPredicate = cb.like(root.get("description"), "%" + finalKeyword + "%");
                            // 搜索目的地
                            Predicate destPredicate = cb.like(root.get("destination"), "%" + finalKeyword + "%");
                            // 搜索正文内容
                            Join<Diary, ContentEntity> contentJoin = root.join("content", JoinType.INNER);
                            Predicate contentPredicate = cb.like(contentJoin.get("content"), "%" + finalKeyword + "%");
                            
                            // 组合所有条件（OR关系）
                            predicates.add(cb.or(titlePredicate, descPredicate, destPredicate, contentPredicate));
                            break;
                    }
                }

                // 添加用户权限过滤
                if (finalUserRole == null || !finalUserRole.equals("ADMIN")) {
                    if (userId != null) {
                        predicates.add(cb.equal(root.get("userID"), userId));
                    }
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            // 执行查询，获取所有符合条件的日记
            List<Diary> diaries = diaryRepository.findAll(spec, sort);
            logger.info("Found {} diaries matching the criteria", diaries.size());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("msg", "success");
            response.put("data", Map.of(
                "data", diaries,
                "total", diaries.size()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting diaries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "code", 500,
                    "msg", "Error getting diaries: " + e.getMessage()
                ));
        }
    }

    @RequestMapping("/get/diary/{id}")
    public ResponseEntity<?> getDiary(@PathVariable Integer id) {
        try {
            Diary diary = diaryService.getDiary(id);
            if (diary == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 如果日记有视频ID，确保视频信息被正确加载
            if (diary.getVideoId() != null) {
                try {
                    Video video = videoService.getVideo(diary.getVideoId());
                    diary.setVideo(video);
                } catch (Exception e) {
                    System.out.println("Error loading video: " + e.getMessage());
                    // 继续处理，即使视频加载失败
                }
            }
            
            return ResponseEntity.ok(diary);
        } catch (Exception e) {
            System.out.println("Error getting diary: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping("/get/diarydata/{ID}")
    public DiaryData getDiaryData(@PathVariable int ID) {
        return diaryService.getDiaryDataById(ID);
    }

    @RequestMapping("/get/diarydata/{diaryId}/checkRating")
    public Map<String, Object> checkRating(@PathVariable int diaryId, @RequestParam(required = false) Integer userId) {
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null) {
            result.put("hasRated", false);
            result.put("rating", 0);
            return result;
        }

        try {
            User user = userService.findById(userId);
            if (user == null) {
                result.put("hasRated", false);
                result.put("rating", 0);
                return result;
            }

            String existingRating = userService.check_rate(userId, diaryId);
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
        
        return result;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/rate")
    public ResponseEntity<?> rateDiary(@RequestBody Map<String, Object> request) {
        logger.info("收到评分请求: {}", request);
        try {
            if (request == null) {
                logger.error("请求数据为空");
                return ResponseEntity.badRequest().body("请求数据不能为空");
            }

            // 验证必要的参数
            if (!request.containsKey("diaryId") || !request.containsKey("userId") || !request.containsKey("rating")) {
                logger.error("缺少必要参数。请求包含: {}", request.keySet());
                return ResponseEntity.badRequest().body("缺少必要的参数");
            }

            int diaryId = (int) request.get("diaryId");
            int userId = (int) request.get("userId");
            int rating = (int) request.get("rating");
            logger.info("处理评分 - 日记ID: {}, 用户ID: {}, 评分: {}", diaryId, userId, rating);

            // 验证评分范围
            if (rating < 1 || rating > 5) {
                logger.error("无效的评分值: {}", rating);
                return ResponseEntity.badRequest().body("评分必须在1到5之间");
            }

            // 获取日记
            Diary diary = diaryService.findById(diaryId);
            if (diary == null) {
                logger.error("未找到日记，ID: {}", diaryId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("日记不存在");
            }
            logger.info("找到日记: {}", diary.getTitle());

            // 获取用户
            User user = userService.findById(userId);
            if (user == null) {
                logger.error("未找到用户，ID: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("用户不存在");
            }
            logger.info("找到用户: {}", user.getName());

            // 检查用户是否已经评分
            String existingRating = userService.check_rate(userId, diaryId);
            if (!existingRating.equals("False")) {
                logger.warn("用户已经对该日记评分。现有评分: {}", existingRating);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("您已经对该日记进行过评分");
            }

            // 更新日记评分
            diary.setRate_sum(diary.getRate_sum() + rating);
            diary.setRate_counts(diary.getRate_counts() + 1);
            diary.setRating((double) diary.getRate_sum() / diary.getRate_counts());
            diaryService.saveDiary(diary);
            logger.info("更新日记评分。新评分: {}", diary.getRating());

            // 更新用户的评分记录
            userService.addRating(userId, diaryId, rating);
            logger.info("更新用户评分记录");

            Map<String, Object> response = new HashMap<>();
            response.put("newRating", diary.getRating());
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

    @RequestMapping("/diary/add_hot/{diaryId}")
    public String add_hot(@PathVariable int diaryId) {
        Diary diary = diaryService.findById(diaryId);
        if (diary != null) {
            diary.setHot(diary.getHot() + 1);
            diaryService.saveDiary(diary);
            return "success";
        }
        return "fail";
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDiary(@PathVariable Integer id, @RequestParam(required = false) Integer userId) {
        try {
            logger.info("尝试删除日记 - 日记ID: {}, 用户ID: {}", id, userId);
            
            // 检查用户是否存在
            if (userId == null) {
                logger.error("删除失败 - 缺少用户ID参数");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("缺少用户ID参数");
            }

            User user = userService.findById(userId);
            if (user == null) {
                logger.error("删除失败 - 用户不存在，用户ID: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("用户不存在");
            }
            logger.info("找到用户 - 用户名: {}, 角色: {}", user.getName(), user.getRole());

            // 检查日记是否存在
            Diary diary = diaryService.findById(id);
            if (diary == null) {
                logger.error("删除失败 - 日记不存在，日记ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("日记不存在");
            }
            logger.info("找到日记 - 标题: {}, 作者ID: {}", diary.getTitle(), diary.getUserID());

            // 检查权限
            if (!user.isAdmin() && diary.getUserID() != userId) {
                logger.error("删除失败 - 权限不足 - 用户ID: {}, 日记作者ID: {}", userId, diary.getUserID());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("您没有权限删除此日记");
            }

            // 执行删除
            diaryService.deleteDiary(id, userId);
            logger.info("日记删除成功 - 日记ID: {}", id);
            return ResponseEntity.ok().body("日记删除成功");
        } catch (RuntimeException e) {
            logger.error("删除日记时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("删除日记时发生错误: " + e.getMessage());
        }
    }

    @GetMapping("/deleteDuplicateDiaries")
    @Transactional
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> deleteDuplicateDiaries() {
        try {
            logger.info("开始删除重复日记");
            // 获取所有日记
            List<Diary> allDiaries = diaryRepository.findAll();
            Map<String, List<Diary>> titleGroups = new HashMap<>();
            
            // 按标题分组
            for (Diary diary : allDiaries) {
                titleGroups.computeIfAbsent(diary.getTitle(), k -> new ArrayList<>()).add(diary);
            }
            
            // 找出重复的日记
            List<Diary> toDelete = new ArrayList<>();
            for (List<Diary> group : titleGroups.values()) {
                if (group.size() > 1) {
                    // 保留ID最小的日记，删除其他的
                    group.sort(Comparator.comparing(Diary::getId));
                    toDelete.addAll(group.subList(1, group.size()));
                }
            }
            
            logger.info("找到 {} 篇重复日记需要删除", toDelete.size());
            
            // 先删除日记记录
            for (Diary diary : toDelete) {
                try {
                    // 先删除日记
                    diaryRepository.delete(diary);
                    logger.info("删除日记 ID: {}, 标题: {}", diary.getId(), diary.getTitle());
                    
                    // 然后删除关联的内容
                    if (diary.getContentId() != null) {
                        contentRepository.deleteById(diary.getContentId());
                        logger.info("删除内容 ID: {}", diary.getContentId());
                    }
                } catch (Exception e) {
                    logger.error("删除日记时发生错误 - ID: {}, 错误: {}", diary.getId(), e.getMessage());
                }
            }
            
            logger.info("成功删除 {} 篇重复日记", toDelete.size());
            return ResponseEntity.ok(Map.of(
                "deletedCount", toDelete.size(),
                "message", "成功删除 " + toDelete.size() + " 篇重复日记"
            ));
        } catch (Exception e) {
            logger.error("删除重复日记时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("删除重复日记失败: " + e.getMessage());
        }
    }

    @GetMapping("/reorderDiaryImages")
    @Transactional
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> reorderDiaryImages() {
        try {
            logger.info("开始重新排序日记图片");
            File directory = new File(IMAGE_STORAGE_DIR);
            if (!directory.exists() || !directory.isDirectory()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("图片目录不存在: " + IMAGE_STORAGE_DIR);
            }

            // 获取所有jpg文件并按名称排序
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg"));
            if (files == null || files.length == 0) {
                return ResponseEntity.ok("没有找到图片文件");
            }
            Arrays.sort(files, Comparator.comparing(File::getName));

            // 创建临时目录用于重命名
            File tempDir = new File(IMAGE_STORAGE_DIR + "temp");
            if (!tempDir.exists()) {
                tempDir.mkdir();
            }

            // 第一步：将所有文件移动到临时目录，使用新名称
            Map<String, String> oldToNewNames = new HashMap<>();
            for (int i = 0; i < files.length; i++) {
                File oldFile = files[i];
                String newName = (i + 1) + ".jpg";
                File newFile = new File(tempDir, newName);
                oldToNewNames.put(oldFile.getName(), newName);
                if (!oldFile.renameTo(newFile)) {
                    throw new RuntimeException("无法重命名文件: " + oldFile.getName());
                }
                logger.info("重命名文件: {} -> {}", oldFile.getName(), newName);
            }

            // 第二步：将文件从临时目录移回原目录
            File[] tempFiles = tempDir.listFiles();
            if (tempFiles != null) {
                for (File file : tempFiles) {
                    File targetFile = new File(IMAGE_STORAGE_DIR, file.getName());
                    if (!file.renameTo(targetFile)) {
                        throw new RuntimeException("无法移动文件: " + file.getName());
                    }
                }
            }

            // 删除临时目录
            tempDir.delete();

            // 更新数据库中的图片路径
            List<ImageEntity> allImages = diaryService.findAllImages();
            for (ImageEntity image : allImages) {
                String oldPath = image.getImagepath();
                String fileName = new File(oldPath).getName();
                String newFileName = oldToNewNames.get(fileName);
                if (newFileName != null) {
                    String newPath = IMAGE_STORAGE_DIR + newFileName;
                    image.setImagepath(newPath);
                    diaryService.saveImage(image);
                    logger.info("更新数据库图片路径: {} -> {}", oldPath, newPath);
                }
            }

            logger.info("成功重新排序 {} 个图片文件", files.length);
            return ResponseEntity.ok(Map.of(
                "message", "成功重新排序 " + files.length + " 个图片文件",
                "count", files.length
            ));
        } catch (Exception e) {
            logger.error("重新排序图片时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("重新排序图片失败: " + e.getMessage());
        }
    }
}

