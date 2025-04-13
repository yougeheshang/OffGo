package org.tinkerhub.offgo.mysql_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tinkerhub.offgo.Repository.ContentRepository;
import org.tinkerhub.offgo.Repository.ImageRepository;
import org.tinkerhub.offgo.Repository.UserRepository;
import org.tinkerhub.offgo.Repository.DiaryRepository;
import org.tinkerhub.offgo.entity.ContentEntity;
import org.tinkerhub.offgo.entity.ImageEntity;
import org.tinkerhub.offgo.entity.diary;

@Service
public class Diary_service {
    @Autowired
    private DiaryRepository diayRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContentRepository contentRepository;
    public Diary_service() {

    }
    public int find_content_max_id() {
        return contentRepository.findMaxID();
    }
    public int find_image_max_id() {
        return imageRepository.findMaxID();
    }
    public diary findById(int id) {
        return diayRepository.findById(id);
    }
    public ContentEntity find_content_by_id(int id) {
        return contentRepository.findById(id);
    }
    public ImageEntity find_image_by_id(int id) {
        return imageRepository.findById(id);
    }
    public diary saveDiary(diary diary) {
        return diayRepository.save(diary);
    }
    public ImageEntity saveImage(ImageEntity imageEntity) {
        return imageRepository.save(imageEntity);
    }
    public ContentEntity saveContent(ContentEntity contentEntity) {
        return contentRepository.save(contentEntity);
    }
}
