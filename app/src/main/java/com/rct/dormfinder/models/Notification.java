package com.rct.dormfinder.models;

import com.google.firebase.Timestamp;

public class Notification {
    private String notificationId;
    private String userId;
    private String title;
    private String message;
    private String type; // "booking", "payment", "message", "review", "dormitory", "general"
    private String relatedId; // ID of related booking, payment, dormitory, etc.
    private boolean read;  // Changed from 'isRead' to 'read' for Firestore compatibility
    private Timestamp createdAt;
    private String imageUrl;
    private String actionUrl; // Deep link or activity to open

    public Notification() {
        // Required empty constructor for Firestore
    }

    public Notification(String userId, String title, String message, String type) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = false;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
    
    // Getter that matches Firestore field name 'isRead'
    public boolean getIsRead() {
        return read;
    }
    
    // Setter that matches Firestore field name 'isRead'
    public void setIsRead(boolean isRead) {
        this.read = isRead;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }
}
