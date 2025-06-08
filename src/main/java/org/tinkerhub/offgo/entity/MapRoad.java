package org.tinkerhub.offgo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "map_road")
public class MapRoad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "road_type")
    private String roadType;

    @Column(name = "start_point_id")
    private Long startPointId;

    @Column(name = "end_point_id")
    private Long endPointId;

    @Column(name = "path_points", columnDefinition = "TEXT")
    private String pathPoints;

    @Column(name = "description")
    private String description;

    @Column(name = "crowd_level")
    private Double crowdLevel;

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

    public String getRoadType() {
        return roadType;
    }

    public void setRoadType(String roadType) {
        this.roadType = roadType;
    }

    public Long getStartPointId() {
        return startPointId;
    }

    public void setStartPointId(Long startPointId) {
        this.startPointId = startPointId;
    }

    public Long getEndPointId() {
        return endPointId;
    }

    public void setEndPointId(Long endPointId) {
        this.endPointId = endPointId;
    }

    public String getPathPoints() {
        return pathPoints;
    }

    public void setPathPoints(String pathPoints) {
        this.pathPoints = pathPoints;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getCrowdLevel() {
        return crowdLevel;
    }

    public void setCrowdLevel(Double crowdLevel) {
        this.crowdLevel = crowdLevel;
    }
} 