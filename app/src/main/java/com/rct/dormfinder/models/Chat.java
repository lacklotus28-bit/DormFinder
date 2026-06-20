package com.rct.dormfinder.models;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

public class Chat implements Serializable {
    private String chatId;
    private String studentId;
    private String landlordId;
    private String dormitoryId;
    private String dormitoryName;
    
    // Optional field (may exist in some Firestore documents)
    // Note: Field is named 'isActive' in Firestore, but Java convention uses 'active'
    private Boolean isActive = true;  // Use Boolean (not boolean) to allow null
    private String studentName;
    private String landlordName;
    private String studentProfileImageUrl;
    private String landlordProfileImageUrl;
    private String lastMessage;
    private long lastMessageTimestamp;
    
    // Separate unread counts for each user
    // ✅ Initialize with default values at field level (before constructor runs)
    private int studentUnreadCount = 0;  // Unread messages for student
    private int landlordUnreadCount = 0; // Unread messages for landlord
    
    private long createdAt;
    private long updatedAt;

    // Empty constructor (required for Firestore)
    public Chat() {
        android.util.Log.d("Chat", "🆕 Creating new Chat object (no-arg constructor)");
        // ⚠️ DON'T reset unread counts here - Firestore will set them via setters
        // Only initialize timestamp fields
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Full constructor
    public Chat(String studentId, String landlordId, String dormitoryId, String dormitoryName) {
        this.studentId = studentId;
        this.landlordId = landlordId;
        this.dormitoryId = dormitoryId;
        this.dormitoryName = dormitoryName;
        this.lastMessage = "";
        this.lastMessageTimestamp = System.currentTimeMillis();
        this.studentUnreadCount = 0;
        this.landlordUnreadCount = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getLandlordId() {
        return landlordId;
    }

    public void setLandlordId(String landlordId) {
        this.landlordId = landlordId;
    }

    public String getDormitoryId() {
        return dormitoryId;
    }

    public void setDormitoryId(String dormitoryId) {
        this.dormitoryId = dormitoryId;
    }

    public String getDormitoryName() {
        return dormitoryName;
    }

    public void setDormitoryName(String dormitoryName) {
        this.dormitoryName = dormitoryName;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        android.util.Log.d("Chat", "📝 Setting isActive to: " + isActive);
        this.isActive = isActive;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getLandlordName() {
        return landlordName;
    }

    public void setLandlordName(String landlordName) {
        this.landlordName = landlordName;
    }

    public String getStudentProfileImageUrl() {
        return studentProfileImageUrl;
    }

    public void setStudentProfileImageUrl(String studentProfileImageUrl) {
        this.studentProfileImageUrl = studentProfileImageUrl;
    }

    public String getLandlordProfileImageUrl() {
        return landlordProfileImageUrl;
    }

    public void setLandlordProfileImageUrl(String landlordProfileImageUrl) {
        this.landlordProfileImageUrl = landlordProfileImageUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public int getStudentUnreadCount() {
        android.util.Log.d("Chat", "📖 GETTING studentUnreadCount: " + studentUnreadCount);
        return studentUnreadCount;
    }

    public void setStudentUnreadCount(int studentUnreadCount) {
        android.util.Log.d("Chat", "📝 Setting studentUnreadCount FROM: " + this.studentUnreadCount + " TO: " + studentUnreadCount);
        this.studentUnreadCount = studentUnreadCount;
        android.util.Log.d("Chat", "✅ studentUnreadCount NOW IS: " + this.studentUnreadCount);
    }

    public int getLandlordUnreadCount() {
        return landlordUnreadCount;
    }

    public void setLandlordUnreadCount(int landlordUnreadCount) {
        android.util.Log.d("Chat", "📝 Setting landlordUnreadCount to: " + landlordUnreadCount);
        this.landlordUnreadCount = landlordUnreadCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Get the other participant's ID (not the current user)
     */
    public String getOtherParticipantId(String currentUserId) {
        if (currentUserId.equals(studentId)) {
            return landlordId;
        } else {
            return studentId;
        }
    }

    /**
     * Get the other participant's name
     */
    public String getOtherParticipantName(String currentUserId) {
        if (currentUserId.equals(studentId)) {
            return landlordName;
        } else {
            return studentName;
        }
    }

    /**
     * Get the other participant's profile image URL
     */
    public String getOtherParticipantImageUrl(String currentUserId) {
        if (currentUserId.equals(studentId)) {
            return landlordProfileImageUrl;
        } else {
            return studentProfileImageUrl;
        }
    }

    /**
     * Get unread count for a specific user
     */
    public int getUnreadCountForUser(String userId) {
        android.util.Log.d("Chat", "============= GET UNREAD COUNT =============");
        android.util.Log.d("Chat", "Chat: " + getDormitoryName());
        android.util.Log.d("Chat", "Checking for User ID: " + userId);
        android.util.Log.d("Chat", "Student ID: " + getStudentId());
        android.util.Log.d("Chat", "Landlord ID: " + getLandlordId());
        android.util.Log.d("Chat", "Student Unread: " + studentUnreadCount);
        android.util.Log.d("Chat", "Landlord Unread: " + landlordUnreadCount);
        
        if (userId == null) {
            android.util.Log.e("Chat", "❌ User ID is null!");
            return 0;
        }
        
        if (studentId != null && userId.equals(studentId)) {
            android.util.Log.d("Chat", "✅ User is STUDENT, returning: " + studentUnreadCount);
            return studentUnreadCount;
        } else if (landlordId != null && userId.equals(landlordId)) {
            android.util.Log.d("Chat", "✅ User is LANDLORD, returning: " + landlordUnreadCount);
            return landlordUnreadCount;
        } else {
            android.util.Log.w("Chat", "⚠️ User ID doesn't match student or landlord!");
            return 0;
        }
    }

    /**
     * Determine if current user is student or landlord
     */
    public boolean isStudent(String userId) {
        return userId.equals(studentId);
    }

    // Deprecated: Keep for backward compatibility, but use specific unread counts instead
    @Deprecated
    public int getUnreadCount() {
        android.util.Log.d("Chat", "⚠️ DEPRECATED getUnreadCount() called, returning: " + (studentUnreadCount + landlordUnreadCount));
        // Return total unread for backward compatibility
        return studentUnreadCount + landlordUnreadCount;
    }

    @Deprecated
    public void setUnreadCount(int unreadCount) {
        android.util.Log.e("Chat", "❌ DEPRECATED setUnreadCount() called with: " + unreadCount + " - THIS RESETS BOTH COUNTS!");
        android.util.Log.e("Chat", "   BEFORE: studentUnreadCount=" + this.studentUnreadCount + ", landlordUnreadCount=" + this.landlordUnreadCount);
        // For backward compatibility, set both counts
        this.studentUnreadCount = unreadCount;
        this.landlordUnreadCount = unreadCount;
        android.util.Log.e("Chat", "   AFTER: studentUnreadCount=" + this.studentUnreadCount + ", landlordUnreadCount=" + this.landlordUnreadCount);
    }
}
