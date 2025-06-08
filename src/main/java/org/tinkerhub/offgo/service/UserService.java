package org.tinkerhub.offgo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tinkerhub.offgo.entity.User;
import org.tinkerhub.offgo.repository.UserRepository;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findById(Integer id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserById(int id) {
        return findById(id);
    }

    public User getUserByName(String username) {
        return userRepository.findByUsername(username);
    }

    public int FindMaxID() {
        return userRepository.findMaxId();
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void deleteall() {
        userRepository.deleteAll();
    }

    public String check_rate(Integer userId, int diaryId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            return user.check_rate(diaryId);
        } catch (Exception e) {
            logger.error("检查用户评分失败", e);
            return "False";
        }
    }

    public void addRating(int userId, int diaryId, int rating) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // 获取现有的评分列表
            List<Integer> rateIds = user.getRate_id();
            List<Integer> rateValues = user.getRate_value();
            
            // 添加新的评分
            rateIds.add(diaryId);
            rateValues.add(rating);
            
            // 更新用户的评分记录
            user.setRate_id(rateIds);
            user.setRate_value(rateValues);
            
            // 保存更新
            userRepository.save(user);
            logger.info("用户 {} 对日记 {} 的评分 {} 已保存", userId, diaryId, rating);
        } catch (Exception e) {
            logger.error("保存用户评分失败", e);
            throw new RuntimeException("保存评分失败: " + e.getMessage());
        }
    }

    public String getUserVideoURL(Long userId) {
        try {
            User user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> new RuntimeException("User not found"));
            return user.getAiVideoUrl();
        } catch (Exception e) {
            logger.error("获取用户视频URL失败", e);
            return "";
        }
    }

    public void updateUserVideoURL(Long userId, String videoUrl) {
        try {
            User user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> new RuntimeException("User not found"));
            user.setAiVideoUrl(videoUrl);
            userRepository.save(user);
        } catch (Exception e) {
            logger.error("更新用户视频URL失败", e);
            throw new RuntimeException("更新视频URL失败");
        }
    }

    public String getUserInterests(String username) {
        User user = userRepository.findByUsername(username);
        return user != null ? user.getInterests() : "";
    }

    public void setUserInterests(String username, java.util.List<String> interests) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            user.setInterests(String.join(",", interests));
            userRepository.save(user);
        }
    }
} 