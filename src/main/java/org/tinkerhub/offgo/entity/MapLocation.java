package org.tinkerhub.offgo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "map_location")
public class MapLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private String type;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "vector_icon", columnDefinition = "TEXT")
    private String vectorIcon;

    @Column(name = "category")
    private String category;  // 大类：如"建筑物"、"服务设施"

    @Column(name = "crowd_level")
    private Integer crowdLevel;  // 0: 一般, 1: 拥挤, 2: 十分拥挤

    @Column(name = "is_start_point")
    private Boolean isStartPoint = false;

    @Column(name = "is_path_point")
    private Boolean isPathPoint = false;

    // 判断是否为建筑物
    public boolean isBuilding() {
        return "建筑物".equals(category);
    }

    // 判断是否为服务设施
    public boolean isServiceFacility() {
        return "服务设施".equals(category);
    }

    // 获取建筑物类型
    public String getBuildingType() {
        if (!isBuilding()) {
            return null;
        }
        return type;
    }

    // 获取服务设施类型
    public String getServiceType() {
        if (!isServiceFacility()) {
            return null;
        }
        return type;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVectorIcon() {
        return vectorIcon;
    }

    public void setVectorIcon(String vectorIcon) {
        this.vectorIcon = vectorIcon;
    }

    public Integer getCrowdLevel() {
        return crowdLevel;
    }

    public void setCrowdLevel(Integer crowdLevel) {
        this.crowdLevel = crowdLevel;
    }

    public Boolean getIsStartPoint() {
        return isStartPoint;
    }

    public void setIsStartPoint(Boolean isStartPoint) {
        this.isStartPoint = isStartPoint;
    }

    public Boolean getIsPathPoint() {
        return isPathPoint;
    }

    public void setIsPathPoint(Boolean isPathPoint) {
        this.isPathPoint = isPathPoint;
    }
} 