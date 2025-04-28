package org.tinkerhub.offgo.mysql_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tinkerhub.offgo.Repository.ContentRepository;
import org.tinkerhub.offgo.Repository.ImageRepository;
import org.tinkerhub.offgo.Repository.UserRepository;
import org.tinkerhub.offgo.Repository.DiaryRepository;
import org.tinkerhub.offgo.entity.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.io.File;


@Service
public class Diary_service {
    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContentRepository contentRepository;
    public Diary_service() {

    }
    public int find_content_max_id() {
        if (contentRepository.findAll().isEmpty()) {
            return 0;
        }

        return contentRepository.findMaxID();
    }
    public int find_image_max_id() {
        if (imageRepository.findAll().isEmpty()) {
        return 0;
    }

        return imageRepository.findMaxID();
    }
    public diary findById(int id) {
        return diaryRepository.findById(id);
    }
    public ContentEntity find_content_by_id(int id) {
        return contentRepository.findById(id);
    }
    public ImageEntity find_image_by_id(int id) {
        return imageRepository.findById(id);
    }
    public diary saveDiary(diary diary) {
        return diaryRepository.save(diary);
    }
    public ImageEntity saveImage(ImageEntity imageEntity) {
        return imageRepository.save(imageEntity);
    }
    public ContentEntity saveContent(ContentEntity contentEntity) {
        return contentRepository.save(contentEntity);
    }
    public void cleanDiaryData() {
        // 清空 diary 表的数据
        diaryRepository.deleteAll();
        // 清空 ContentEntity 表的数据
        contentRepository.deleteAll();
        // 清空 ImageEntity 表的数据
        imageRepository.deleteAll();
    }
    public List<diary> getAllDiaries() {
        return diaryRepository.findAll();
    }
    List<diary> order_diaries_hot(List<diary> diaries) {
        int n = diaries.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (diaries.get(j).getHot() < diaries.get(j + 1).getHot()) {
                    // 交换元素
                    diary temp = diaries.get(j);
                    diaries.set(j, diaries.get(j + 1));
                    diaries.set(j + 1, temp);
                }
            }
        }
        return diaries;
    }

    List<diary> order_diaries_rating(List<diary> diaries) {
        int n = diaries.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (diaries.get(j).getRating() < diaries.get(j + 1).getRating()) {
                    // 交换元素
                    diary temp = diaries.get(j);
                    diaries.set(j, diaries.get(j + 1));
                    diaries.set(j + 1, temp);
                }
            }
        }
        return diaries;
    }
    public List<DiaryCard> getDiaryCards(int sortField, String sortOrder) {
        List<diary> diaries = getAllDiaries();
        switch (sortField) {
            case 0:
                diaries = order_diaries_rating(diaries);
                break;
            case 1:
                diaries = order_diaries_hot(diaries);
                break;
            default:
                break;
        }
        
        // 如果是升序，反转列表
        if ("asc".equalsIgnoreCase(sortOrder)) {
            java.util.Collections.reverse(diaries);
        }
        
        List<DiaryCard> diaryCards = new ArrayList<>();
        for (diary d : diaries) {
            DiaryCard card = new DiaryCard(
                    d.getId(),
                    d.getTitle(),
                    d.getDescription(),
                    d.getImage(),
                    d.getHot(),
                    d.getRating(),
                    d.getDestination()
            );
            diaryCards.add(card);
        }
        return diaryCards;
    }

    public DiaryData convertToDiaryData(diary d) {
        ContentEntity contentEntity = contentRepository.findById(d.getContent());
        String content = contentEntity != null ? contentEntity.getContent() : "";

        return new DiaryData(
                d.getId(),
                d.getTitle(),
                d.getDescription(),
                d.getImage(),
                d.getHot(),
                d.getDestination(),
                d.getRating(),
                d.getRate_sum(),
                d.getRate_counts(),
                d.getUserID(),
                content
        );
    }

    public DiaryData getDiaryDataById(int id) {
        diary d = diaryRepository.findById(id);
        if (d != null) {
            return convertToDiaryData(d);
        }
        return null;
    }

    @Transactional
    public void deleteDiary(int diaryId) {
        try {
            // 先获取日记对象
            diary diaryToDelete = diaryRepository.findById(diaryId);
            if (diaryToDelete == null) {
                throw new RuntimeException("日记不存在");
            }

            // 删除关联的图片
            if (diaryToDelete.getImage() != null) {
                for (int imageId : diaryToDelete.getImage()) {
                    imageRepository.deleteById(imageId);
                }
            }

            // 删除关联的内容
            contentRepository.deleteById(diaryToDelete.getContent());

            // 最后删除日记
            diaryRepository.deleteById(diaryId);
        } catch (Exception e) {
            throw new RuntimeException("删除日记失败: " + e.getMessage());
        }
    }
}
