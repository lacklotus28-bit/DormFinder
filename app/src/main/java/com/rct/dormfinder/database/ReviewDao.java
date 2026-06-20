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
public interface ReviewDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CachedReview review);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedReview> reviews);
    
    @Update
    void update(CachedReview review);
    
    @Delete
    void delete(CachedReview review);
    
    @Query("DELETE FROM cached_reviews")
    void deleteAll();
    
    @Query("SELECT * FROM cached_reviews ORDER BY reviewDate DESC")
    LiveData<List<CachedReview>> getAllReviews();
    
    @Query("SELECT * FROM cached_reviews ORDER BY reviewDate DESC")
    List<CachedReview> getAllReviewsSync();
    
    @Query("SELECT * FROM cached_reviews WHERE reviewId = :reviewId LIMIT 1")
    CachedReview getReviewById(String reviewId);
    
    @Query("SELECT * FROM cached_reviews WHERE dormId = :dormId ORDER BY reviewDate DESC")
    LiveData<List<CachedReview>> getDormitoryReviews(String dormId);
    
    @Query("SELECT * FROM cached_reviews WHERE dormId = :dormId ORDER BY reviewDate DESC")
    List<CachedReview> getDormitoryReviewsSync(String dormId);
    
    @Query("SELECT * FROM cached_reviews WHERE studentId = :studentId ORDER BY reviewDate DESC")
    List<CachedReview> getStudentReviews(String studentId);
    
    @Query("SELECT * FROM cached_reviews WHERE rating >= :minRating ORDER BY reviewDate DESC")
    List<CachedReview> getReviewsByMinRating(int minRating);
    
    @Query("SELECT * FROM cached_reviews WHERE hasReply = 1 ORDER BY replyDate DESC")
    List<CachedReview> getReviewsWithReplies();
    
    @Query("SELECT COUNT(*) FROM cached_reviews WHERE dormId = :dormId")
    int getReviewCountForDormitory(String dormId);
    
    @Query("SELECT AVG(rating) FROM cached_reviews WHERE dormId = :dormId")
    float getAverageRatingForDormitory(String dormId);
    
    @Query("SELECT COUNT(*) FROM cached_reviews")
    int getCount();
    
    @Query("SELECT * FROM cached_reviews WHERE lastSyncTime < :timestamp")
    List<CachedReview> getOutdatedReviews(long timestamp);
}
