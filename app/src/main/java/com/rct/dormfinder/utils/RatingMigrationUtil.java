package com.rct.dormfinder.utils;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.models.Review;
import java.util.HashMap;
import java.util.Map;

/**
 * [MIGRATION COMPLETE - ARCHIVED]
 * This utility was used for a one-time migration to recalculate all dormitory ratings.
 * The migration has been completed and this class is kept for reference only.
 * 
 * Note: Going forward, ratings are automatically updated when reviews are added/deleted
 * via AddReviewActivity.recalculateDormRating() method.
 * 
 * @deprecated Migration completed. Use AddReviewActivity.recalculateDormRating() for ongoing updates.
 */
@Deprecated
public class RatingMigrationUtil {
    private static final String TAG = "RatingMigration";
    private FirebaseFirestore db;

    public RatingMigrationUtil() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Update ratings for all dormitories by recalculating from reviews
     */
    public void updateAllDormitoryRatings(OnMigrationCompleteListener listener) {
        Log.d(TAG, "Starting rating migration for all dormitories...");
        
        // Get all dormitories
        db.collection("dormitories")
                .get()
                .addOnSuccessListener(dormSnapshot -> {
                    int totalDorms = dormSnapshot.size();
                    int[] processedCount = {0};
                    int[] updatedCount = {0};
                    
                    Log.d(TAG, "Found " + totalDorms + " dormitories to process");
                    
                    if (totalDorms == 0) {
                        listener.onComplete(true, 0, 0);
                        return;
                    }
                    
                    for (int i = 0; i < dormSnapshot.size(); i++) {
                        String dormId = dormSnapshot.getDocuments().get(i).getId();
                        
                        // Get all reviews for this dormitory
                        db.collection("reviews")
                                .whereEqualTo("dormId", dormId)
                                .get()
                                .addOnSuccessListener(reviewSnapshot -> {
                                    int reviewCount = reviewSnapshot.size();
                                    float totalRating = 0f;
                                    
                                    // Calculate average rating
                                    for (int j = 0; j < reviewCount; j++) {
                                        Review review = reviewSnapshot.getDocuments().get(j)
                                                .toObject(Review.class);
                                        if (review != null) {
                                            totalRating += review.getRating();
                                        }
                                    }
                                    
                                    float averageRating = reviewCount > 0 ? totalRating / reviewCount : 0f;
                                    
                                    // Update dormitory
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("averageRating", averageRating);
                                    updates.put("totalReviews", reviewCount);
                                    
                                    db.collection("dormitories").document(dormId)
                                            .update(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "Updated " + dormId + " - Rating: " + averageRating + ", Reviews: " + reviewCount);
                                                processedCount[0]++;
                                                if (reviewCount > 0) {
                                                    updatedCount[0]++;
                                                }
                                                
                                                // Check if all dormitories processed
                                                if (processedCount[0] == totalDorms) {
                                                    Log.d(TAG, "Migration complete! Updated " + updatedCount[0] + " dormitories with reviews");
                                                    listener.onComplete(true, totalDorms, updatedCount[0]);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Failed to update " + dormId + ": " + e.getMessage());
                                                processedCount[0]++;
                                                
                                                // Check if all dormitories processed
                                                if (processedCount[0] == totalDorms) {
                                                    listener.onComplete(false, totalDorms, updatedCount[0]);
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to get reviews for " + dormId + ": " + e.getMessage());
                                    processedCount[0]++;
                                    
                                    // Check if all dormitories processed
                                    if (processedCount[0] == totalDorms) {
                                        listener.onComplete(false, totalDorms, updatedCount[0]);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get dormitories: " + e.getMessage());
                    listener.onComplete(false, 0, 0);
                });
    }

    /**
     * Update rating for a specific dormitory
     */
    public void updateDormitoryRating(String dormId, OnSingleUpdateListener listener) {
        db.collection("reviews")
                .whereEqualTo("dormId", dormId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int reviewCount = querySnapshot.size();
                    float totalRating = 0f;
                    
                    for (int i = 0; i < reviewCount; i++) {
                        Review review = querySnapshot.getDocuments().get(i)
                                .toObject(Review.class);
                        if (review != null) {
                            totalRating += review.getRating();
                        }
                    }
                    
                    float averageRating = reviewCount > 0 ? totalRating / reviewCount : 0f;
                    
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("averageRating", averageRating);
                    updates.put("totalReviews", reviewCount);
                    
                    db.collection("dormitories").document(dormId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Updated rating for " + dormId + " - " + averageRating + " (" + reviewCount + " reviews)");
                                listener.onComplete(true, averageRating, reviewCount);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update rating for " + dormId + ": " + e.getMessage());
                                listener.onComplete(false, 0, 0);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get reviews for " + dormId + ": " + e.getMessage());
                    listener.onComplete(false, 0, 0);
                });
    }

    public interface OnMigrationCompleteListener {
        void onComplete(boolean success, int totalProcessed, int withReviews);
    }

    public interface OnSingleUpdateListener {
        void onComplete(boolean success, float rating, int reviewCount);
    }
}
