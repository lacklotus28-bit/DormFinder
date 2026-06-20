package com.rct.dormfinder.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DormitoryDao {
    
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
}
