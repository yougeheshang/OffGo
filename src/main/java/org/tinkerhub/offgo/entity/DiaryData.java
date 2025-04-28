package org.tinkerhub.offgo.entity;

public class DiaryData {
    private int id;
    private String title;
    private String description;
    private int[] image;
    private int hot;
    private String destination;
    private double rating;
    private int rate_sum;
    private int rate_counts;
    private int authorID;
    private String content;

    // 构造函数
    public DiaryData(int id, String title, String description, int[] image, int hot, String destination, double rating, int rate_sum, int rate_counts, int authorID, String content) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.image = image;
        this.hot = hot;
        this.destination = destination;
        this.rating = rating;
        this.rate_sum = rate_sum;
        this.rate_counts = rate_counts;
        this.authorID = authorID;
        this.content = content;
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

    public int[] getImage() {
        return image;
    }

    public void setImage(int[] image) {
        this.image = image;
    }

    public int getHot() {
        return hot;
    }

    public void setHot(int hot) {
        this.hot = hot;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getRate_sum() {
        return rate_sum;
    }

    public void setRate_sum(int rate_sum) {
        this.rate_sum = rate_sum;
    }

    public int getRate_counts() {
        return rate_counts;
    }

    public void setRate_counts(int rate_counts) {
        this.rate_counts = rate_counts;
    }

    public int getAuthorID() {
        return authorID;
    }

    public void setAuthorID(int authorID) {
        this.authorID = authorID;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}