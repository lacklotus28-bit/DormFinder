package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.ReviewAdapter;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.models.Review;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AllReviewsActivity extends AppCompatActivity {
    private static final String TAG = "AllReviewsActivity";
    
    private ImageView ivBack;
    private Button btnAddReview;
    private TextView tvDormName, tvAverageRating, tvTotalReviews, tvNoReviews;
    private RatingBar ratingBarAverage;
    private RecyclerView recyclerViewReviews;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String dormitoryId;
    private String currentUserId;
    private Dormitory dormitory;
    
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewsList = new ArrayList<>();
    private ListenerRegistration dormitoryListener;
    private ListenerRegistration reviewsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reviews);
        
        // Set status bar color to match header
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.mint_primary));
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // Get dormitory ID from intent
        dormitoryId = getIntent().getStringExtra("dormitory_id");
        if (dormitoryId == null) {
            Log.e(TAG, "No dormitory ID provided");
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupListeners();
        loadDormitoryInfo();
        loadReviews();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        btnAddReview = findViewById(R.id.btnAddReview);
        tvDormName = findViewById(R.id.tvDormName);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        ratingBarAverage = findViewById(R.id.ratingBarAverage);
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter(reviewsList, this);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReviews.setAdapter(reviewAdapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnAddReview.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddReviewActivity.class);
            intent.putExtra("dormId", dormitoryId);
            if (dormitory != null) {
                intent.putExtra("dormName", dormitory.getName());
            }
            startActivity(intent);
        });

        // Pull to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadReviews();
            swipeRefreshLayout.setRefreshing(false);
        });
        
        // Set refresh colors
        swipeRefreshLayout.setColorSchemeResources(
                R.color.orange_primary,
                R.color.orange_primary
        );
    }

    private void loadDormitoryInfo() {
        // Remove old listener if exists
        if (dormitoryListener != null) {
            dormitoryListener.remove();
        }
        
        // Setup real-time listener for dormitory
        dormitoryListener = db.collection("dormitories").document(dormitoryId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading dormitory: " + error.getMessage());
                        return;
                    }

                    if (document != null && document.exists()) {
                        dormitory = document.toObject(Dormitory.class);
                        if (dormitory != null) {
                            updateDormitoryInfo();
                        }
                    }
                });
    }

    private void updateDormitoryInfo() {
        tvDormName.setText(dormitory.getName());
        tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", dormitory.getAverageRating()));
        ratingBarAverage.setRating(dormitory.getAverageRating());
        tvTotalReviews.setText(String.format(Locale.getDefault(), 
                "Based on %d %s", 
                dormitory.getTotalReviews(),
                dormitory.getTotalReviews() == 1 ? "review" : "reviews"));
    }

    private void loadReviews() {
        // Remove old listener if exists
        if (reviewsListener != null) {
            reviewsListener.remove();
        }
        
        // Setup real-time listener for reviews
        reviewsListener = db.collection("reviews")
                .whereEqualTo("dormId", dormitoryId)
                .orderBy("datePosted", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading reviews: " + error.getMessage());
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        reviewsList.clear();
                        
                        for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                            Review review = queryDocumentSnapshots.getDocuments()
                                    .get(i).toObject(Review.class);
                            if (review != null) {
                                reviewsList.add(review);
                            }
                        }
                        
                        updateReviewsDisplay();
                    }
                });
    }

    private void updateReviewsDisplay() {
        if (reviewsList.isEmpty()) {
            tvNoReviews.setVisibility(View.VISIBLE);
            recyclerViewReviews.setVisibility(View.GONE);
        } else {
            tvNoReviews.setVisibility(View.GONE);
            recyclerViewReviews.setVisibility(View.VISIBLE);
            reviewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners
        if (dormitoryListener != null) {
            dormitoryListener.remove();
        }
        if (reviewsListener != null) {
            reviewsListener.remove();
        }
    }
}
