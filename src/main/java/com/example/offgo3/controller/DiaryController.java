package com.example.offgo3.controller;

import com.example.offgo3.entity.Diary;
import com.example.offgo3.service.DiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diaries")
public class DiaryController {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Diary> createDiary(@RequestBody Diary diary) {
        Diary savedDiary = diaryService.createDiary(diary);
        return ResponseEntity.ok(savedDiary);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Diary> getDiary(@PathVariable Long id) {
        Diary diary = diaryService.getDiaryById(id);
        return diary != null ? ResponseEntity.ok(diary) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Diary>> getAllDiaries() {
        return ResponseEntity.ok(diaryService.getAllDiaries());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Diary> updateDiary(@PathVariable Long id, @RequestBody Diary diary) {
        try {
            diary.setId(id);
            Diary updatedDiary = diaryService.updateDiary(diary);
            return ResponseEntity.ok(updatedDiary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiary(@PathVariable Long id) {
        diaryService.deleteDiary(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/hot")
    public ResponseEntity<?> incrementHot(@PathVariable Long id) {
        try {
            diaryService.incrementHot(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文件为空"));
            }
            String imageUrl = diaryService.saveImage(file);
            if (imageUrl == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "文件保存失败"));
            }
            return ResponseEntity.ok(Map.of("url", "/uploads/" + imageUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "上传失败：" + e.getMessage()));
        }
    }

    @PostMapping("/init")
    public ResponseEntity<String> initTestData() {
        try {
            // 创建测试日记
            Diary diary1 = new Diary();
            diary1.setTitle("测试日记1");
            diary1.setContent("这是第一篇测试日记的内容");
            diary1.setImages(List.of("/uploads/test1.jpg"));
            diaryService.createDiary(diary1);

            Diary diary2 = new Diary();
            diary2.setTitle("测试日记2");
            diary2.setContent("这是第二篇测试日记的内容");
            diary2.setImages(List.of("/uploads/test2.jpg"));
            diaryService.createDiary(diary2);

            return ResponseEntity.ok("测试数据初始化成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("初始化失败：" + e.getMessage());
        }
    }
} 