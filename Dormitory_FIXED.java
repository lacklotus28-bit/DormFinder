package com.rct.dormfinder.models;

import java.util.List;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class Dormitory {
    private String dormId;
    private String landlordId;
    private String name;
    private String address;
    private String city; // "Batangas" or "Lipa"
    private double latitude;
    private double longitude;
    private String description;
    private double monthlyPrice;
    private List<String> amenities;
    private List<String> images;
    private int availableRooms;
    private int totalRooms;
    @PropertyName("isAvailable")
    private boolean isAvailable;
    
    // FIXED: Changed from long to Timestamp for Firestore compatibility
    @ServerTimestamp
    private Timestamp createdAt;
    
    @ServerTimestamp
    private Timestamp updatedAt;
    
    private float averageRating;
    private int totalReviews;

    public Dormitory() {} // Required for Firestore

    public Dormitory(String landlordId, String name, String address, String city, 
                     double latitude, double longitude, String description, 
                     double monthlyPrice, int totalRooms) {
        this.landlordId = landlordId;
        this.name = name;
        this.address = address;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.monthlyPrice = monthlyPrice;
        this.totalRooms = totalRooms;
        this.availableRooms = totalRooms;
        this.isAvailable = true;
        // Timestamps will be set by Firestore @ServerTimestamp
        this.averageRating = 0.0f;
        this.totalReviews = 0;
    }

    // Getters
    public String getDormId() {
        return dormId;
    }

    public String getLandlordId() {
        return landlordId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getDescription() {
        return description;
    }

    public double getMonthlyPrice() {
        return monthlyPrice;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public List<String> getImages() {
        return images;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    // FIXED: Return Timestamp instead of long
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    // Helper method to get long value if needed (for backward compatibility)
    public long getCreatedAtMillis() {
        return createdAt != null ? createdAt.toDate().getTime() : 0;
    }

    // FIXED: Return Timestamp instead of long
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    // Helper method to get long value if needed (for backward compatibility)
    public long getUpdatedAtMillis() {
        return updatedAt != null ? updatedAt.toDate().getTime() : 0;
    }

    public float getAverageRating() {
        return averageRating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public int getReviewCount() {
        return totalReviews;
    }

    // Setters
    public void setDormId(String dormId) {
        this.dormId = dormId;
    }

    public void setLandlordId(String landlordId) {
        this.landlordId = landlordId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMonthlyPrice(double monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public void setAvailableRooms(int availableRooms) {
        this.availableRooms = availableRooms;
        this.updatedAt = Timestamp.now();
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
        this.updatedAt = Timestamp.now();
    }

    // FIXED: Accept Timestamp instead of long
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    // FIXED: Accept Timestamp instead of long
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setAverageRating(float averageRating) {
        this.averageRating = averageRating;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    // Additional setters for Firestore compatibility
    public void setOccupancyPercentage(double occupancyPercentage) {
        // This is a calculated field, so we don't need to store it
        // But we need the setter for Firestore deserialization
    }

    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    @PropertyName("isAvailable")
    public boolean getIsAvailable() {
        return isAvailable;
    }

    // Helper methods
    public void decrementAvailableRooms() {
        if (availableRooms > 0) {
            availableRooms--;
            if (availableRooms == 0) {
                isAvailable = false;
            }
            updatedAt = Timestamp.now();
        }
    }

    public void incrementAvailableRooms() {
        if (availableRooms < totalRooms) {
            availableRooms++;
            if (!isAvailable && availableRooms > 0) {
                isAvailable = true;
            }
            updatedAt = Timestamp.now();
        }
    }

    public boolean hasRoomsAvailable() {
        return isAvailable && availableRooms > 0;
    }

    public double getOccupancyPercentage() {
        if (totalRooms == 0) return 0;
        return ((double)(totalRooms - availableRooms) / totalRooms) * 100;
    }

    @Override
    public String toString() {
        return "Dormitory{" +
                "dormId='" + dormId + '\'' +
                ", landlordId='" + landlordId + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", monthlyPrice=" + monthlyPrice +
                ", availableRooms=" + availableRooms +
                ", totalRooms=" + totalRooms +
                ", isAvailable=" + isAvailable +
                '}';
    }
}
