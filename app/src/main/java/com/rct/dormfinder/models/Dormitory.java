package com.rct.dormfinder.models;

import java.util.List;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import android.util.Log;

public class Dormitory {
    private String dormId;
    private String landlordId;
    private String name;
    private String address;
    private String city; // "Batangas" or "Lipa"
    private double latitude;
    private double longitude;
    private String description;
    private Object monthlyPrice; // Can be String or double
    private List<String> amenities;
    private List<String> images;
    private int availableRooms;
    private int totalRooms;
    @PropertyName("isAvailable")
    private boolean isAvailable;
    
    // Support both Long (legacy) and Timestamp formats
    // Note: @ServerTimestamp removed because it doesn't work with Object type
    // For new documents, set these manually in your code
    private Object createdAt;  // Can be Long or Timestamp
    private Object updatedAt;  // Can be Long or Timestamp
    
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
        // Set timestamps for new documents
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
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
        if (monthlyPrice == null) return 0.0;
        if (monthlyPrice instanceof Double) {
            return (Double) monthlyPrice;
        } else if (monthlyPrice instanceof String) {
            try {
                return Double.parseDouble((String) monthlyPrice);
            } catch (NumberFormatException e) {
                Log.e("Dormitory", "Failed to parse monthlyPrice: " + monthlyPrice, e);
                return 0.0;
            }
        } else if (monthlyPrice instanceof Number) {
            return ((Number) monthlyPrice).doubleValue();
        }
        return 0.0;
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

    // Return Timestamp (handles both Long and Timestamp from Firestore)
    public Timestamp getCreatedAt() {
        if (createdAt == null) return null;
        if (createdAt instanceof Timestamp) {
            return (Timestamp) createdAt;
        } else if (createdAt instanceof Long) {
            return new Timestamp(((Long) createdAt) / 1000, 0);
        }
        return null;
    }
    
    // Helper method to get long value
    public long getCreatedAtMillis() {
        if (createdAt == null) return 0;
        if (createdAt instanceof Timestamp) {
            return ((Timestamp) createdAt).toDate().getTime();
        } else if (createdAt instanceof Long) {
            return (Long) createdAt;
        }
        return 0;
    }

    // Return Timestamp (handles both Long and Timestamp from Firestore)
    public Timestamp getUpdatedAt() {
        if (updatedAt == null) return null;
        if (updatedAt instanceof Timestamp) {
            return (Timestamp) updatedAt;
        } else if (updatedAt instanceof Long) {
            return new Timestamp(((Long) updatedAt) / 1000, 0);
        }
        return null;
    }
    
    // Helper method to get long value
    public long getUpdatedAtMillis() {
        if (updatedAt == null) return 0;
        if (updatedAt instanceof Timestamp) {
            return ((Timestamp) updatedAt).toDate().getTime();
        } else if (updatedAt instanceof Long) {
            return (Long) updatedAt;
        }
        return 0;
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

    public void setMonthlyPrice(Object monthlyPrice) {
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

    // Accept both Timestamp and Long
    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }
    
    // Accept both Timestamp and Long
    public void setUpdatedAt(Object updatedAt) {
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
