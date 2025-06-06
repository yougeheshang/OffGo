package org.tinkerhub.offgo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "diary")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Diary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String title;
    private String description;
    
    @Column(name = "content")
    private Integer contentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ContentEntity content;
    
    @Column(name = "image")
    private String image;
    
    private Integer hot;
    private String destination;
    private Double rating;
    private Integer rate_sum;
    private Integer rate_counts;
    private Integer userID;
    
    @Column(name = "video_id")
    private Long videoId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Video video;

    @Column(name = "tags")
    private String tags; // 存储标签，用逗号分隔
} 