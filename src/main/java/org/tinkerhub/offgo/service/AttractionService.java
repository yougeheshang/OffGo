package org.tinkerhub.offgo.service;

import org.springframework.data.domain.Sort;
import org.tinkerhub.offgo.entity.Attraction;
import org.tinkerhub.offgo.entity.Tag;
import org.tinkerhub.offgo.repository.AttractionRepository;
import org.tinkerhub.offgo.dao.TagDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class AttractionService {

    @Autowired
    private AttractionRepository attractionRepository;

    @Autowired
    private TagDao tagDao;

    @Autowired
    private UserService userService;

    private static final Random random = new Random();

    /**
     * 强制生成景点数据并覆盖现有数据
     *
     * @return 返回操作结果的描述信息
     */
    public String generateAttractions() {
        // 清空现有数据
        attractionRepository.deleteAll();

        // 生成新数据
        File folder = new File("src/main/resources/static/images/attraction");
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            List<Attraction> attractions = new ArrayList<>();
            for (int i = 0; i < Math.min(listOfFiles.length, 200); i++) {
                Attraction attraction = new Attraction();

                // 设置标题
                attraction.setTitle(generateRandomTitle());

                // 设置描述
                attraction.setDescription(generateRandomDescription());

                // 设置图片
                String imageFileName = listOfFiles[i].getName();
                attraction.setImage("/images/attraction/" + imageFileName);

                // 设置热度（1 到 1000）
                attraction.setHot(random.nextInt(1000) + 1);

                // 设置评分（1 到 5，保留一位小数）
                attraction.setRating(Math.round((random.nextDouble() * 4 + 1) * 10.0) / 10.0);

                // 设置区域类型（随机分配校区或景区）
                attraction.setAreaType(random.nextBoolean() ? "校区" : "景区");

                attractions.add(attraction);
            }
            attractionRepository.saveAll(attractions);
            return "景点数据已成功生成并覆盖现有数据...";
        }

        return "未找到图片资源，无法生成数据...";
    }

    private String generateRandomTitle() {
        String[] adjectives = {"美丽的", "神秘的", "壮观的", "迷人的", "宁静的", "古老的"};
        String[] nouns = {"山峰", "湖泊", "森林", "城市", "海滩", "古迹"};
        return adjectives[random.nextInt(adjectives.length)] +
                nouns[random.nextInt(nouns.length)];
    }

    private String generateRandomDescription() {
        String[] phrases = {
                "这里风景如画，让人流连忘返。",
                "适合放松心情，享受大自然的馈赠。",
                "历史悠久，文化底蕴深厚。",
                "是摄影爱好者的天堂。",
                "每年吸引成千上万的游客前来参观。",
                "周边有许多特色小吃和手工艺品。"
        };
        return phrases[random.nextInt(phrases.length)];
    }

    public List<Attraction> getAttractionCards(String keyword, String sortField, String sortOrder) {
        // 查询所有景点数据
        List<Attraction> attractions = attractionRepository.findAll();

        // 为每个景点获取标签并计算评分
        for (Attraction attraction : attractions) {
            // 获取标签
            List<Tag> tags = tagDao.findByAttractionId(attraction.getId());
            attraction.setTags(tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toList()));
            
            // 计算评分
            if (attraction.getRate_counts() != null && attraction.getRate_counts() > 0) {
                attraction.setRating((double) attraction.getRate_sum() / attraction.getRate_counts());
            } else {
                attraction.setRating(0.0);
            }
        }

        // 根据关键词过滤
        if (keyword != null && !keyword.isEmpty()) {
            attractions = attractions.stream()
                    .filter(attraction -> attraction.getTitle().contains(keyword))
                    .collect(Collectors.toList());
        }

        // 根据排序字段和排序方式排序
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        switch (sortField.toLowerCase()) {
            case "hot":
                attractions.sort((a1, a2) -> direction == Sort.Direction.ASC
                        ? Integer.compare(a1.getHot(), a2.getHot())
                        : Integer.compare(a2.getHot(), a1.getHot()));
                break;
            case "rating":
                attractions.sort((a1, a2) -> direction == Sort.Direction.ASC
                        ? Double.compare(a1.getRating(), a2.getRating())
                        : Double.compare(a2.getRating(), a1.getRating()));
                break;
            default: // 默认按名称排序
                attractions.sort((a1, a2) -> direction == Sort.Direction.ASC
                        ? a1.getTitle().compareTo(a2.getTitle())
                        : a2.getTitle().compareTo(a1.getTitle()));
                break;
        }

        return attractions;
    }

    /**
     * 增加景点热度
     */
    public void incrementHot(Long id) {
        Attraction attraction = attractionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attraction not found"));
        attraction.setHot(attraction.getHot() + 1);
        attractionRepository.save(attraction);
    }

    /**
     * 检查用户是否已对景点评分
     */
    public String checkRating(Integer userId, Long attractionId) {
        return userService.check_rate(userId, attractionId.intValue());
    }

    /**
     * 对景点进行评分
     */
    public Attraction rateAttraction(Long attractionId, Integer userId, Integer rating) {
        // 获取景点
        Attraction attraction = attractionRepository.findById(attractionId)
                .orElseThrow(() -> new RuntimeException("Attraction not found"));

        // 检查用户是否已经评分
        String existingRating = userService.check_rate(userId, attractionId.intValue());
        if (!existingRating.equals("False")) {
            throw new RuntimeException("User has already rated this attraction");
        }

        // 直接使用数据库中的值进行更新
        attraction.setRate_sum(attraction.getRate_sum() + rating);
        attraction.setRate_counts(attraction.getRate_counts() + 1);
        attraction.setRating((double) attraction.getRate_sum() / attraction.getRate_counts());
        attraction = attractionRepository.save(attraction);

        // 更新用户的评分记录
        userService.addRating(userId, attractionId.intValue(), rating);

        return attraction;
    }
}