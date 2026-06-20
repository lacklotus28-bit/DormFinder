package com.rct.dormfinder.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.List;

@Entity(tableName = "cached_dormitories")
@TypeConverters(Converters.class)
public class CachedDormitory {
    
    @PrimaryKey
    @NonNull
    private String dormId;
    
    private String name;
    private String description;
    private String address;
    private String location;
    private double latitude;
    private double longitude;
    private double price;
    private int totalRooms;
    private int availableRooms;
    private String landlordId;
    private String landlordName;
    private List<String> amenities;
    private List<String> imageUrls;
    private String rules;
    private long lastSyncTime;
    private boolean isAvailable;
    private float averageRating;
    private int totalReviews;
    
    // Constructor
    public CachedDormitory() {
        this.dormId = ""; // Initialize with empty string to satisfy @NonNull
        this.lastSyncTime = System.currentTimeMillis();
    }
    
    // Getters and Setters
    @NonNull
    public String getDormId() {
        return dormId;
    }
    
    public void setDormId(@NonNull String dormId) {
        this.dormId = dormId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public int getTotalRooms() {
        return totalRooms;
    }
    
    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }
    
    public int getAvailableRooms() {
        return availableRooms;
    }
    
    public void setAvailableRooms(int availableRooms) {
        this.availableRooms = availableRooms;
    }
    
    public String getLandlordId() {
        return landlordId;
    }
    
    public void setLandlordId(String landlordId) {
        this.landlordId = landlordId;
    }
    
    public String getLandlordName() {
        return landlordName;
    }
    
    public void setLandlordName(String landlordName) {
        this.landlordName = landlordName;
    }
    
    public List<String> getAmenities() {
        return amenities;
    }
    
    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
    
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    public String getRules() {
        return rules;
    }
    
    public void setRules(String rules) {
        this.rules = rules;
    }
    
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    
    public void setLastSyncTime(long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
    
    public boolean isAvailable() {
        return isAvailable;
    }
    
    public void setAvailable(boolean available) {
        isAvailable = available;
    }
    
    public float getAverageRating() {
        return averageRating;
    }
    
    public void setAverageRating(float averageRating) {
        this.averageRating = averageRating;
    }
    
    public int getTotalReviews() {
        return totalReviews;
    }
    
    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }
}
