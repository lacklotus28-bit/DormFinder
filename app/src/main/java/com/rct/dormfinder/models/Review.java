package com.rct.dormfinder.models;

public class Review {
    private String reviewId;
    private String studentId;
    private String userId; // Same as studentId, for Firestore rules compatibility
    private String studentName;
    private String dormId;
    private float rating; // 1.0 to 5.0
    private String comment;
    private long datePosted;
    private boolean isVerified; // Student actually stayed here
    private String studentProfileImageUrl;
    
    // Landlord reply fields
    private String landlordReply;
    private long replyDate;
    private String landlordName;
    private boolean hasReply;

    public Review() {} // Required for Firestore

    public Review(String studentId, String studentName, String dormId, float rating, String comment) {
        this.studentId = studentId;
        this.userId = studentId; // Set userId same as studentId
        this.studentName = studentName;
        this.dormId = dormId;
        this.rating = rating;
        this.comment = comment;
        this.datePosted = System.currentTimeMillis();
        this.isVerified = false;
    }

    // Getters
    public String getReviewId() { return reviewId; }
    public String getStudentId() { return studentId; }
    public String getUserId() { return userId; } // For Firestore rules
    public String getStudentName() { return studentName; }
    public String getDormId() { return dormId; }
    public float getRating() { return rating; }
    public String getComment() { return comment; }
    public long getDatePosted() { return datePosted; }
    public boolean isVerified() { return isVerified; }
    public String getStudentProfileImageUrl() { return studentProfileImageUrl; }
    public String getLandlordReply() { return landlordReply; }
    public long getReplyDate() { return replyDate; }
    public String getLandlordName() { return landlordName; }
    public boolean hasReply() { return hasReply; }

    // Setters
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public void setStudentId(String studentId) { 
        this.studentId = studentId; 
        this.userId = studentId; // Keep userId in sync
    }
    public void setUserId(String userId) { 
        this.userId = userId;
        this.studentId = userId; // Keep studentId in sync
    }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setDormId(String dormId) { this.dormId = dormId; }
    public void setRating(float rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setDatePosted(long datePosted) { this.datePosted = datePosted; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setStudentProfileImageUrl(String studentProfileImageUrl) { 
        this.studentProfileImageUrl = studentProfileImageUrl; 
    }
    public void setLandlordReply(String landlordReply) { 
        this.landlordReply = landlordReply;
        this.hasReply = (landlordReply != null && !landlordReply.isEmpty());
    }
    public void setReplyDate(long replyDate) { this.replyDate = replyDate; }
    public void setLandlordName(String landlordName) { this.landlordName = landlordName; }
    public void setHasReply(boolean hasReply) { this.hasReply = hasReply; }
}
