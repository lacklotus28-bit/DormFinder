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
public interface BookingDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CachedBooking booking);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedBooking> bookings);
    
    @Update
    void update(CachedBooking booking);
    
    @Delete
    void delete(CachedBooking booking);
    
    @Query("DELETE FROM cached_bookings")
    void deleteAll();
    
    @Query("SELECT * FROM cached_bookings ORDER BY bookingDate DESC")
    LiveData<List<CachedBooking>> getAllBookings();
    
    @Query("SELECT * FROM cached_bookings ORDER BY bookingDate DESC")
    List<CachedBooking> getAllBookingsSync();
    
    @Query("SELECT * FROM cached_bookings WHERE bookingId = :bookingId LIMIT 1")
    CachedBooking getBookingById(String bookingId);
    
    @Query("SELECT * FROM cached_bookings WHERE studentId = :studentId ORDER BY bookingDate DESC")
    LiveData<List<CachedBooking>> getStudentBookings(String studentId);
    
    @Query("SELECT * FROM cached_bookings WHERE studentId = :studentId ORDER BY bookingDate DESC")
    List<CachedBooking> getStudentBookingsSync(String studentId);
    
    @Query("SELECT * FROM cached_bookings WHERE status = :status ORDER BY bookingDate DESC")
    List<CachedBooking> getBookingsByStatus(String status);
    
    @Query("SELECT * FROM cached_bookings WHERE dormId = :dormId ORDER BY bookingDate DESC")
    List<CachedBooking> getBookingsForDormitory(String dormId);
    
    @Query("SELECT COUNT(*) FROM cached_bookings")
    int getCount();
    
    @Query("SELECT * FROM cached_bookings WHERE lastSyncTime < :timestamp")
    List<CachedBooking> getOutdatedBookings(long timestamp);
}
