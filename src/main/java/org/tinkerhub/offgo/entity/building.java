package org.tinkerhub.offgo.entity;

public class building {
    private int id;
    private String name;
    private int x;
    private int y;
    private String type;
    private String url;
    static int count = 0;
    public building(int id, String name, int x, int y, String type, String url) {
        this.id = count++;
        this.name = name;
        this.x = x;
        this.y = y;
        this.type = type;
        this.url = url;
    }
}
