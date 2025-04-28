package org.tinkerhub.offgo.entity;

import java.util.List;

public class DiaryCard {
    private int id;
    private String title;
    private String description;
    private int[] images;
    private int hot;
    private double rating;
    private String destination;

    // 构造函数
    public DiaryCard(int id, String title, String description, int[] images, int hot, double rating, String destination) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.images = images;
        this.hot = hot;
        this.rating = rating;
        this.destination = destination;
    }

    // Getter 和 Setter 方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public int[] getImages() {
        return images;
    }

    public void setImages(int[] images) {
        this.images = images;
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

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
