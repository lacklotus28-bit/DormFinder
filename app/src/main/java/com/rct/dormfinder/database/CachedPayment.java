package com.rct.dormfinder.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_payments")
public class CachedPayment {
    
    @PrimaryKey
    @NonNull
    private String paymentId;
    
    private String bookingId;
    private String studentId;
    private String dormName;
    private double amount;
    private String paymentMethod; // GCASH, PAYMAYA, CASH
    private String status; // PENDING, COMPLETED, FAILED, REFUNDED
    private String referenceNumber;
    private long paymentDate;
    private long dueDate;
    private String remarks;
    private long lastSyncTime;
    
    public CachedPayment() {
        this.paymentId = "";
        this.lastSyncTime = System.currentTimeMillis();
    }
    
    // Getters and Setters
    @NonNull
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(@NonNull String paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getDormName() {
        return dormName;
    }
    
    public void setDormName(String dormName) {
        this.dormName = dormName;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getReferenceNumber() {
        return referenceNumber;
    }
    
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
    
    public long getPaymentDate() {
        return paymentDate;
    }
    
    public void setPaymentDate(long paymentDate) {
        this.paymentDate = paymentDate;
    }
    
    public long getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    
    public void setLastSyncTime(long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
}
