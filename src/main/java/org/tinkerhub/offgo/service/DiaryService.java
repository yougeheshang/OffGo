package org.tinkerhub.offgo.service;

import org.springframework.stereotype.Service;
import org.tinkerhub.offgo.entity.Diary;
import org.tinkerhub.offgo.entity.ContentEntity;
import org.tinkerhub.offgo.entity.ImageEntity;
import org.tinkerhub.offgo.entity.DiaryCard;
import org.tinkerhub.offgo.entity.DiaryData;
import org.tinkerhub.offgo.entity.User;
import org.tinkerhub.offgo.repository.DiaryRepository;
import org.tinkerhub.offgo.repository.ImageRepository;
import org.tinkerhub.offgo.repository.ContentRepository;
import org.tinkerhub.offgo.service.UserService;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.tinkerhub.offgo.entity.User;
import java.util.Arrays;

@Service
public class DiaryService {
    private final DiaryRepository diaryRepository;
    private final ImageRepository imageRepository;
    private final ContentRepository contentRepository;
    private final VideoService videoService;
    @Autowired
    private UserService userService;

    public DiaryService(DiaryRepository diaryRepository, ImageRepository imageRepository, 
                       ContentRepository contentRepository, VideoService videoService) {
        this.diaryRepository = diaryRepository;
        this.imageRepository = imageRepository;
        this.contentRepository = contentRepository;
        this.videoService = videoService;
    }

