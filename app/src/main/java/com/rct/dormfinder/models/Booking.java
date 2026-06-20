package com.rct.dormfinder.models;

public class Booking {
    private String bookingId;
    private String studentId;
    private String landlordId;
    private String dormitoryId;
    private String dormitoryName;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    private String studentSchool;
    private String studentCourse;
    private String message;
    private String status; // "pending", "approved", "declined", "confirmed", "cancelled"
    private long requestDate;
    private long responseDate;
    private long confirmedDate; // When landlord confirms payment
    private String landlordResponse;
    private double monthlyPrice;
    private String roomType;
    private long moveInDate;
    private boolean isActive;
    private String paymentStatus; // "unpaid", "pending", "paid"
    private String paymentId;
    private long paymentDate;

    public Booking() {} // Required for Firestore

    public Booking(String studentId, String landlordId, String dormitoryId, String dormitoryName) {
        this.studentId = studentId;
        this.landlordId = landlordId;
        this.dormitoryId = dormitoryId;
        this.dormitoryName = dormitoryName;
        this.status = "pending";
        this.requestDate = System.currentTimeMillis();
        this.isActive = true;
    }

    // Getters
    public String getBookingId() { return bookingId; }
    public String getStudentId() { return studentId; }
    public String getLandlordId() { return landlordId; }
    public String getDormitoryId() { return dormitoryId; }
    public String getDormitoryName() { return dormitoryName; }
    public String getStudentName() { return studentName; }
    public String getStudentEmail() { return studentEmail; }
    public String getStudentPhone() { return studentPhone; }
    public String getStudentSchool() { return studentSchool; }
    public String getStudentCourse() { return studentCourse; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public long getRequestDate() { return requestDate; }
    public long getResponseDate() { return responseDate; }
    public long getConfirmedDate() { return confirmedDate; }
    public String getLandlordResponse() { return landlordResponse; }
    public double getMonthlyPrice() { return monthlyPrice; }
    public String getRoomType() { return roomType; }
    public long getMoveInDate() { return moveInDate; }
    public boolean isActive() { return isActive; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getPaymentId() { return paymentId; }
    public long getPaymentDate() { return paymentDate; }

    // Setters
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }
    public void setDormitoryId(String dormitoryId) { this.dormitoryId = dormitoryId; }
    public void setDormitoryName(String dormitoryName) { this.dormitoryName = dormitoryName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public void setStudentPhone(String studentPhone) { this.studentPhone = studentPhone; }
    public void setStudentSchool(String studentSchool) { this.studentSchool = studentSchool; }
    public void setStudentCourse(String studentCourse) { this.studentCourse = studentCourse; }
    public void setMessage(String message) { this.message = message; }
    public void setStatus(String status) { this.status = status; }
    public void setRequestDate(long requestDate) { this.requestDate = requestDate; }
    public void setResponseDate(long responseDate) { this.responseDate = responseDate; }
    public void setConfirmedDate(long confirmedDate) { this.confirmedDate = confirmedDate; }
    public void setLandlordResponse(String landlordResponse) { this.landlordResponse = landlordResponse; }
    public void setMonthlyPrice(double monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setMoveInDate(long moveInDate) { this.moveInDate = moveInDate; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public void setPaymentDate(long paymentDate) { this.paymentDate = paymentDate; }
}
