package com.example.offgo3.service.impl;

import com.example.offgo3.entity.Diary;
import com.example.offgo3.repository.DiaryRepository;
import com.example.offgo3.service.DiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class DiaryServiceImpl implements DiaryService {

    private static final Logger logger = LoggerFactory.getLogger(DiaryServiceImpl.class);

    @Autowired
    private DiaryRepository diaryRepository;

    private final String uploadDir;

    public DiaryServiceImpl() {
        this.uploadDir = Paths.get("").toAbsolutePath().toString() + "/uploads/";
        logger.info("Upload directory: {}", this.uploadDir);
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created upload directory: {}", uploadPath);
            }
        } catch (IOException e) {
            logger.error("Failed to create upload directory", e);
        }
    }

    @Override
    @Transactional
    public Diary createDiary(Diary diary) {
        diary.setCreateTime(LocalDateTime.now());
        if (diary.getImages() != null) {
            List<String> processedImages = new ArrayList<>();
            for (String imageUrl : diary.getImages()) {
                if (imageUrl.startsWith("http")) {
                    String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                    processedImages.add("/uploads/" + fileName);
                } else if (imageUrl.startsWith("/uploads/")) {
                    processedImages.add(imageUrl);
                } else {
                    processedImages.add("/uploads/" + imageUrl);
                }
            }
            diary.setImages(processedImages);
        }
        return diaryRepository.save(diary);
    }

    @Override
    public Diary getDiaryById(Long id) {
        return diaryRepository.findById(id).orElse(null);
    }

    @Override
    public List<Diary> getAllDiaries() {
        return diaryRepository.findAll();
    }

    @Override
    @Transactional
    public Diary updateDiary(Diary diary) {
        try {
            Diary existingDiary = diaryRepository.findById(diary.getId())
                    .orElseThrow(() -> new RuntimeException("日记不存在"));
            
            existingDiary.setTitle(diary.getTitle());
            existingDiary.setContent(diary.getContent());
            
            if (diary.getImages() != null) {
                List<String> processedImages = new ArrayList<>();
                for (String imageUrl : diary.getImages()) {
                    if (imageUrl.startsWith("http")) {
                        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                        processedImages.add("/uploads/" + fileName);
                    } else if (imageUrl.startsWith("/uploads/")) {
                        processedImages.add(imageUrl);
                    } else {
                        processedImages.add("/uploads/" + imageUrl);
                    }
                }
                existingDiary.setImages(processedImages);
            }
            
            existingDiary.setUpdateTime(LocalDateTime.now());
            return diaryRepository.save(existingDiary);
        } catch (Exception e) {
            logger.error("更新日记失败", e);
            throw new RuntimeException("更新日记失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteDiary(Long id) {
        Diary diary = diaryRepository.findById(id).orElse(null);
        if (diary != null && diary.getImages() != null) {
            for (String imageUrl : diary.getImages()) {
                try {
                    String fileName = imageUrl;
                    Path filePath = Paths.get(uploadDir, fileName);
                    logger.info("Deleting file: {}", filePath);
                    if (Files.deleteIfExists(filePath)) {
                        logger.info("File deleted successfully: {}", fileName);
                    } else {
                        logger.warn("File not found: {}", fileName);
                    }
                } catch (IOException e) {
                    logger.error("Failed to delete file: {}", imageUrl, e);
                }
            }
        }
        diaryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void incrementHot(Long id) {
        Diary diary = diaryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("日记不存在"));
        diary.setHot(diary.getHot() + 1);
        diaryRepository.save(diary);
    }

    @Override
    public String saveImage(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + extension;
            
            Path filePath = Paths.get(uploadDir, newFilename);
            logger.info("Saving file to: {}", filePath);
            
            Files.copy(file.getInputStream(), filePath);
            logger.info("File saved successfully: {}", newFilename);
            
            return newFilename;
        } catch (IOException e) {
            logger.error("Failed to save file", e);
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }
    }
} 