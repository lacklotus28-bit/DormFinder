package com.rct.dormfinder.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_reviews")
public class CachedReview {
    
    @PrimaryKey
    @NonNull
    private String reviewId;
    
    private String dormId;
    private String dormName;
    private String studentId;
    private String studentName;
    private int rating;
    private String comment;
    private long reviewDate;
    private String landlordReply;
    private long replyDate;
    private long lastSyncTime;
    private boolean hasReply;
    
    public CachedReview() {
        this.reviewId = "";
        this.lastSyncTime = System.currentTimeMillis();
        this.hasReply = false;
    }
    
    // Getters and Setters
    @NonNull
    public String getReviewId() {
        return reviewId;
    }
    
    public void setReviewId(@NonNull String reviewId) {
        this.reviewId = reviewId;
    }
    
    public String getDormId() {
        return dormId;
    }
    
    public void setDormId(String dormId) {
        this.dormId = dormId;
    }
    
    public String getDormName() {
        return dormName;
    }
    
    public void setDormName(String dormName) {
        this.dormName = dormName;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public int getRating() {
        return rating;
    }
    
    public void setRating(int rating) {
        this.rating = rating;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public long getReviewDate() {
        return reviewDate;
    }
    
    public void setReviewDate(long reviewDate) {
        this.reviewDate = reviewDate;
    }
    
    public String getLandlordReply() {
        return landlordReply;
    }
    
    public void setLandlordReply(String landlordReply) {
        this.landlordReply = landlordReply;
    }
    
    public long getReplyDate() {
        return replyDate;
    }
    
    public void setReplyDate(long replyDate) {
        this.replyDate = replyDate;
    }
    
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    
    public void setLastSyncTime(long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
    
    public boolean isHasReply() {
        return hasReply;
    }
    
    public void setHasReply(boolean hasReply) {
        this.hasReply = hasReply;
    }
}
