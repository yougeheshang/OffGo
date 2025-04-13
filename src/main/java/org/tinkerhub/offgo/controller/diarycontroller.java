package org.tinkerhub.offgo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tinkerhub.offgo.entity.ContentEntity;
import org.tinkerhub.offgo.entity.ImageEntity;
import org.tinkerhub.offgo.entity.diary;
import org.tinkerhub.offgo.mysql_service.Diary_service;

import java.io.*;
import java.awt.*;
import java.nio.file.Path;

@RestController
public class diarycontroller {
    @Autowired
    private Diary_service diaryService;


    private static final String IMAGE_STORAGE_DIR = "src/main/resources/static/images/";

    @RequestMapping("/savediary_withoutimage")
    public boolean savediary_without(@RequestParam String title, @RequestParam String description, @RequestParam String content,@RequestParam int userID) {
        int contentID = diaryService.find_content_max_id() + 1;
        ContentEntity contentEntity = new ContentEntity(contentID, content);
        diaryService.saveContent(contentEntity);
        int diaryID = diaryService.find_image_max_id() + 1;
        diaryService.saveDiary(new diary(diaryID,title, userID, description, contentID));
        return true;
    }
    @RequestMapping("/savediary_withimage")
    public boolean savediary_with(@RequestParam String title, @RequestParam String description, @RequestParam String content, @RequestParam int userID, @RequestParam int image, @RequestParam int image_num) {
        int contentID = diaryService.find_content_max_id() + 1;
        ContentEntity contentEntity = new ContentEntity(contentID, "");
        diaryService.saveContent(contentEntity);
        int diaryID = diaryService.find_image_max_id() + 1;
        int imageID = diaryService.find_image_max_id() + 1;
        int[]arr=new int[image_num];
        imageID-=image_num;
        for (int i = 0; i < image_num; i++) {
            arr[i]=imageID;
            imageID++;
        }
        diary diary = new diary(diaryID, title, userID, description, contentID,arr);
        diaryService.saveDiary(diary);
        return true;
    }
    @RequestMapping("/getdiary/{ID}")
    public diary getdiary(@PathVariable int ID) {
        return diaryService.findById(ID);
    }
    @RequestMapping("/getimage/{ID}")
    public byte[] getimage(@PathVariable int ID) {
        ImageEntity imageEntity = diaryService.find_image_by_id(ID);
        String imagePath = imageEntity.getImagepath();
        File file = new File(imagePath);
        byte[] buffer = new byte[(int) file.length()];
        if (file.exists() && file.canRead()) { // 检查文件是否存在且可读
            try (FileInputStream fis = new FileInputStream(file)) { // 使用 try-with-resources 自动关闭资源
                fis.read(buffer);
            } catch (FileNotFoundException e) {
                e.printStackTrace(); // 处理文件未找到异常
            } catch (IOException e) {
                e.printStackTrace(); // 处理 IO 异常
            }
        }
        return buffer; // 返回字节数组
    }
    @RequestMapping("/getcontent/{ID}")
    public ContentEntity getcontent(@PathVariable int ID) {
        return diaryService.find_content_by_id(ID);
    }
    @RequestMapping("/saveImage")
    public void saveImage(@RequestParam byte[] imageData) {
        int imageID = diaryService.find_image_max_id() + 1;
        String imagePath = IMAGE_STORAGE_DIR +imageID+"jpg";
        try (FileOutputStream fos = new FileOutputStream(imagePath)){
            fos.write(imageData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int maxID = diaryService.find_image_max_id() + 1;
        ImageEntity imageEntity = new ImageEntity(maxID, imagePath);
    }
    @RequestMapping("/test_add")
    public String test_add() {
        int image_num =1;
        String image_path="src/main/resources/static/images/1.jpg";
        String title="巴黎";
        String description="巴黎";
        int userID = 1;
        String content="实验内容";
        int contentID = diaryService.find_content_max_id() + 1;
        ContentEntity contentEntity = new ContentEntity(contentID, content);
        diaryService.saveContent(contentEntity);
        int diaryID = diaryService.find_image_max_id() + 1;
        int imageID = diaryService.find_image_max_id() + 1;
        for (int i = 0; i < image_num; i++) {
            ImageEntity imageEntity = new ImageEntity(imageID, image_path);
            diaryService.saveImage(imageEntity);
            imageID++;
        }
        int[]arr=new int[image_num];
        imageID-=image_num;
        for (int i = 0; i < image_num; i++) {
            arr[i]=imageID;
            imageID++;
        }
        diary diary = new diary(diaryID, title, userID, description, contentID,arr);
        diaryService.saveDiary(diary);
        return "success";
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

}
