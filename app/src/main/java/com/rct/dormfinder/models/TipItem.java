package com.rct.dormfinder.models;

public class TipItem {
    private String title;
    private String subtitle;
    private int iconResId;

    public TipItem(String title, String subtitle, int iconResId) {
        this.title = title;
        this.subtitle = subtitle;
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getIconResId() {
        return iconResId;
    }
}