    public Diary saveDiaryWithImages(String title, String description, String content, Long userId, 
                                   String destination, List<Long> imageIds, Long videoId, List<String> tags) {
        Diary diary = new Diary();
        diary.setTitle(title);
        diary.setDescription(description);
        diary.setContentId(Integer.parseInt(content));
        diary.setUserID(userId.intValue());
        diary.setDestination(destination);
        diary.setImage(imageIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",")));
        diary.setVideoId(videoId);
        diary.setHot(0);
        diary.setRating(0.0);
        diary.setRate_sum(0);
        diary.setRate_counts(0);
        if (tags != null && !tags.isEmpty()) {
            diary.setTags(String.join(",", tags));
        }
        return diaryRepository.save(diary);
    }

    public int findContentMaxId() {
        return contentRepository.findMaxId();
    }

    public ContentEntity saveContent(ContentEntity content) {
        return contentRepository.save(content);
    }

    public int findImageMaxId() {
        return imageRepository.findMaxId();
    }

    public Diary saveDiary(Diary diary) {
        return diaryRepository.save(diary);
    }

    public Diary findById(int id) {
        Optional<Diary> diaryOptional = diaryRepository.findById(id);
        return diaryOptional.orElse(null);
    }

    public ImageEntity findImageById(int id) {
        return imageRepository.findById(id);
    }

    public ContentEntity findContentById(int id) {
        return contentRepository.findById(id);
    }

    public ImageEntity saveImage(ImageEntity image) {
        return imageRepository.save(image);
    }

    public List<DiaryCard> getDiaryCards(String keyword, String searchType, int sortField, 
                                       String sortOrder, int page, int pageSize, Integer userId) {
        Specification<Diary> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (keyword != null && !keyword.isEmpty()) {
                if ("name".equals(searchType)) {
                    predicates.add(cb.like(root.get("title"), "%" + keyword + "%"));
                } else if ("destination".equals(searchType)) {
                    predicates.add(cb.like(root.get("destination"), "%" + keyword + "%"));
                }
            }
            
            if (userId != null) {
                predicates.add(cb.equal(root.get("userID"), userId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortFieldName;
        switch (sortField) {
            case 1: sortFieldName = "hot"; break;
            case 2: sortFieldName = "rating"; break;
            case 3: sortFieldName = "id"; break;
            default: sortFieldName = "hot";
        }
        
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(direction, sortFieldName));
        Page<Diary> diaryPage = diaryRepository.findAll(spec, pageable);
        
        return diaryPage.getContent().stream()
            .map(diary -> {
                DiaryCard card = new DiaryCard();
                card.setId(diary.getId());
                card.setTitle(diary.getTitle());
                card.setDescription(diary.getDescription());
                card.setDestination(diary.getDestination());
                card.setHot(diary.getHot());
                card.setRating(diary.getRating());
                card.setUserId(diary.getUserID());
                card.setImageIds(diary.getImage());
                if (diary.getTags() != null) {
                    card.setTags(Arrays.asList(diary.getTags().split(",")));
                }
                return card;
            })
            .collect(Collectors.toList());
    }

    public int getTotalDiaries(String keyword, String searchType, Integer userId) {
        Specification<Diary> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (keyword != null && !keyword.isEmpty()) {
                if ("name".equals(searchType)) {
                    predicates.add(cb.like(root.get("title"), "%" + keyword + "%"));
                } else if ("destination".equals(searchType)) {
                    predicates.add(cb.like(root.get("destination"), "%" + keyword + "%"));
                }
            }
            
            if (userId != null) {
                predicates.add(cb.equal(root.get("userID"), userId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return (int) diaryRepository.count(spec);
    }

    public Diary getDiary(Integer id) {
        return diaryRepository.findById(id).orElse(null);
    }

    public DiaryData getDiaryDataById(int id) {
        Diary diary = findById(id);
        if (diary == null) {
            return null;
        }
        
        DiaryData data = new DiaryData();
        data.setId(diary.getId());
        data.setTitle(diary.getTitle());
        data.setDescription(diary.getDescription());
        data.setDestination(diary.getDestination());
        data.setHot(diary.getHot());
        data.setRating(diary.getRating());
        data.setUserId(diary.getUserID());
        data.setImageIds(diary.getImage());
        
        ContentEntity content = findContentById(diary.getContentId());
        if (content != null) {
            data.setContent(content.getContent());
        }
        
        return data;
    }

    public boolean canManageDiary(Integer userId, Integer diaryId) {
        if (userId == null) return false;
        
        User user = userService.findById(userId);
        if (user == null) return false;
        
        // 管理员可以管理所有日记
        if ("ADMIN".equals(user.getRole())) return true;
        
        // 普通用户只能管理自己的日记
        Diary diary = findById(diaryId);
        return diary != null && diary.getUserID().equals(userId);
    }

    public void deleteDiary(int id, Integer userId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        
        User user = userService.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        Diary diary = findById(id);
        if (diary == null) {
            throw new RuntimeException("日记不存在");
        }
        
        // 检查用户角色
        if (!"ADMIN".equals(user.getRole()) && !diary.getUserID().equals(userId)) {
            throw new RuntimeException("没有权限删除此日记");
        }
        
        // 删除日记
        diaryRepository.deleteById(id);
        
        // 如果日记有关联的内容，也删除内容
        if (diary.getContentId() != null) {
            contentRepository.deleteById(diary.getContentId());
        }
        
        // 如果日记有关联的图片，也删除图片
        if (diary.getImage() != null && !diary.getImage().isEmpty()) {
            String[] imageIds = diary.getImage().split(",");
            for (String imageIdStr : imageIds) {
                try {
                    int imageId = Integer.parseInt(imageIdStr.trim());
                    imageRepository.deleteById(imageId);
                } catch (NumberFormatException e) {
                    // 忽略无效的图片ID
                }
            }
        }
        
        // 如果日记有关联的视频，也删除视频
        if (diary.getVideoId() != null) {
            try {
                videoService.deleteById(diary.getVideoId());
            } catch (Exception e) {
                // 忽略视频删除失败的错误
                System.out.println("删除视频失败: " + e.getMessage());
            }
        }
    }

    public Page<Diary> findAll(Specification<Diary> spec, Pageable pageable) {
        return diaryRepository.findAll(spec, pageable);
    }

    public List<ImageEntity> findAllImages() {
        return imageRepository.findAll();
    }
} 