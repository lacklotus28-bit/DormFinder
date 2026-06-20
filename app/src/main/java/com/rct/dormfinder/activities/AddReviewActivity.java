package com.rct.dormfinder.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Review;
import com.rct.dormfinder.models.User;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.utils.ConfirmationDialogHelper;
import com.rct.dormfinder.utils.NotificationHelper;

public class AddReviewActivity extends AppCompatActivity {
    private static final String TAG = "AddReviewActivity";
    private TextView tvDormName, tvRatingText;
    private RatingBar ratingBar;
    private EditText etComment;
    private MaterialButton btnSubmit, btnCancel;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String dormId;
    private String dormName;
    private String currentUserId;
    private String currentUserName;
    private String currentUserProfileImageUrl;
    private String landlordId; // Store landlord ID for notification

    private final String[] ratingTexts = {
        "Tap to rate",
        "Poor",
        "Fair", 
        "Good",
        "Very Good",
        "Excellent"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_review);
        
        // Set status bar color to match header
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.mint_primary));
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        // Get dorm info from intent
        dormId = getIntent().getStringExtra("dormId");
        dormName = getIntent().getStringExtra("dormName");

        Log.d(TAG, "Creating review for dorm: " + dormId);

        initializeViews();
        setupToolbar();
        loadUserInfo();
        loadDormInfo(); // Load dormitory info to get landlordId
        setupListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvDormName = findViewById(R.id.tvDormName);
        tvRatingText = findViewById(R.id.tvRatingText);
        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etComment);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnCancel = findViewById(R.id.btnCancel);

        tvDormName.setText(dormName);
        ratingBar.setRating(0);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Write Review");
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserInfo() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            currentUserName = user.getName();
                            currentUserProfileImageUrl = user.getProfileImageUrl();
                            Log.d(TAG, "User name loaded: " + currentUserName);
                            Log.d(TAG, "User profile image URL: " + currentUserProfileImageUrl);
                        }
                    }
                });
    }

    private void loadDormInfo() {
        db.collection("dormitories").document(dormId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Dormitory dormitory = documentSnapshot.toObject(Dormitory.class);
                        if (dormitory != null) {
                            landlordId = dormitory.getLandlordId();
                            Log.d(TAG, "Landlord ID loaded: " + landlordId);
                        } else {
                            Log.e(TAG, "Dormitory object is null");
                        }
                    } else {
                        Log.e(TAG, "Dormitory document does not exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load dormitory info: " + e.getMessage());
                });
    }

    private void setupListeners() {
        // Rating bar listener with animation
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                animateRatingBar();
                updateRatingText((int) rating);
            }
        });

        // Text input listener for character count feedback
        etComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Auto-enable/disable submit based on content
                updateSubmitButtonState();
            }
        });

        btnSubmit.setOnClickListener(v -> submitReview());
        btnCancel.setOnClickListener(v -> handleCancelPress());
    }

    private void animateRatingBar() {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.15f, 1.0f, 1.15f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(150);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        ratingBar.startAnimation(scaleAnimation);
    }

    private void updateRatingText(int rating) {
        if (rating >= 0 && rating < ratingTexts.length) {
            tvRatingText.setText(ratingTexts[rating]);
            tvRatingText.setVisibility(rating > 0 ? TextView.VISIBLE : TextView.GONE);
        }
    }

    private void updateSubmitButtonState() {
        boolean hasRating = ratingBar.getRating() > 0;
        boolean hasComment = !etComment.getText().toString().trim().isEmpty();
        btnSubmit.setEnabled(hasRating && hasComment);
        btnSubmit.setAlpha(btnSubmit.isEnabled() ? 1.0f : 0.5f);
    }

    private void handleCancelPress() {
        // Check if user has entered any data
        boolean hasData = ratingBar.getRating() > 0 || 
                         !etComment.getText().toString().trim().isEmpty();

        if (hasData) {
            ConfirmationDialogHelper.showCancelReviewDialog(this,
                    new ConfirmationDialogHelper.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            finish();
                        }

                        @Override
                        public void onCancel() {
                            // User wants to continue, do nothing
                        }
                    });
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleCancelPress();
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        String comment = etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate minimum comment length
        if (comment.length() < 20) {
            Toast.makeText(this, "Please write a more detailed review (at least 20 characters)", 
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");
        btnSubmit.setIcon(null);

        Log.d(TAG, "Submitting review with rating: " + rating);

        // Create review object
        Review review = new Review(currentUserId, currentUserName, dormId, rating, comment);
        
        // Set profile image URL if available
        if (currentUserProfileImageUrl != null && !currentUserProfileImageUrl.isEmpty()) {
            review.setStudentProfileImageUrl(currentUserProfileImageUrl);
            Log.d(TAG, "Setting profile image URL for review: " + currentUserProfileImageUrl);
        } else {
            Log.d(TAG, "No profile image URL available for this user");
        }

        // Check if user has stayed here (has approved booking)
        checkIfVerified(review);
    }

    private void checkIfVerified(Review review) {
        db.collection("bookings")
                .whereEqualTo("studentId", currentUserId)
                .whereEqualTo("dormitoryId", dormId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean hasStayed = !queryDocumentSnapshots.isEmpty();
                    review.setVerified(hasStayed);
                    Log.d(TAG, "Review verified status: " + hasStayed);
                    saveReview(review);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check verification: " + e.getMessage());
                    review.setVerified(false);
                    saveReview(review);
                });
    }

    private void saveReview(Review review) {
        DocumentReference reviewRef = db.collection("reviews").document();
        review.setReviewId(reviewRef.getId());

        Log.d(TAG, "Saving review with ID: " + review.getReviewId());

        reviewRef.set(review)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Review saved successfully!");
                    
                    // Send notification to landlord
                    sendReviewNotificationToLandlord(review.getRating());
                    
                    // Recalculate rating from all reviews (more accurate)
                    recalculateDormRating();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save review: " + e.getMessage());
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Review");
                    btnSubmit.setIcon(getDrawable(R.drawable.ic_send));
                    Toast.makeText(this, "Failed to submit review: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendReviewNotificationToLandlord(float rating) {
        if (landlordId == null || landlordId.isEmpty()) {
            Log.e(TAG, "Cannot send notification: landlordId is null or empty");
            return;
        }

        if (currentUserName == null || currentUserName.isEmpty()) {
            Log.e(TAG, "Cannot send notification: currentUserName is null or empty");
            return;
        }

        Log.d(TAG, "Sending review notification to landlord: " + landlordId);
        
        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.notifyNewReview(
                landlordId,
                currentUserName,
                dormName,
                (int) rating,
                dormId
        );
        
        Log.d(TAG, "Review notification sent successfully");
    }

    private void recalculateDormRating() {
        Log.d(TAG, "Recalculating dorm rating from all reviews");
        
        // Query all reviews for this dormitory
        db.collection("reviews")
                .whereEqualTo("dormId", dormId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalReviews = queryDocumentSnapshots.size();
                    float sum = 0f;
                    
                    Log.d(TAG, "Found " + totalReviews + " reviews");
                    
                    for (int i = 0; i < totalReviews; i++) {
                        Review review = queryDocumentSnapshots.getDocuments().get(i)
                                .toObject(Review.class);
                        if (review != null) {
                            sum += review.getRating();
                            Log.d(TAG, "Review " + (i+1) + " rating: " + review.getRating());
                        }
                    }
                    
                    float averageRating = totalReviews > 0 ? sum / totalReviews : 0f;
                    
                    Log.d(TAG, "Calculated - Total: " + totalReviews + ", Average: " + averageRating);
                    
                    // Update dormitory document
                    db.collection("dormitories").document(dormId)
                            .update(
                                    "averageRating", averageRating,
                                    "totalReviews", totalReviews
                            )
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Dormitory rating updated successfully!");
                                Toast.makeText(this, "Review submitted successfully! Thank you for your feedback.", 
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update dormitory rating: " + e.getMessage());
                                Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch reviews: " + e.getMessage());
                    Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
