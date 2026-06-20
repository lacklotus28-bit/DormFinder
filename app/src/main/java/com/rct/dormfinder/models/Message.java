package com.rct.dormfinder.models;

public class Message {
    private String messageId;
    private String chatId;
    private String senderId;
    private String receiverId;
    private String content;
    private long timestamp;
    private boolean isRead;
    private String messageType; // "text", "image", "booking_request"

    public Message() {} // Required for Firestore

    public Message(String chatId, String senderId, String receiverId, String content) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.messageType = "text";
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean getIsRead() { return isRead; }
    public void setIsRead(boolean isRead) { this.isRead = isRead; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}
