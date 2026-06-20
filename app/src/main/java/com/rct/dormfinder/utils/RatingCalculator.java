package com.rct.dormfinder.utils;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.models.Review;

/**
 * Utility class to recalculate dormitory ratings from reviews
 */
public class RatingCalculator {
    private static final String TAG = "RatingCalculator";

    /**
     * Recalculates and updates the average rating for a dormitory
     * @param dormitoryId The dormitory ID to recalculate
     */
    public static void recalculateRating(String dormitoryId) {
        if (dormitoryId == null || dormitoryId.isEmpty()) {
            Log.e(TAG, "Invalid dormitory ID");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        Log.d(TAG, "Recalculating rating for dormitory: " + dormitoryId);
        
        db.collection("reviews")
                .whereEqualTo("dormId", dormitoryId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalReviews = queryDocumentSnapshots.size();
                    float sum = 0f;
                    
                    for (int i = 0; i < totalReviews; i++) {
                        Review review = queryDocumentSnapshots.getDocuments().get(i)
                                .toObject(Review.class);
                        if (review != null) {
                            sum += review.getRating();
                        }
                    }
                    
                    float averageRating = totalReviews > 0 ? sum / totalReviews : 0f;
                    
                    Log.d(TAG, "Calculated rating: " + averageRating + " from " + totalReviews + " reviews");
                    
                    // Update dormitory document
                    db.collection("dormitories").document(dormitoryId)
                            .update(
                                    "averageRating", averageRating,
                                    "totalReviews", totalReviews
                            )
                            .addOnSuccessListener(aVoid -> 
                                    Log.d(TAG, "Rating updated successfully"))
                            .addOnFailureListener(e -> 
                                    Log.e(TAG, "Failed to update rating: " + e.getMessage()));
                })
                .addOnFailureListener(e -> 
                        Log.e(TAG, "Failed to fetch reviews: " + e.getMessage()));
    }

    /**
     * Recalculates ratings for all dormitories (admin function)
     */
    public static void recalculateAllRatings() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        Log.d(TAG, "Recalculating ratings for all dormitories...");
        
        db.collection("dormitories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalDorms = queryDocumentSnapshots.size();
                    Log.d(TAG, "Found " + totalDorms + " dormitories to recalculate");
                    
                    queryDocumentSnapshots.getDocuments().forEach(document -> {
                        String dormId = document.getId();
                        recalculateRating(dormId);
                    });
                })
                .addOnFailureListener(e -> 
                        Log.e(TAG, "Failed to fetch dormitories: " + e.getMessage()));
    }
}
