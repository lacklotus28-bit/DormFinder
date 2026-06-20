package com.rct.dormfinder.models;

public class Payment {
    private String paymentId;
    private String bookingId;
    private String studentId;
    private String landlordId;
    private String dormitoryId;
    private String dormitoryName;
    private double amount;
    private String paymentMethod; // "gcash", "paymaya", "cash"
    private String status; // "pending", "completed", "failed", "refunded"
    private long timestamp;
    private String referenceNumber;
    private String transactionId; // From payment gateway
    private String studentName;
    private String studentEmail;
    private String description;
    private String receiptUrl; // URL to receipt image (for cash payments)
    private String paymentProof; // URL to payment proof image
    private long completedDate;
    private String failureReason;

    public Payment() {} // Required for Firestore

    public Payment(String bookingId, String studentId, String landlordId, String dormitoryId, double amount) {
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.landlordId = landlordId;
        this.dormitoryId = dormitoryId;
        this.amount = amount;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getPaymentId() { return paymentId; }
    public String getBookingId() { return bookingId; }
    public String getStudentId() { return studentId; }
    public String getLandlordId() { return landlordId; }
    public String getDormitoryId() { return dormitoryId; }
    public String getDormitoryName() { return dormitoryName; }
    public double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getTransactionId() { return transactionId; }
    public String getStudentName() { return studentName; }
    public String getStudentEmail() { return studentEmail; }
    public String getDescription() { return description; }
    public String getReceiptUrl() { return receiptUrl; }
    public String getPaymentProof() { return paymentProof; }
    public long getCompletedDate() { return completedDate; }
    public String getFailureReason() { return failureReason; }

    // Setters
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }
    public void setDormitoryId(String dormitoryId) { this.dormitoryId = dormitoryId; }
    public void setDormitoryName(String dormitoryName) { this.dormitoryName = dormitoryName; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public void setDescription(String description) { this.description = description; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
    public void setPaymentProof(String paymentProof) { this.paymentProof = paymentProof; }
    public void setCompletedDate(long completedDate) { this.completedDate = completedDate; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
