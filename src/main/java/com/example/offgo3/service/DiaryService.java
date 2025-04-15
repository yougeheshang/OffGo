package com.example.offgo3.service;

import com.example.offgo3.entity.Diary;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface DiaryService {
    Diary createDiary(Diary diary);
    Diary getDiaryById(Long id);
    List<Diary> getAllDiaries();
    Diary updateDiary(Diary diary);
    void deleteDiary(Long id);
    @Transactional
    void incrementHot(Long id);
    String saveImage(MultipartFile file);
} 