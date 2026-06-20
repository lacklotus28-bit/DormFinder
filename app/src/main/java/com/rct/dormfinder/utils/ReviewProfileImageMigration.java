package com.rct.dormfinder.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.models.Review;

public class ReviewProfileImageMigration {
    private static final String TAG = "ReviewMigration";
    private FirebaseFirestore db;
    private Context context;

    public ReviewProfileImageMigration(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Migrates all existing reviews to include studentProfileImageUrl field
     * This should be run once after updating the app to include profile images in reviews
     */
    public void migrateReviewProfileImages() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "Starting Review Profile Image Migration");
        Log.d(TAG, "========================================");

        // Get all reviews
        db.collection("reviews")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalReviews = queryDocumentSnapshots.size();
                    final int[] processedCount = {0};
                    final int[] updatedCount = {0};
                    final int[] alreadyHadImages = {0};
                    final int[] noProfileImages = {0};

                    Log.d(TAG, "Found " + totalReviews + " reviews to process");

                    if (totalReviews == 0) {
                        Log.d(TAG, "No reviews to migrate");
                        showToast("No reviews found to migrate");
                        return;
                    }

                    for (int i = 0; i < totalReviews; i++) {
                        String reviewId = queryDocumentSnapshots.getDocuments().get(i).getId();
                        Review review = queryDocumentSnapshots.getDocuments().get(i)
                                .toObject(Review.class);

                        if (review != null) {
                            String studentId = review.getStudentId();
                            String studentName = review.getStudentName();

                            // Check if profile image URL is already set
                            if (review.getStudentProfileImageUrl() != null &&
                                    !review.getStudentProfileImageUrl().isEmpty()) {
                                processedCount[0]++;
                                alreadyHadImages[0]++;
                                Log.d(TAG, "[" + processedCount[0] + "/" + totalReviews + "] " +
                                        "Review by " + studentName + " already has profile image ✓");

                                if (processedCount[0] == totalReviews) {
                                    logMigrationSummary(totalReviews, updatedCount[0], 
                                            alreadyHadImages[0], noProfileImages[0]);
                                }
                                continue;
                            }

                            // Fetch user's profile image URL
                            db.collection("users").document(studentId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String profileImageUrl = userDoc.getString("profileImageUrl");

                                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                                // Update review with profile image URL
                                                db.collection("reviews").document(reviewId)
                                                        .update("studentProfileImageUrl", profileImageUrl)
                                                        .addOnSuccessListener(aVoid -> {
                                                            updatedCount[0]++;
                                                            processedCount[0]++;
                                                            Log.d(TAG, "[" + processedCount[0] + "/" + 
                                                                    totalReviews + "] ✅ Updated review by " + 
                                                                    studentName + " with profile image");
                                                            Log.d(TAG, "   URL: " + profileImageUrl);

                                                            if (processedCount[0] == totalReviews) {
                                                                logMigrationSummary(totalReviews, updatedCount[0],
                                                                        alreadyHadImages[0], noProfileImages[0]);
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            processedCount[0]++;
                                                            Log.e(TAG, "[" + processedCount[0] + "/" + 
                                                                    totalReviews + "] ❌ Failed to update review by " + 
                                                                    studentName + ": " + e.getMessage());

                                                            if (processedCount[0] == totalReviews) {
                                                                logMigrationSummary(totalReviews, updatedCount[0],
                                                                        alreadyHadImages[0], noProfileImages[0]);
                                                            }
                                                        });
                                            } else {
                                                processedCount[0]++;
                                                noProfileImages[0]++;
                                                Log.d(TAG, "[" + processedCount[0] + "/" + totalReviews + 
                                                        "] ⚠️ User " + studentName + 
                                                        " has no profile image (will show default icon)");

                                                if (processedCount[0] == totalReviews) {
                                                    logMigrationSummary(totalReviews, updatedCount[0],
                                                            alreadyHadImages[0], noProfileImages[0]);
                                                }
                                            }
                                        } else {
                                            processedCount[0]++;
                                            Log.e(TAG, "[" + processedCount[0] + "/" + totalReviews + 
                                                    "] ❌ User not found: " + studentId);

                                            if (processedCount[0] == totalReviews) {
                                                logMigrationSummary(totalReviews, updatedCount[0],
                                                        alreadyHadImages[0], noProfileImages[0]);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        processedCount[0]++;
                                        Log.e(TAG, "[" + processedCount[0] + "/" + totalReviews + 
                                                "] ❌ Failed to fetch user " + studentId + ": " + 
                                                e.getMessage());

                                        if (processedCount[0] == totalReviews) {
                                            logMigrationSummary(totalReviews, updatedCount[0],
                                                    alreadyHadImages[0], noProfileImages[0]);
                                        }
                                    });
                        } else {
                            processedCount[0]++;
                            Log.e(TAG, "[" + processedCount[0] + "/" + totalReviews + 
                                    "] ❌ Could not parse review " + reviewId);

                            if (processedCount[0] == totalReviews) {
                                logMigrationSummary(totalReviews, updatedCount[0],
                                        alreadyHadImages[0], noProfileImages[0]);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to fetch reviews: " + e.getMessage());
                    showToast("Migration failed: " + e.getMessage());
                });
    }

    private void logMigrationSummary(int total, int updated, int alreadyHad, int noImages) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "Migration Complete!");
        Log.d(TAG, "========================================");
        Log.d(TAG, "Total reviews processed: " + total);
        Log.d(TAG, "✅ Reviews updated with images: " + updated);
        Log.d(TAG, "✓ Reviews already had images: " + alreadyHad);
        Log.d(TAG, "⚠️ Users with no profile images: " + noImages);
        Log.d(TAG, "========================================");

        String message = "Migration Complete!\n" +
                "Updated: " + updated + "\n" +
                "Already had images: " + alreadyHad + "\n" +
                "No profile images: " + noImages;
        showToast(message);
    }

    private void showToast(final String message) {
        if (context != null) {
            ((android.app.Activity) context).runOnUiThread(() ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            );
        }
    }
}
