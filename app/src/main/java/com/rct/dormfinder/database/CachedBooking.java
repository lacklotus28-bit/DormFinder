package com.rct.dormfinder.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_bookings")
public class CachedBooking {
    
    @PrimaryKey
    @NonNull
    private String bookingId;
    
    private String studentId;
    private String dormId;
    private String dormName;
    private String checkInDate;
    private String checkOutDate;
    private int numberOfRooms;
    private double totalPrice;
    private String status; // PENDING, CONFIRMED, CANCELLED, COMPLETED
    private String paymentStatus; // UNPAID, PAID, REFUNDED
    private String specialRequests;
    private long bookingDate;
    private long lastSyncTime;
    
    public CachedBooking() {
        this.bookingId = "";
        this.lastSyncTime = System.currentTimeMillis();
    }
    
    // Getters and Setters
    @NonNull
    public String getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(@NonNull String bookingId) {
        this.bookingId = bookingId;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
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
    
    public String getCheckInDate() {
        return checkInDate;
    }
    
    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }
    
    public String getCheckOutDate() {
        return checkOutDate;
    }
    
    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }
    
    public int getNumberOfRooms() {
        return numberOfRooms;
    }
    
    public void setNumberOfRooms(int numberOfRooms) {
        this.numberOfRooms = numberOfRooms;
    }
    
    public double getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getSpecialRequests() {
        return specialRequests;
    }
    
    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }
    
    public long getBookingDate() {
        return bookingDate;
    }
    
    public void setBookingDate(long bookingDate) {
        this.bookingDate = bookingDate;
    }
    
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    
    public void setLastSyncTime(long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
}
