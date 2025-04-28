package org.tinkerhub.offgo.mysql_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tinkerhub.offgo.Repository.ContentRepository;
import org.tinkerhub.offgo.Repository.ImageRepository;
import org.tinkerhub.offgo.Repository.UserRepository;
import org.tinkerhub.offgo.Repository.DiaryRepository;
import org.tinkerhub.offgo.entity.*;

import java.util.ArrayList;
import java.util.List;


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
    public List<DiaryCard> getDiaryCards(int i) {
        List<diary> diaries = getAllDiaries();
        switch (i) {
            case 0:
                diaries= order_diaries_rating(diaries);
                break;
            case 1:
                diaries= order_diaries_hot(diaries);
                break;
            default:
                break;
        }
        List<DiaryCard> diaryCards = new ArrayList<>();
        for (diary d : diaries) {
            // 假设这里需要根据 imageIDs 从其他地方获取图片路径列表
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
}
