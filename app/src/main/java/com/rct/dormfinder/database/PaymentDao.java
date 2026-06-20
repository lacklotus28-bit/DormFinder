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
public interface PaymentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CachedPayment payment);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedPayment> payments);
    
    @Update
    void update(CachedPayment payment);
    
    @Delete
    void delete(CachedPayment payment);
    
    @Query("DELETE FROM cached_payments")
    void deleteAll();
    
    @Query("SELECT * FROM cached_payments ORDER BY paymentDate DESC")
    LiveData<List<CachedPayment>> getAllPayments();
    
    @Query("SELECT * FROM cached_payments ORDER BY paymentDate DESC")
    List<CachedPayment> getAllPaymentsSync();
    
    @Query("SELECT * FROM cached_payments WHERE paymentId = :paymentId LIMIT 1")
    CachedPayment getPaymentById(String paymentId);
    
    @Query("SELECT * FROM cached_payments WHERE studentId = :studentId ORDER BY paymentDate DESC")
    LiveData<List<CachedPayment>> getStudentPayments(String studentId);
    
    @Query("SELECT * FROM cached_payments WHERE studentId = :studentId ORDER BY paymentDate DESC")
    List<CachedPayment> getStudentPaymentsSync(String studentId);
    
    @Query("SELECT * FROM cached_payments WHERE bookingId = :bookingId")
    List<CachedPayment> getPaymentsForBooking(String bookingId);
    
    @Query("SELECT * FROM cached_payments WHERE status = :status ORDER BY paymentDate DESC")
    List<CachedPayment> getPaymentsByStatus(String status);
    
    @Query("SELECT * FROM cached_payments WHERE paymentMethod = :method ORDER BY paymentDate DESC")
    List<CachedPayment> getPaymentsByMethod(String method);
    
    @Query("SELECT SUM(amount) FROM cached_payments WHERE studentId = :studentId AND status = 'COMPLETED'")
    double getTotalAmountPaidByStudent(String studentId);
    
    @Query("SELECT COUNT(*) FROM cached_payments WHERE status = 'PENDING'")
    int getPendingPaymentCount();
    
    @Query("SELECT COUNT(*) FROM cached_payments")
    int getCount();
    
    @Query("SELECT * FROM cached_payments WHERE lastSyncTime < :timestamp")
    List<CachedPayment> getOutdatedPayments(long timestamp);
}
