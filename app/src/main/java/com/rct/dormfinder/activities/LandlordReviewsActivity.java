package com.rct.dormfinder.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.LandlordReviewAdapter;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.models.Review;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LandlordReviewsActivity extends BaseActivity {

    private ImageButton btnBack;
    private TextView tvNeedsReplyBadge;
    private TextView tvOverallRating, tvTotalReviews, tvNeedsReply;
    private RatingBar ratingBarOverall;
    private Spinner spinnerDormFilter, spinnerSort;
    private RecyclerView recyclerViewReviews;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private View layoutNeedsReply;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentLandlordId;

    private LandlordReviewAdapter adapter;
    private List<Review> allReviews;
    private List<Review> filteredReviews;
    private List<Dormitory> landlordDormitories;
    private Map<String, String> dormIdToNameMap;

    private ListenerRegistration reviewsListener;

    private String selectedDormId = "all";
    private String selectedSort = "newest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landlord_reviews);

        // Check if we should filter by a specific dormitory (from notification)
        String notificationDormId = getIntent().getStringExtra("dormitoryId");
        boolean shouldFilter = getIntent().getBooleanExtra("filterByDormitory", false);

        if (shouldFilter && notificationDormId != null && !notificationDormId.isEmpty()) {
            selectedDormId = notificationDormId;
            android.util.Log.d("LandlordReviews", "Will filter by dormitory from notification: " + selectedDormId);
        }

        initializeViews();
        setupFirebase();
        setupToolbar();
        setupRecyclerView();
        setupSpinners();
        setupSwipeRefresh();
        loadLandlordDormitories();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to header
        applyTopInsets(insets, R.id.headerLayout);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvNeedsReplyBadge = findViewById(R.id.tvNeedsReplyBadge);
        tvOverallRating = findViewById(R.id.tvOverallRating);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        tvNeedsReply = findViewById(R.id.tvNeedsReply);
        ratingBarOverall = findViewById(R.id.ratingBarOverall);
        spinnerDormFilter = findViewById(R.id.spinnerDormFilter);
        spinnerSort = findViewById(R.id.spinnerSort);
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        layoutNeedsReply = findViewById(R.id.layoutNeedsReply);

        allReviews = new ArrayList<>();
        filteredReviews = new ArrayList<>();
        landlordDormitories = new ArrayList<>();
        dormIdToNameMap = new HashMap<>();
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentLandlordId = mAuth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }
    }

    private void setupToolbar() {
        // Setup back button click listener
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new LandlordReviewAdapter(filteredReviews, this, dormIdToNameMap);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewReviews.setLayoutManager(layoutManager);
        recyclerViewReviews.setAdapter(adapter);
        recyclerViewReviews.setHasFixedSize(false); // Allow dynamic sizing
    }

    private void setupSpinners() {
        // Sort options
        String[] sortOptions = {"Newest First", "Oldest First", "Highest Rating", "Lowest Rating", "Needs Reply"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_black_text, sortOptions);
        sortAdapter.setDropDownViewResource(R.layout.spinner_item_black_text);
        spinnerSort.setAdapter(sortAdapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        selectedSort = "newest";
                        break;
                    case 1:
                        selectedSort = "oldest";
                        break;
                    case 2:
                        selectedSort = "highest";
                        break;
                    case 3:
                        selectedSort = "lowest";
                        break;
                    case 4:
                        selectedSort = "needs_reply";
                        break;
                }
                filterAndSortReviews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_color);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadReviews();
        });
    }

    private void loadLandlordDormitories() {
        showLoading(true);

        db.collection("dormitories")
                .whereEqualTo("landlordId", currentLandlordId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    landlordDormitories.clear();
                    dormIdToNameMap.clear();

                    List<String> dormNames = new ArrayList<>();
                    dormNames.add("All Dormitories");

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Dormitory dorm = doc.toObject(Dormitory.class);
                        if (dorm != null) {
                            dorm.setDormId(doc.getId());
                            landlordDormitories.add(dorm);
                            dormIdToNameMap.put(doc.getId(), dorm.getName());
                            dormNames.add(dorm.getName());
                        }
                    }

                    // Setup dormitory filter spinner
                    ArrayAdapter<String> dormAdapter = new ArrayAdapter<>(this,
                            R.layout.spinner_item_black_text, dormNames);
                    dormAdapter.setDropDownViewResource(R.layout.spinner_item_black_text);
                    spinnerDormFilter.setAdapter(dormAdapter);

                    spinnerDormFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position == 0) {
                                selectedDormId = "all";
                            } else {
                                selectedDormId = landlordDormitories.get(position - 1).getDormId();
                            }
                            filterAndSortReviews();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    // If we have a notification dorm filter, select it in the spinner
                    if (!selectedDormId.equals("all")) {
                        for (int i = 0; i < landlordDormitories.size(); i++) {
                            if (landlordDormitories.get(i).getDormId().equals(selectedDormId)) {
                                spinnerDormFilter.setSelection(i + 1); // +1 because "All" is at position 0
                                android.util.Log.d("LandlordReviews", "Auto-selected dormitory at position: " + (i + 1));
                                break;
                            }
                        }
                    }

                    // Load reviews after dormitories are loaded
                    loadReviews();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to load dormitories: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReviews() {
        if (landlordDormitories.isEmpty()) {
            showLoading(false);
            swipeRefreshLayout.setRefreshing(false);
            updateEmptyState();
            android.util.Log.w("LandlordReviews", "No dormitories found, cannot load reviews");
            return;
        }

        // Remove existing listener if any
        if (reviewsListener != null) {
            reviewsListener.remove();
        }

        // Get all dormitory IDs
        List<String> dormIds = new ArrayList<>();
        for (Dormitory dorm : landlordDormitories) {
            dormIds.add(dorm.getDormId());
            android.util.Log.d("LandlordReviews", "Loading reviews for dorm: " + dorm.getName() + " (" + dorm.getDormId() + ")");
        }

        // Check if dormIds is too large for whereIn query (max 10 items)
        if (dormIds.size() > 10) {
            android.util.Log.w("LandlordReviews", "More than 10 dormitories, using alternative query method");
            loadReviewsForManyDormitories();
            return;
        }

        // Load reviews for all landlord's dormitories with real-time updates
        android.util.Log.d("LandlordReviews", "Setting up review listener for " + dormIds.size() + " dormitories");

        reviewsListener = db.collection("reviews")
                .whereIn("dormId", dormIds)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        android.util.Log.e("LandlordReviews", "Error loading reviews", error);
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(this, "Failed to load reviews: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        android.util.Log.d("LandlordReviews", "Received " + queryDocumentSnapshots.size() + " reviews");
                        allReviews.clear();

                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Review review = doc.toObject(Review.class);
                            if (review != null) {
                                review.setReviewId(doc.getId());
                                allReviews.add(review);
                                android.util.Log.d("LandlordReviews", "Loaded review: " + review.getReviewId() + " for dorm: " + review.getDormId());
                            }
                        }

                        updateStatistics();
                        filterAndSortReviews();
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void loadReviewsForManyDormitories() {
        // For more than 10 dormitories, load all reviews and filter locally
        allReviews.clear();

        db.collection("reviews")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            review.setReviewId(doc.getId());
                            // Check if this review belongs to one of the landlord's dormitories
                            for (Dormitory dorm : landlordDormitories) {
                                if (dorm.getDormId().equals(review.getDormId())) {
                                    allReviews.add(review);
                                    break;
                                }
                            }
                        }
                    }

                    updateStatistics();
                    filterAndSortReviews();
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LandlordReviews", "Error loading reviews", e);
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterAndSortReviews() {
        android.util.Log.d("LandlordReviews", "Filtering reviews. Total: " + allReviews.size() + ", Selected Dorm: " + selectedDormId + ", Sort: " + selectedSort);

        filteredReviews.clear();

        // Filter by dormitory
        if (selectedDormId.equals("all")) {
            filteredReviews.addAll(allReviews);
            android.util.Log.d("LandlordReviews", "Showing all reviews: " + filteredReviews.size());
        } else {
            for (Review review : allReviews) {
                if (review.getDormId().equals(selectedDormId)) {
                    filteredReviews.add(review);
                }
            }
            android.util.Log.d("LandlordReviews", "Filtered reviews for dorm " + selectedDormId + ": " + filteredReviews.size());
        }

        // Sort reviews
        switch (selectedSort) {
            case "newest":
                Collections.sort(filteredReviews, (r1, r2) ->
                        Long.compare(r2.getDatePosted(), r1.getDatePosted()));
                break;

            case "oldest":
                Collections.sort(filteredReviews, (r1, r2) ->
                        Long.compare(r1.getDatePosted(), r2.getDatePosted()));
                break;

            case "highest":
                Collections.sort(filteredReviews, (r1, r2) ->
                        Float.compare(r2.getRating(), r1.getRating()));
                break;

            case "lowest":
                Collections.sort(filteredReviews, (r1, r2) ->
                        Float.compare(r1.getRating(), r2.getRating()));
                break;

            case "needs_reply":
                Collections.sort(filteredReviews, (r1, r2) -> {
                    // Put reviews without replies first
                    if (!r1.hasReply() && r2.hasReply()) return -1;
                    if (r1.hasReply() && !r2.hasReply()) return 1;
                    // Then sort by date
                    return Long.compare(r2.getDatePosted(), r1.getDatePosted());
                });
                break;
        }

        android.util.Log.d("LandlordReviews", "Final filtered and sorted reviews: " + filteredReviews.size());
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            android.util.Log.d("LandlordReviews", "Adapter notified. Adapter item count: " + adapter.getItemCount());
        }
        updateEmptyState();
    }

    private void updateStatistics() {
        if (allReviews.isEmpty()) {
            tvOverallRating.setText("0.0");
            ratingBarOverall.setRating(0);
            tvTotalReviews.setText("No reviews yet");
            layoutNeedsReply.setVisibility(View.GONE);
            tvNeedsReplyBadge.setVisibility(View.GONE);
            return;
        }

        // Calculate overall rating
        float totalRating = 0;
        int needsReplyCount = 0;

        for (Review review : allReviews) {
            totalRating += review.getRating();
            if (!review.hasReply()) {
                needsReplyCount++;
            }
        }

        float averageRating = totalRating / allReviews.size();

        tvOverallRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
        ratingBarOverall.setRating(averageRating);
        tvTotalReviews.setText(String.format(Locale.getDefault(),
                "Based on %d review%s", allReviews.size(), allReviews.size() == 1 ? "" : "s"));

        if (needsReplyCount > 0) {
            layoutNeedsReply.setVisibility(View.VISIBLE);
            tvNeedsReply.setText(String.format(Locale.getDefault(),
                    "%d review%s need reply", needsReplyCount, needsReplyCount == 1 ? "" : "s"));
            tvNeedsReplyBadge.setVisibility(View.VISIBLE);
            tvNeedsReplyBadge.setText(String.valueOf(needsReplyCount));
        } else {
            layoutNeedsReply.setVisibility(View.GONE);
            tvNeedsReplyBadge.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (filteredReviews.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerViewReviews.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewReviews.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reviewsListener != null) {
            reviewsListener.remove();
        }
    }
}