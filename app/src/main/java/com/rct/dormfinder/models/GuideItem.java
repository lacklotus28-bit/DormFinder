package com.rct.dormfinder.models;

public class GuideItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_SECTION = 1;
    public static final int TYPE_STEP = 2;

    private String title;
    private String content;
    private int type;

    public GuideItem(String title, String content, int type) {
        this.title = title;
        this.content = content;
        this.type = type;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getType() {
        return type;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setType(int type) {
        this.type = type;
    }
}
