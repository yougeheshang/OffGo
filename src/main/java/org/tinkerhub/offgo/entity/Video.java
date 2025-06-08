package org.tinkerhub.offgo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "video")
@Data
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "video_path", nullable = false)
    private String videoPath;
    
    @Column(name = "content_type", nullable = false)
    private String contentType;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;
    
    @Column(name = "original_name")
    private String originalName;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "style")
    private String style;

    @Column(name = "type")
    private String type;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
} 