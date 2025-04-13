package org.tinkerhub.offgo.entity;
import java.util.ArrayList;

public class Site {
    private String name;
    private int id;
    private String url;
    private String type;
    private ArrayList<Integer> roads_id;
    public Site(String name, int id, String url, String type) {
        this.name = name;
        this.id = id;
        this.url = url;
        this.type = type;
    }
}
