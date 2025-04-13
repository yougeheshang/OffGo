package org.tinkerhub.offgo.entity;

import jakarta.persistence.*;

@Entity
@Table(name="content")
public class ContentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String content;
    public ContentEntity() {}
    public ContentEntity(Integer diaryID, String diary_content) {
        this.id = diaryID;
        this.content = diary_content;
    }
}
