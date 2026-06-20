package com.rct.dormfinder.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Enhanced DAO with advanced filtering and sorting capabilities
 * This extends the existing DormitoryDao with additional optimized queries
 */
@Dao
public interface DormitoryDao {
    
    // ============= EXISTING METHODS (DO NOT REMOVE) =============
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CachedDormitory dormitory);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedDormitory> dormitories);
    
    @Update
    void update(CachedDormitory dormitory);
    
    @Delete
    void delete(CachedDormitory dormitory);
    
    @Query("DELETE FROM cached_dormitories")
    void deleteAll();
    
    @Query("SELECT * FROM cached_dormitories ORDER BY name ASC")
    LiveData<List<CachedDormitory>> getAllDormitories();
    
    @Query("SELECT * FROM cached_dormitories ORDER BY name ASC")
    List<CachedDormitory> getAllDormitoriesSync();
    
    @Query("SELECT * FROM cached_dormitories WHERE dormId = :dormId LIMIT 1")
    CachedDormitory getDormitoryById(String dormId);
    
    @Query("SELECT * FROM cached_dormitories WHERE isAvailable = 1 ORDER BY name ASC")
    LiveData<List<CachedDormitory>> getAvailableDormitories();
    
    @Query("SELECT * FROM cached_dormitories WHERE isAvailable = 1 ORDER BY name ASC")
    List<CachedDormitory> getAvailableDormitoriesSync();
    
    // Search and Filter Queries
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "name LIKE '%' || :searchQuery || '%' OR " +
           "location LIKE '%' || :searchQuery || '%' OR " +
           "address LIKE '%' || :searchQuery || '%' " +
           "ORDER BY name ASC")
    List<CachedDormitory> searchDormitories(String searchQuery);
    
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "price >= :minPrice AND price <= :maxPrice AND " +
           "isAvailable = 1 " +
           "ORDER BY price ASC")
    List<CachedDormitory> filterByPrice(double minPrice, double maxPrice);
    
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "location LIKE '%' || :location || '%' AND " +
           "isAvailable = 1 " +
           "ORDER BY name ASC")
    List<CachedDormitory> filterByLocation(String location);
    
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "availableRooms > 0 AND " +
           "isAvailable = 1 " +
           "ORDER BY availableRooms DESC")
    List<CachedDormitory> getWithAvailableRooms();
    
    @Query("SELECT COUNT(*) FROM cached_dormitories")
    int getCount();
    
    @Query("SELECT * FROM cached_dormitories WHERE lastSyncTime < :timestamp")
    List<CachedDormitory> getOutdatedDormitories(long timestamp);
    
    // ============= ENHANCED METHODS (NEW ADDITIONS) =============
    
    /**
     * Advanced filter with multiple parameters and sorting
     * Pass empty string for city to ignore that filter
     * Pass 0 for minPrice and Double.MAX_VALUE for maxPrice to ignore price filter
     */
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "(:city = '' OR location = :city) AND " +
           "price BETWEEN :minPrice AND :maxPrice AND " +
           "(:showAvailableOnly = 0 OR (isAvailable = 1 AND availableRooms > 0)) " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'price_asc' THEN price END ASC, " +
           "CASE WHEN :sortBy = 'price_desc' THEN price END DESC, " +
           "CASE WHEN :sortBy = 'rooms_desc' THEN availableRooms END DESC, " +
           "name ASC")
    List<CachedDormitory> advancedFilter(
            String city,
            double minPrice,
            double maxPrice,
            int showAvailableOnly, // 1 for true, 0 for false
            String sortBy // Options: "price_asc", "price_desc", "rooms_desc", "name"
    );
    
    /**
     * Search with sorting options
     */
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "(name LIKE '%' || :searchQuery || '%' OR " +
           "location LIKE '%' || :searchQuery || '%' OR " +
           "address LIKE '%' || :searchQuery || '%') " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'price_asc' THEN price END ASC, " +
           "CASE WHEN :sortBy = 'price_desc' THEN price END DESC, " +
           "CASE WHEN :sortBy = 'rooms_desc' THEN availableRooms END DESC, " +
           "name ASC")
    List<CachedDormitory> searchDormitoriesWithSort(String searchQuery, String sortBy);
    
    /**
     * Get dormitories by price range (available only)
     */
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "price BETWEEN :minPrice AND :maxPrice AND " +
           "isAvailable = 1 AND availableRooms > 0 " +
           "ORDER BY price ASC")
    List<CachedDormitory> getDormitoriesByPriceRange(double minPrice, double maxPrice);
    
    /**
     * Get cheapest available dormitories
     */
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "isAvailable = 1 AND availableRooms > 0 " +
           "ORDER BY price ASC LIMIT :limit")
    List<CachedDormitory> getCheapestDormitories(int limit);
    
    /**
     * Get dormitories with most rooms available
     */
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "isAvailable = 1 AND availableRooms > 0 " +
           "ORDER BY availableRooms DESC LIMIT :limit")
    List<CachedDormitory> getMostAvailableDormitories(int limit);
    
    /**
     * Get dormitories by location with availability check
     */
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "location = :location AND " +
           "isAvailable = 1 AND availableRooms > 0 " +
           "ORDER BY price ASC")
    List<CachedDormitory> getAvailableDormitoriesByLocation(String location);
    
    /**
     * Count available dormitories by location
     */
    @Query("SELECT COUNT(*) FROM cached_dormitories WHERE " +
           "location = :location AND isAvailable = 1 AND availableRooms > 0")
    int countAvailableByLocation(String location);
    
    /**
     * Get all unique locations (cities)
     */
    @Query("SELECT DISTINCT location FROM cached_dormitories " +
           "WHERE location IS NOT NULL AND location != '' " +
           "ORDER BY location ASC")
    List<String> getAllLocations();
    
    /**
     * Get price statistics
     */
    @Query("SELECT MIN(price) FROM cached_dormitories WHERE isAvailable = 1")
    double getMinPrice();
    
    @Query("SELECT MAX(price) FROM cached_dormitories WHERE isAvailable = 1")
    double getMaxPrice();
    
    @Query("SELECT AVG(price) FROM cached_dormitories WHERE isAvailable = 1")
    double getAveragePrice();
    
    /**
     * Complex filter with amenity support
     * Note: This checks if amenities field contains the search term
     * For more accurate amenity filtering, you may need to check in code
     */
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "(:city = '' OR location = :city) AND " +
           "price BETWEEN :minPrice AND :maxPrice AND " +
           "(:showAvailableOnly = 0 OR (isAvailable = 1 AND availableRooms > 0)) AND " +
           "(:amenityFilter = '' OR amenities LIKE '%' || :amenityFilter || '%') " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'price_asc' THEN price END ASC, " +
           "CASE WHEN :sortBy = 'price_desc' THEN price END DESC, " +
           "CASE WHEN :sortBy = 'rooms_desc' THEN availableRooms END DESC, " +
           "name ASC")
    List<CachedDormitory> complexFilter(
            String city,
            double minPrice,
            double maxPrice,
            int showAvailableOnly,
            String amenityFilter, // Single amenity to check
            String sortBy
    );
    
    /**
     * Full-text search across all text fields with filters
     */
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "(name LIKE '%' || :query || '%' OR " +
           "location LIKE '%' || :query || '%' OR " +
           "address LIKE '%' || :query || '%' OR " +
           "description LIKE '%' || :query || '%') AND " +
           "(:showAvailableOnly = 0 OR (isAvailable = 1 AND availableRooms > 0)) " +
           "ORDER BY " +
           "CASE WHEN name LIKE :query || '%' THEN 1 " + // Exact match first
           "     WHEN name LIKE '%' || :query || '%' THEN 2 " + // Contains
           "     ELSE 3 END, " +
           "name ASC")
    List<CachedDormitory> fullTextSearch(String query, int showAvailableOnly);
    
    /**
     * Get recently synced dormitories
     */
    @Query("SELECT * FROM cached_dormitories " +
           "ORDER BY lastSyncTime DESC LIMIT :limit")
    List<CachedDormitory> getRecentlySynced(int limit);
    
    /**
     * Get dormitories near a location (basic text-based proximity)
     */
    @Query("SELECT * FROM cached_dormitories WHERE " +
           "location = :location AND " +
           "isAvailable = 1 AND availableRooms > 0 " +
           "ORDER BY " +
           "CASE WHEN address LIKE '%' || :landmark || '%' THEN 1 ELSE 2 END, " +
           "price ASC")
    List<CachedDormitory> getNearbyDormitories(String location, String landmark);
    
    /**
     * Batch operations for specific dormitories
     */
    @Query("SELECT * FROM cached_dormitories WHERE dormId IN (:dormIds)")
    List<CachedDormitory> getDormitoriesByIds(List<String> dormIds);
    
    @Query("DELETE FROM cached_dormitories WHERE dormId IN (:dormIds)")
    void deleteByIds(List<String> dormIds);
    
    /**
     * Get count of dormitories matching criteria
     */
    @Query("SELECT COUNT(*) FROM cached_dormitories WHERE " +
           "(:city = '' OR location = :city) AND " +
           "price BETWEEN :minPrice AND :maxPrice AND " +
           "(:showAvailableOnly = 0 OR (isAvailable = 1 AND availableRooms > 0))")
    int getCountByFilters(String city, double minPrice, double maxPrice, int showAvailableOnly);
}
