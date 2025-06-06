package org.tinkerhub.offgo.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "image")
public class ImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;


    private String imagepath;

    // 其他属性和方法
    public ImageEntity() {}

    public ImageEntity(int id, String imageData) {
        this.id = id;
        this.imagepath = imageData;
    }
    public int getId() {
        return id;
    }

    public String getImagepath() {
        return imagepath;
    }

    public void setImagepath(String imagepath) {
        this.imagepath = imagepath;
    }
}