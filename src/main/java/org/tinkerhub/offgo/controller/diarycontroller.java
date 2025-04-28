package org.tinkerhub.offgo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.tinkerhub.offgo.entity.*;
import org.tinkerhub.offgo.mysql_service.Diary_service;
import org.tinkerhub.offgo.mysql_service.User_service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.io.*;
import java.util.*;
import java.util.Base64;

@RestController
@CrossOrigin(origins = "*")
public class DiaryController {
    @Autowired
    private Diary_service diaryService;
    @Autowired
    private User_service userService;

    @RequestMapping("/clean_database")
    public boolean cleanDatabase() {
        diaryService.cleanDiaryData();
        userService.deleteall();
        return true;
    }

    private static final String IMAGE_STORAGE_DIR = "src/main/resources/static/images/diary/";

    @RequestMapping("/savediary_withoutimage")
    public boolean savediary_without(@RequestParam String title, @RequestParam String description, @RequestParam String content, @RequestParam int userID, @RequestParam String destination) {
        int contentID = diaryService.find_content_max_id() + 1;
        ContentEntity contentEntity = new ContentEntity(contentID, content);
        diaryService.saveContent(contentEntity);
        int diaryID = diaryService.find_image_max_id() + 1;
        diaryService.saveDiary(new diary(diaryID, title, userID, description, contentID, destination));
        return true;
    }

    @RequestMapping("/savediary_withimage")
    public boolean savediary_with(@RequestParam String title, @RequestParam String description, @RequestParam String content, @RequestParam int userID, @RequestParam int image_num, @RequestParam String destination) {
        int contentID = diaryService.find_content_max_id() + 1;
        ContentEntity contentEntity = new ContentEntity(contentID, content);
        diaryService.saveContent(contentEntity);
        int diaryID = diaryService.find_image_max_id() + 1;
        int imageID = diaryService.find_image_max_id() + 1;
        int[] arr = new int[image_num];
        imageID -= image_num;
        for (int i = 0; i < image_num; i++) {
            arr[i] = imageID;
            imageID++;
        }
        diary diary = new diary(diaryID, title, userID, description, contentID, arr, destination);
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
    public int saveImage(@RequestParam String imageData) {
        try {
            if (imageData == null || imageData.isEmpty()) {
                throw new RuntimeException("图片数据为空");
            }

            // 解码Base64数据
            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            int imageID = diaryService.find_image_max_id() + 1;
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
        int[] arr = new int[image_num];
        imageID -= image_num;
        for (int i = 0; i < image_num; i++) {
            arr[i] = imageID;
            imageID++;
        }
        diary diary = new diary(diaryID, title, userID, description, contentID, arr, "Hongko");
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

    // 模拟生成随机的目的地
    private String generateRandomDestination() {
        String[] destinations = {
                "巴黎", "纽约", "东京", "伦敦", "悉尼"
        };
        Random random = new Random();
        return destinations[random.nextInt(destinations.length)];
    }

    // 为每张图片生成日记
    @RequestMapping("/generateDiariesForImages")
    public List<diary> generateDiariesForImages() {
        File directory = new File(IMAGE_STORAGE_DIR);
        File[] files = directory.listFiles();
        List<diary> a = new ArrayList<diary>();
        for (int i = 1; i <= 94; i++) {
            String fileName = i + ".jpg";
            File file = new File(IMAGE_STORAGE_DIR + fileName);
            if (file.exists() && file.isFile()) {
                // 生成日记信息
                int contentID = diaryService.find_content_max_id() + 1;
                String content = generateRandomContent();
                ContentEntity contentEntity = new ContentEntity(contentID, content);
                diaryService.saveContent(contentEntity);

                int diaryID = diaryService.find_image_max_id() + 1;
                int imageID = diaryService.find_image_max_id() + 1;
                ImageEntity imageEntity = new ImageEntity(imageID, file.getAbsolutePath());
                diaryService.saveImage(imageEntity);
                String destination = generateRandomDestination();
                int[] arr = {imageID};
                String title = "日记 - " + i;
                String description = "关于 " + file.getName() + " 的日记";
                int userID = 1; // 假设用户 ID 为 1
                diary diary = new diary(diaryID, title, userID, description, contentID, arr, destination);
                a.add(diary);
                diaryService.saveDiary(diary);
            }
        }
        return a;
        //return diaryService.getAllDiaries();
    }

    @RequestMapping("/get/diaries")
    public List<DiaryCard> getdiarys( @RequestParam(required = false, defaultValue = "") String keyword,
                                      @RequestParam(required = false, defaultValue = "name") String searchType,
                                      @RequestParam(required = false, defaultValue = "hot") int sortField,
                                      @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        return diaryService.getDiaryCards(sortField, sortOrder);
    }

    @RequestMapping("/get/diary/{ID}")
    public diary getdiarybyID(@PathVariable int ID) {
        diary diary1 = diaryService.findById(ID);
        return diary1;
    }

    @RequestMapping("/get/diarydata/{ID}")
    public DiaryData getDiaryData(@PathVariable int ID) {
        return diaryService.getDiaryDataById(ID);
    }

    @RequestMapping("/get/diarydata/{diaryId}/checkRating")
    public Map<String, Object> checkRating(@PathVariable int diaryId) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasRated", false); // 模拟用户已评分
        result.put("rating", 4); // 模拟评分为4
        return result;

    }

    @RequestMapping("/diary/add_hot/{diaryId}")
    public String add_hot(@PathVariable int diaryId) {
        diary diary = diaryService.findById(diaryId);
        if (diary != null) {
            diary.setHot(diary.getHot() + 1);
            diaryService.saveDiary(diary);
            return "success";
        }
        return "fail";
    }

    @DeleteMapping("/diary/{id}")
    public ResponseEntity<?> deleteDiary(@PathVariable int id) {
        try {
            diaryService.deleteDiary(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除日记失败: " + e.getMessage());
        }
    }
}

