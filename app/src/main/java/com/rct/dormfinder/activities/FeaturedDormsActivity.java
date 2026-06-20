package com.rct.dormfinder.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.AllDormsAdapter;
import com.rct.dormfinder.models.Dormitory;

import java.util.ArrayList;
import java.util.List;

public class FeaturedDormsActivity extends AppCompatActivity {
    private static final String TAG = "FeaturedDormsActivity";
    
    // Criteria for featured dorms
    private static final double MIN_RATING = 4.0;
    private static final int MIN_REVIEW_COUNT = 3;
    
    private ImageView ivBack;
    private RecyclerView recyclerViewFeaturedDorms;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View layoutEmptyState;
    private ProgressBar progressBar;
    
    private FirebaseFirestore db;
    private AllDormsAdapter adapter;
    private List<Dormitory> featuredDormitories;
    private boolean isLoading = false;
    private int processedCount = 0;
    private int totalToProcess = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_featured_dorms);
        
        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupSwipeRefresh();
        setupListeners();
        loadFeaturedDorms();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        recyclerViewFeaturedDorms = findViewById(R.id.recyclerViewFeaturedDorms);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void setupRecyclerView() {
        featuredDormitories = new ArrayList<>();
        adapter = new AllDormsAdapter(this, featuredDormitories, true); // true = grid layout
        
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerViewFeaturedDorms.setLayoutManager(gridLayoutManager);
        recyclerViewFeaturedDorms.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadFeaturedDorms);
        swipeRefreshLayout.setColorSchemeResources(R.color.mint_primary);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private void loadFeaturedDorms() {
        // Prevent multiple simultaneous loads
        if (isLoading) {
            android.util.Log.d(TAG, "Already loading, skipping duplicate call");
            return;
        }
        
        android.util.Log.d(TAG, "Loading featured dorms with criteria: rating >= " + MIN_RATING + ", verified reviews >= " + MIN_REVIEW_COUNT);
        
        isLoading = true;
        showLoading(true);
        
        // Query all available dormitories
        db.collection("dormitories")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d(TAG, "Query successful, total dorms: " + queryDocumentSnapshots.size());
                    
                    featuredDormitories.clear();
                    processedCount = 0;
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        isLoading = false;
                        updateEmptyState();
                        showLoading(false);
                        return;
                    }
                    
                    totalToProcess = queryDocumentSnapshots.size();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Dormitory dorm = document.toObject(Dormitory.class);
                        if (dorm != null) {
                            dorm.setDormId(document.getId());
                            
                            // Count verified reviews for this dorm
                            countVerifiedReviews(dorm);
                        }
                    }
                    
                    android.util.Log.d(TAG, "Total featured dorms: " + featuredDormitories.size());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to load featured dorms: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to load featured dorms", Toast.LENGTH_SHORT).show();
                    isLoading = false;
                    showLoading(false);
                });
    }
    
    private void countVerifiedReviews(Dormitory dorm) {
        db.collection("reviews")
                .whereEqualTo("dormId", dorm.getDormId())
                .whereEqualTo("verified", true)
                .get()
                .addOnSuccessListener(reviewSnapshots -> {
                    int verifiedReviewCount = reviewSnapshots.size();
                    double avgRating = dorm.getAverageRating();
                    
                    android.util.Log.d(TAG, "Dorm: " + dorm.getName() + 
                        ", Rating: " + avgRating + 
                        ", Total Reviews: " + dorm.getReviewCount() +
                        ", Verified Reviews: " + verifiedReviewCount);
                    
                    // Check if dorm meets featured criteria with VERIFIED reviews
                    if (avgRating >= MIN_RATING && verifiedReviewCount >= MIN_REVIEW_COUNT) {
                        // Check for duplicates before adding
                        boolean alreadyExists = false;
                        for (Dormitory existingDorm : featuredDormitories) {
                            if (existingDorm.getDormId().equals(dorm.getDormId())) {
                                alreadyExists = true;
                                break;
                            }
                        }
                        
                        if (!alreadyExists) {
                            featuredDormitories.add(dorm);
                            android.util.Log.d(TAG, "Added to featured: " + dorm.getName());
                        }
                    }
                    
                    processedCount++;
                    
                    // Only update UI when all dorms have been processed
                    if (processedCount >= totalToProcess) {
                        // Sort by rating after all additions
                        featuredDormitories.sort((d1, d2) -> 
                            Double.compare(d2.getAverageRating(), d1.getAverageRating())
                        );
                        
                        adapter.updateDormitories(featuredDormitories);
                        updateEmptyState();
                        showLoading(false);
                        isLoading = false;
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to count verified reviews for: " + dorm.getName(), e);
                    processedCount++;
                    
                    if (processedCount >= totalToProcess) {
                        showLoading(false);
                        isLoading = false;
                    }
                });
    }

    private void updateEmptyState() {
        if (featuredDormitories.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewFeaturedDorms.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewFeaturedDorms.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            swipeRefreshLayout.setRefreshing(true);
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only reload if adapter has data (meaning we're returning from another activity)
        // Don't reload on initial creation to prevent duplicate calls
        if (adapter != null && adapter.getItemCount() > 0) {
            loadFeaturedDorms();
        }
    }
}
