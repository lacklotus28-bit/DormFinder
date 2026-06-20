package com.rct.dormfinder.models;

public class User {
    private String userId;
    private String email;
    private String name;
    private String contactNumber;
    private String userType; // "student" or "landlord"
    private String school;
    private String course;
    private long createdAt;
    private long updatedAt;
    private boolean isActive;
    private String profileImageUrl;
    private java.util.List<String> favoriteDormitories; // List of favorite dormitory IDs

    public User() {} // Required for Firestore

    public User(String userId, String email, String name, String userType) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.userType = userType;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isActive = true;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getUserType() {
        return userType;
    }

    public String getSchool() {
        return school;
    }

    public String getCourse() {
        return course;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public java.util.List<String> getFavoriteDormitories() {
        return favoriteDormitories;
    }

    public void setFavoriteDormitories(java.util.List<String> favoriteDormitories) {
        this.favoriteDormitories = favoriteDormitories;
    }
}