/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tinkerhub.offgo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:chenxilzx1@gmail.com">theonefx</a>
 */
@Entity
@Table(name = "users")
@Data
public class User {
    @Column(name = "name")
    private String username;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "password")
    private String password;
    @Column(name = "role")
    private String role = "USER";  // 默认为普通用户
    
    @Column(name = "ai_video_url")
    private String aiVideoUrl;  // 添加AI视频URL字段
    
    @ElementCollection
    @CollectionTable(name = "user_rate_ids", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "rate_id")
    private List<Integer> rate_id;
    
    @ElementCollection
    @CollectionTable(name = "user_rate_values", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "rate_value")
    private List<Integer> rate_value;

    @Column(name = "interests")
    private String interests;

    public User() {
        this.rate_id = new ArrayList<>();
        this.rate_value = new ArrayList<>();
        this.role = "USER";
        this.aiVideoUrl = "";
    }

    public User(String name, String password, int id) {
        this.username = name;
        this.password = password;
        this.id = id;
        this.rate_id = new ArrayList<>();
        this.rate_value = new ArrayList<>();
        this.role = "USER";
        this.aiVideoUrl = "";
    }

    public User(String name, String password, int id, String role) {
        this.username = name;
        this.password = password;
        this.id = id;
        this.rate_id = new ArrayList<>();
        this.rate_value = new ArrayList<>();
        this.role = role;
        this.aiVideoUrl = "";
    }

    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public boolean checkPassword(String password) {
        System.out.println("Checking password - Input: " + password + ", Stored: " + this.password);
        return password.equals(this.password);
    }
    public boolean changePassword(String oldPassword, String newPassword) {
        if(checkPassword(oldPassword)) {
            this.password = newPassword;
            return true;
        }
        else {
            return false;
        }
    }
    public String getName() {
        return username;
    }

    public void setName(String name) {
        this.username = name;
    }

    public Integer getID() {
        return id;
    }

    public String check_rate(int diary_id) {
        if (rate_id == null || rate_value == null || rate_id.isEmpty() || rate_value.isEmpty()) {
            return "False";
        }
        for (int i = 0; i < rate_id.size(); i++) {
            if (rate_id.get(i) == diary_id) {
                return String.valueOf(rate_value.get(i));
            }
        }
        return "False";
    }

    public List<Integer> getRate_id() {
        if (rate_id == null) {
            rate_id = new ArrayList<>();
        }
        return rate_id;
    }

    public void setRate_id(List<Integer> rate_id) {
        this.rate_id = rate_id != null ? rate_id : new ArrayList<>();
    }

    public List<Integer> getRate_value() {
        if (rate_value == null) {
            rate_value = new ArrayList<>();
        }
        return rate_value;
    }

    public void setRate_value(List<Integer> rate_value) {
        this.rate_value = rate_value != null ? rate_value : new ArrayList<>();
    }

    public String getAiVideoUrl() {
        return aiVideoUrl != null ? aiVideoUrl : "";
    }

    public void setAiVideoUrl(String aiVideoUrl) {
        this.aiVideoUrl = aiVideoUrl != null ? aiVideoUrl : "";
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }
}
