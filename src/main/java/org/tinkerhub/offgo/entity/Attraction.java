package org.tinkerhub.offgo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "attraction")
@Data
public class Attraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String image;
    private int hot;
    private double rating;
    private String areaType;
    private Integer rate_sum;
    private Integer rate_counts;

    @Transient // 这个字段不会被持久化到数据库
    private List<String> tags;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public int getHot() {
        return hot;
    }
    public void setHot(int hot) {
        this.hot = hot;
    }
    public double getRating() {
        return rating;
    }
    public void setRating(double rating) {
        this.rating = rating;
    }
    public String getAreaType() {
        return areaType;
    }
    public void setAreaType(String areaType) {
        this.areaType = areaType;
    }
    public List<String> getTags() {
        return tags;
    }
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    public Integer getRate_sum() {
        return rate_sum;
    }
    public void setRate_sum(Integer rate_sum) {
        this.rate_sum = rate_sum;
    }
    public Integer getRate_counts() {
        return rate_counts;
    }
    public void setRate_counts(Integer rate_counts) {
        this.rate_counts = rate_counts;
    }
}