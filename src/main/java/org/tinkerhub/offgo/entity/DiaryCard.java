package org.tinkerhub.offgo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryCard {
    private Integer id;
    private String title;
    private String description;
    private String destination;
    private Integer hot;
    private Double rating;
    private Integer userId;
    private String imageIds;
    private List<String> tags;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public Integer getHot() { return hot; }
    public void setHot(Integer hot) { this.hot = hot; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getImageIds() { return imageIds; }
    public void setImageIds(String imageIds) { this.imageIds = imageIds; }
}
