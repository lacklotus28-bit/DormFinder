package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.FeaturedDormAdapter;
import com.rct.dormfinder.adapters.AllDormsAdapter;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.models.User;
import com.rct.dormfinder.utils.NetworkUtil;

import java.util.ArrayList;
import java.util.List;

public class StudentHomeActivity extends BaseActivity {
    private static final String TAG = "StudentHomeActivity";
    
    // Header views
    private ImageView ivNotification, ivGuide;
    private TextView tvNotificationBadge, tvMessageBadge, tvBookingBadge;
    
    // Search and filter
    private EditText etSearch;
    private ImageView ivSearchIcon, ivClearSearch;
    private ProgressBar searchLoadingIndicator;
    private CardView btnFilter, btnViewOnMap;
    
    // RecyclerViews
    private RecyclerView recyclerViewFeaturedDorms, recyclerViewAllDorms;
    
    // No results view
    private LinearLayout noResultsView;
    private TextView tvNoResults;
    
    // See all links
    private TextView tvSeeAllFeatured, tvSeeAllDorms;
    
    // Bottom Navigation
    private BottomNavigationView bottomNavigation;
    
    // Swipe to refresh
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private NetworkUtil networkUtil;
    private com.rct.dormfinder.utils.NotificationHelper notificationHelper;
    private com.google.firebase.firestore.ListenerRegistration notificationListener;
    private com.google.firebase.firestore.ListenerRegistration messageListener;
    private com.google.firebase.firestore.ListenerRegistration bookingListener;
    
    private FeaturedDormAdapter featuredDormAdapter;
    private AllDormsAdapter allDormsAdapter;
    private List<Dormitory> featuredDormitories;
    private List<Dormitory> allDormitories;
    private List<Dormitory> filteredFeaturedDormitories;
    private List<Dormitory> filteredAllDormitories;

    private String currentUserId;
    
    // Filter variables
    private String selectedCity = "All";
    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;
    private boolean showAvailableOnly = false;
    private List<String> selectedAmenities = new ArrayList<>();
    
    // Search handler for debouncing
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);
        Log.d(TAG, "onCreate called");

        initializeViews();
        setupFirebase();
        setupRecyclerViews();
        setupListeners();
        loadDormitories();
        
        // Show interactive tutorial for first-time users
        showInteractiveTutorial();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to header
        applyTopInsets(insets, R.id.headerLayout);
        
        // Apply bottom insets to bottom navigation
        applyBottomInsets(insets, R.id.bottomNavigation);
    }

    private void initializeViews() {
        Log.d(TAG, "initializeViews called");
        
        // Header
        ivNotification = findViewById(R.id.ivNotification);
        ivGuide = findViewById(R.id.ivGuide);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        // Note: tvMessageBadge and tvBookingBadge are handled by NavigationHelper on bottom navigation
        
        // Search and filter
        etSearch = findViewById(R.id.etSearch);
        ivSearchIcon = findViewById(R.id.ivSearchIcon);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        searchLoadingIndicator = findViewById(R.id.searchLoadingIndicator);
        btnFilter = findViewById(R.id.btnFilter);
        btnViewOnMap = findViewById(R.id.btnViewOnMap);
        
        // RecyclerViews
        recyclerViewFeaturedDorms = findViewById(R.id.recyclerViewFeaturedDorms);
        recyclerViewAllDorms = findViewById(R.id.recyclerViewAllDorms);
        
        // No results view
        noResultsView = findViewById(R.id.noResultsView);
        tvNoResults = findViewById(R.id.tvNoResults);
        
        // See all links
        tvSeeAllFeatured = findViewById(R.id.tvSeeAllFeatured);
        tvSeeAllDorms = findViewById(R.id.tvSeeAllDorms);
        
        // Bottom Navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);
        
        // Swipe to refresh
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        Log.d(TAG, "Views initialized - Featured RV: " + (recyclerViewFeaturedDorms != null) + 
                   ", All RV: " + (recyclerViewAllDorms != null));
    }

    private void setupFirebase() {
        Log.d(TAG, "setupFirebase called");
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        networkUtil = new NetworkUtil(this);
        notificationHelper = new com.rct.dormfinder.utils.NotificationHelper(this);

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
            Log.d(TAG, "User ID: " + currentUserId);
        } else {
            Log.d(TAG, "No user logged in, redirecting to login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void setupRecyclerViews() {
        Log.d(TAG, "setupRecyclerViews called");
        
        // Featured Dorms (Horizontal)
        featuredDormitories = new ArrayList<>();
        filteredFeaturedDormitories = new ArrayList<>();
        featuredDormAdapter = new FeaturedDormAdapter(this, filteredFeaturedDormitories);
        
        LinearLayoutManager featuredLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        recyclerViewFeaturedDorms.setLayoutManager(featuredLayoutManager);
        recyclerViewFeaturedDorms.setAdapter(featuredDormAdapter);
        
        // All Dorms (2-column Grid for Home)
        allDormitories = new ArrayList<>();
        filteredAllDormitories = new ArrayList<>();
        
        // Force 2 columns for home page display
        int gridColumns = 2;
        boolean useGridLayout = true;
        
        Log.d(TAG, "Grid columns: " + gridColumns + " (fixed for home), Use grid: " + useGridLayout);
        
        allDormsAdapter = new AllDormsAdapter(this, filteredAllDormitories, useGridLayout);
        
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, gridColumns);
        recyclerViewAllDorms.setLayoutManager(gridLayoutManager);
        recyclerViewAllDorms.setAdapter(allDormsAdapter);
        
        Log.d(TAG, "RecyclerViews setup complete with " + gridColumns + " columns");
    }

    private void setupListeners() {
        // Header icons
        ivNotification.setOnClickListener(v -> 
                startActivity(new Intent(this, NotificationActivity.class)));
        
        ivGuide.setOnClickListener(v -> 
                startActivity(new Intent(this, AppGuideActivity.class)));
        
        // Search EditText with debouncing and typing indicator
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show/hide clear button
                if (s.length() > 0) {
                    ivClearSearch.setVisibility(View.VISIBLE);
                } else {
                    ivClearSearch.setVisibility(View.GONE);
                }
                
                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                // Show loading indicator
                ivSearchIcon.setVisibility(View.GONE);
                searchLoadingIndicator.setVisibility(View.VISIBLE);
                
                // Debounce search - wait 500ms after user stops typing
                searchRunnable = () -> {
                    filterDormitories(s.toString());
                    // Hide loading indicator
                    searchLoadingIndicator.setVisibility(View.GONE);
                    ivSearchIcon.setVisibility(View.VISIBLE);
                };
                searchHandler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Clear search button
        ivClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            ivClearSearch.setVisibility(View.GONE);
        });
        
        // Filter button
        btnFilter.setOnClickListener(v -> showFilterDialog());
        
        // View on Map button
        btnViewOnMap.setOnClickListener(v -> 
                startActivity(new Intent(this, MapActivity.class)));
        
        // See All links
        tvSeeAllFeatured.setOnClickListener(v -> 
                startActivity(new Intent(this, FeaturedDormsActivity.class)));
        
        tvSeeAllDorms.setOnClickListener(v -> 
                startActivity(new Intent(this, SearchFilterActivity.class)));
        
        // Setup Bottom Navigation using NavigationHelper
        com.rct.dormfinder.utils.NavigationHelper.setupStudentBottomNavigation(
            this, 
            bottomNavigation, 
            R.id.nav_home
        );
        
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadDormitories();
        });
        
        // Set refresh colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.mint_primary,
            R.color.mint_light,
            R.color.mint_primary
        );
    }

    private void loadDormitories() {
        Log.d(TAG, "loadDormitories called");
        
        if (!networkUtil.isNetworkAvailable()) {
            Log.d(TAG, "No network available");
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }
        
        Log.d(TAG, "Network available, loading dorms...");

        // IMPORTANT: Clear lists before loading to prevent duplicates
        featuredDormitories.clear();
        allDormitories.clear();
        featuredDormAdapter.notifyDataSetChanged();
        allDormsAdapter.notifyDataSetChanged();

        // Load Featured Dorms (limit to 5) - Only show dorms with rating >= 4.0 and at least 3 VERIFIED reviews
        db.collection("dormitories")
                .whereEqualTo("available", true)
                .limit(20)  // Get more to filter, then limit to 5
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Featured query successful! Total docs: " + queryDocumentSnapshots.size());
                    
                    featuredDormitories.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Dormitory dorm = document.toObject(Dormitory.class);
                        if (dorm != null) {
                            dorm.setDormId(document.getId());
                            
                            // Count verified reviews for this dorm
                            if (dorm.getAverageRating() >= 4.0) {
                                countVerifiedReviewsForFeatured(dorm);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load featured dorms: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to load featured dorms", Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });

        // Load All Dorms (limit to 4 for home screen)
        db.collection("dormitories")
                .limit(4)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "All dorms query successful! Total docs: " + queryDocumentSnapshots.size());
                    allDormitories.clear();
                    queryDocumentSnapshots.forEach(doc -> {
                        Dormitory dorm = doc.toObject(Dormitory.class);
                        if (dorm != null) {
                            dorm.setDormId(doc.getId());
                            allDormitories.add(dorm);
                            Log.d(TAG, "Added all dorm: " + dorm.getName() + ", isAvailable: " + dorm.isAvailable());
                        }
                    });
                    Log.d(TAG, "All dorms list size: " + allDormitories.size());
                    
                    // Apply filters to populate filteredAllDormitories
                    filterDormitories(etSearch.getText().toString());
                    Log.d(TAG, "All dorms adapter notified");
                    
                    // Stop refresh animation
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load all dorms: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to load dorms", Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        
        // CRITICAL: Re-select Home ONLY after view is rendered to prevent race conditions
        if (bottomNavigation != null) {
            bottomNavigation.post(() -> {
                bottomNavigation.setSelectedItemId(R.id.nav_home);
                Log.d(TAG, "✅ Bottom navigation set to Home in onResume");
            });
        }
        
        // Don't reload on resume - only load on create
        // User can swipe down to refresh manually
        
        // Start listening for notifications, messages, and bookings
        setupNotificationListener();
        setupMessageListener();
        setupBookingListener();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Remove notification listener when activity is paused
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
        // Remove message listener when activity is paused
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
        // Remove booking listener when activity is paused
        if (bookingListener != null) {
            bookingListener.remove();
            bookingListener = null;
        }
    }
    
    /**
     * Setup real-time listener for notification count
     */
    private void setupNotificationListener() {
        Log.d(TAG, "===== NOTIFICATION LISTENER SETUP CALLED =====");
        if (currentUserId == null) {
            Log.e(TAG, "Cannot setup notification listener - currentUserId is NULL!");
            return;
        }
        Log.d(TAG, "Setting up notification listener for user: " + currentUserId);
        
        // Remove existing listener if any
        if (notificationListener != null) {
            notificationListener.remove();
        }
        
        // Listen for real-time notification updates
        Log.d(TAG, "Attaching Firestore snapshot listener...");
        // Note: Changed from "read" to "isRead" to match Notification model field name
        notificationListener = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    Log.d(TAG, "Notification listener triggered!");
                    if (error != null) {
                        Log.e(TAG, "Notification listener error: " + error.getMessage());
                        return;
                    }
                    
                    if (queryDocumentSnapshots != null) {
                        int unreadCount = queryDocumentSnapshots.size();
                        Log.d(TAG, "Unread notifications count: " + unreadCount);
                        
                        // DEBUG: Log details of unread notifications
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Log.d(TAG, "📧 Unread Notification ID: " + doc.getId());
                            Log.d(TAG, "   Title: " + doc.getString("title"));
                            Log.d(TAG, "   Message: " + doc.getString("message"));
                            Log.d(TAG, "   Type: " + doc.getString("type"));
                            Log.d(TAG, "   isRead: " + doc.getBoolean("isRead"));
                            Log.d(TAG, "   Created: " + doc.getTimestamp("createdAt"));
                        }
                        
                        updateNotificationBadge(unreadCount);
                    } else {
                        Log.w(TAG, "queryDocumentSnapshots is NULL!");
                    }
                });
    }
    
    /**
     * Update notification badge UI
     */
    private void updateNotificationBadge(int count) {
        Log.d(TAG, "updateNotificationBadge called with count: " + count);
        if (tvNotificationBadge != null) {
            Log.d(TAG, "Badge TextView found, updating visibility");
            if (count > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Setup real-time listener for unread message count
     */
    private void setupMessageListener() {
        Log.d(TAG, "===== MESSAGE LISTENER SETUP CALLED =====");
        if (currentUserId == null) {
            Log.e(TAG, "Cannot setup message listener - currentUserId is NULL!");
            return;
        }
        Log.d(TAG, "Setting up message listener for user: " + currentUserId);
        
        // Remove existing listener if any
        if (messageListener != null) {
            messageListener.remove();
        }
        
        // Listen for chats where user is student
        Log.d(TAG, "Attaching Firestore message listener...");
        messageListener = db.collection("chats")
                .whereEqualTo("studentId", currentUserId)
                .addSnapshotListener((studentChats, error) -> {
                    Log.d(TAG, "===== MESSAGE LISTENER TRIGGERED =====");
                    if (error != null) {
                        Log.e(TAG, "Message listener error: " + error.getMessage());
                        return;
                    }
                    
                    int totalUnread = 0;
                    
                    if (studentChats != null) {
                        Log.d(TAG, "Found " + studentChats.size() + " chats for student");
                        
                        for (int i = 0; i < studentChats.size(); i++) {
                            String chatId = studentChats.getDocuments().get(i).getId();
                            Long unreadCount = studentChats.getDocuments().get(i).getLong("studentUnreadCount");
                            
                            Log.d(TAG, "Chat #" + (i+1) + " - ID: " + chatId + ", Unread: " + unreadCount);
                            
                            if (unreadCount != null) {
                                totalUnread += unreadCount.intValue();
                            }
                        }
                    } else {
                        Log.w(TAG, "studentChats is NULL!");
                    }
                    
                    Log.d(TAG, "===== TOTAL UNREAD MESSAGES: " + totalUnread + " =====");
                    updateMessageBadge(totalUnread);
                });
        
        Log.d(TAG, "Message listener attached successfully");
    }
    
    /**
     * Update message badge UI
     */
    private void updateMessageBadge(int count) {
        Log.d(TAG, "==== UPDATE MESSAGE BADGE CALLED ====");
        Log.d(TAG, "updateMessageBadge called with count: " + count);
        
        runOnUiThread(() -> {
            // Update bottom navigation badge
            com.rct.dormfinder.utils.NavigationHelper.updateBadge(
                bottomNavigation, 
                R.id.nav_messages, 
                count
            );
            Log.d(TAG, "✅ Message badge updated on bottom navigation with count: " + count);
        });
    }
    
    /**
     * Setup real-time listener for bookings requiring attention
     */
    private void setupBookingListener() {
        Log.d(TAG, "===== BOOKING LISTENER SETUP CALLED =====");
        if (currentUserId == null) {
            Log.e(TAG, "Cannot setup booking listener - currentUserId is NULL!");
            return;
        }
        Log.d(TAG, "Setting up booking listener for user: " + currentUserId);
        
        // Remove existing listener if any
        if (bookingListener != null) {
            bookingListener.remove();
        }
        
        // Listen for bookings where student needs to take action
        // Count bookings with these statuses:
        // - "approved" (needs payment)
        // - "paid" (waiting for landlord confirmation)
        Log.d(TAG, "Attaching Firestore booking listener...");
        bookingListener = db.collection("bookings")
                .whereEqualTo("studentId", currentUserId)
                .addSnapshotListener((bookingSnapshots, error) -> {
                    Log.d(TAG, "===== BOOKING LISTENER TRIGGERED =====");
                    if (error != null) {
                        Log.e(TAG, "Booking listener error: " + error.getMessage());
                        return;
                    }
                    
                    int actionRequiredCount = 0;
                    
                    if (bookingSnapshots != null) {
                        Log.d(TAG, "Found " + bookingSnapshots.size() + " total bookings for student");
                        
                        for (int i = 0; i < bookingSnapshots.size(); i++) {
                            String status = bookingSnapshots.getDocuments().get(i).getString("status");
                            String bookingId = bookingSnapshots.getDocuments().get(i).getId();
                            
                            // Count bookings requiring student action
                            // Only "approved" status requires action (student needs to pay)
                            // "paid" status means student already paid and is waiting for landlord
                            if ("approved".equals(status)) {
                                actionRequiredCount++;
                                Log.d(TAG, "📋 Booking #" + (i+1) + " - ID: " + bookingId + ", Status: " + status + " (ACTION REQUIRED - NEEDS PAYMENT)");
                            } else {
                                Log.d(TAG, "Booking #" + (i+1) + " - ID: " + bookingId + ", Status: " + status);
                            }
                        }
                    } else {
                        Log.w(TAG, "bookingSnapshots is NULL!");
                    }
                    
                    Log.d(TAG, "===== TOTAL BOOKINGS REQUIRING ACTION: " + actionRequiredCount + " =====");
                    updateBookingBadge(actionRequiredCount);
                });
        
        Log.d(TAG, "Booking listener attached successfully");
    }
    
    /**
     * Update booking badge UI
     */
    private void updateBookingBadge(int count) {
        Log.d(TAG, "==== UPDATE BOOKING BADGE CALLED ====");
        Log.d(TAG, "updateBookingBadge called with count: " + count);
        
        runOnUiThread(() -> {
            // Update bottom navigation badge
            com.rct.dormfinder.utils.NavigationHelper.updateBadge(
                bottomNavigation, 
                R.id.nav_bookings, 
                count
            );
            Log.d(TAG, "✅ Booking badge updated on bottom navigation with count: " + count);
        });
    }
    
    private void countVerifiedReviewsForFeatured(Dormitory dorm) {
        db.collection("reviews")
                .whereEqualTo("dormId", dorm.getDormId())
                .whereEqualTo("verified", true)
                .get()
                .addOnSuccessListener(reviewSnapshots -> {
                    int verifiedReviewCount = reviewSnapshots.size();
                    
                    Log.d(TAG, "Dorm: " + dorm.getName() + 
                        ", Rating: " + dorm.getAverageRating() + 
                        ", Total Reviews: " + dorm.getReviewCount() +
                        ", Verified Reviews: " + verifiedReviewCount);
                    
                    // Only add if has 3+ verified reviews and not already full
                    if (verifiedReviewCount >= 3 && featuredDormitories.size() < 5) {
                        featuredDormitories.add(dorm);
                        Log.d(TAG, "Added featured dorm: " + dorm.getName());
                        
                        // Sort by rating (highest first)
                        featuredDormitories.sort((d1, d2) -> 
                            Double.compare(d2.getAverageRating(), d1.getAverageRating())
                        );
                        
                        // Apply filters to populate filtered list
                        filterFeaturedDorms(etSearch.getText().toString());
                        Log.d(TAG, "Featured dorms list size: " + featuredDormitories.size());
                    }
                    
                    // Stop refresh animation when done
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to count verified reviews for: " + dorm.getName(), e);
                });
    }
    
    /**
     * Filter both featured and all dormitories based on search text and filters
     */
    private void filterDormitories(String searchText) {
        filterFeaturedDorms(searchText);
        filterAllDorms(searchText);
    }
    
    /**
     * Filter featured dormitories
     */
    private void filterFeaturedDorms(String searchText) {
        filteredFeaturedDormitories.clear();

        for (Dormitory dorm : featuredDormitories) {
            if (matchesSearch(dorm, searchText) && matchesFilters(dorm)) {
                filteredFeaturedDormitories.add(dorm);
            }
        }

        featuredDormAdapter.updateDormitories(filteredFeaturedDormitories);
    }
    
    /**
     * Filter all dormitories and show/hide no results view
     */
    private void filterAllDorms(String searchText) {
        filteredAllDormitories.clear();

        for (Dormitory dorm : allDormitories) {
            if (matchesSearch(dorm, searchText) && matchesFilters(dorm)) {
                filteredAllDormitories.add(dorm);
            }
        }

        allDormsAdapter.updateDormitories(filteredAllDormitories);
        
        // Show/hide no results view (check if BOTH lists are empty)
        boolean noResults = filteredAllDormitories.isEmpty() && filteredFeaturedDormitories.isEmpty();
        
        if (noResults) {
            recyclerViewAllDorms.setVisibility(View.GONE);
            noResultsView.setVisibility(View.VISIBLE);
            
            // Update message based on search text
            if (searchText != null && !searchText.trim().isEmpty()) {
                tvNoResults.setText("No search results found");
            } else {
                tvNoResults.setText("No dormitories found");
            }
        } else {
            recyclerViewAllDorms.setVisibility(View.VISIBLE);
            noResultsView.setVisibility(View.GONE);
        }
    }

    private boolean matchesSearch(Dormitory dorm, String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) return true;

        String search = searchText.toLowerCase();
        return dorm.getName().toLowerCase().contains(search) ||
                dorm.getAddress().toLowerCase().contains(search) ||
                dorm.getCity().toLowerCase().contains(search);
    }

    private boolean matchesFilters(Dormitory dorm) {
        // Check city
        if (!selectedCity.equals("All") && !dorm.getCity().equals(selectedCity)) {
            return false;
        }
        
        // Check availability
        if (showAvailableOnly) {
            if (!dorm.isAvailable() || dorm.getAvailableRooms() <= 0) {
                return false;
            }
        }
        
        // Check price range
        if (dorm.getMonthlyPrice() < minPrice || dorm.getMonthlyPrice() > maxPrice) {
            return false;
        }

        // Check amenities
        if (!selectedAmenities.isEmpty()) {
            for (String amenity : selectedAmenities) {
                if (!dorm.getAmenities().contains(amenity)) {
                    return false;
                }
            }
        }

        return true;
    }
    
    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        
        // Make dialog background transparent to show rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Initialize dialog views
        RadioGroup rgCity = dialogView.findViewById(R.id.rgCity);
        EditText etMinPrice = dialogView.findViewById(R.id.etMinPrice);
        EditText etMaxPrice = dialogView.findViewById(R.id.etMaxPrice);
        CheckBox cbWifi = dialogView.findViewById(R.id.cbWifi);
        CheckBox cbAircon = dialogView.findViewById(R.id.cbAircon);
        CheckBox cbParking = dialogView.findViewById(R.id.cbParking);
        CheckBox cbLaundry = dialogView.findViewById(R.id.cbLaundry);
        CheckBox cbAvailableOnly = dialogView.findViewById(R.id.cbAvailableOnly);
        Button btnReset = dialogView.findViewById(R.id.btnReset);
        Button btnApply = dialogView.findViewById(R.id.btnApply);
        
        // Set current filter values
        if (selectedCity.equals("Batangas")) {
            rgCity.check(R.id.rbBatangas);
        } else if (selectedCity.equals("Lipa")) {
            rgCity.check(R.id.rbLipa);
        } else {
            rgCity.check(R.id.rbAll);
        }
        
        if (minPrice > 0) {
            etMinPrice.setText(String.valueOf((int)minPrice));
        }
        if (maxPrice < Double.MAX_VALUE) {
            etMaxPrice.setText(String.valueOf((int)maxPrice));
        }
        
        cbWifi.setChecked(selectedAmenities.contains("WiFi"));
        cbAircon.setChecked(selectedAmenities.contains("Air Conditioning"));
        cbParking.setChecked(selectedAmenities.contains("Parking"));
        cbLaundry.setChecked(selectedAmenities.contains("Laundry"));
        cbAvailableOnly.setChecked(showAvailableOnly);

        // Apply button
        btnApply.setOnClickListener(v -> {
            // Get selected city
            int checkedId = rgCity.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBatangas) {
                selectedCity = "Batangas";
            } else if (checkedId == R.id.rbLipa) {
                selectedCity = "Lipa";
            } else {
                selectedCity = "All";
            }

            // Get price range
            String minPriceStr = etMinPrice.getText().toString();
            String maxPriceStr = etMaxPrice.getText().toString();
            minPrice = minPriceStr.isEmpty() ? 0 : Double.parseDouble(minPriceStr);
            maxPrice = maxPriceStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxPriceStr);

            // Get selected amenities
            selectedAmenities.clear();
            if (cbWifi.isChecked()) selectedAmenities.add("WiFi");
            if (cbAircon.isChecked()) selectedAmenities.add("Air Conditioning");
            if (cbParking.isChecked()) selectedAmenities.add("Parking");
            if (cbLaundry.isChecked()) selectedAmenities.add("Laundry");

            showAvailableOnly = cbAvailableOnly.isChecked();

            // Apply filters
            filterDormitories(etSearch.getText().toString());
            
            Toast.makeText(this, "Filters applied", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Reset button
        btnReset.setOnClickListener(v -> {
            rgCity.check(R.id.rbAll);
            etMinPrice.setText("");
            etMaxPrice.setText("");
            cbWifi.setChecked(false);
            cbAircon.setChecked(false);
            cbParking.setChecked(false);
            cbLaundry.setChecked(false);
            cbAvailableOnly.setChecked(false);
            
            // Reset filter variables
            selectedCity = "All";
            minPrice = 0;
            maxPrice = Double.MAX_VALUE;
            selectedAmenities.clear();
            showAvailableOnly = false;
            
            // Apply reset filters
            filterDormitories(etSearch.getText().toString());
            
            Toast.makeText(this, "Filters reset", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
    
    /**
     * Show interactive game-like tutorial for first-time users
     */
    private void showInteractiveTutorial() {
        com.rct.dormfinder.utils.TutorialHelper tutorialHelper = 
            new com.rct.dormfinder.utils.TutorialHelper(this);
        
        // Build per-account tutorial key (fallback to global key if user is null)
        String tutorialKey;
        {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getUid() != null) {
                tutorialKey = user.getUid() + "_student_home_tutorial";
            } else {
                tutorialKey = "student_home_tutorial";
            }
        }

        // Only show if this account hasn't seen it before on this device
        if (!tutorialHelper.hasSeen(tutorialKey)) {
            // Wait for views to be laid out
            recyclerViewFeaturedDorms.post(() -> {
                com.rct.dormfinder.utils.TutorialHelper.TutorialStep[] steps = {
                    // Step 1: Welcome
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "Welcome to DormFinder! 🎉",
                        "Let's take a quick tour to help you find your perfect dorm!"
                    ),
                    
                    // Step 2: Search bar
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "Search for Dorms",
                        "Type here to search for dormitories by name, address, or city",
                        (etSearch != null && etSearch.getParent() instanceof View)
                            ? (View) etSearch.getParent()
                            : etSearch
                    ),
                    
                    // Step 3: Filter button
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "Apply Filters 🔍",
                        "Tap here to filter by city, price, amenities, and availability",
                        btnFilter
                    ),
                    
                    // Step 4: Map view
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "View on Map 🗺️",
                        "See all dormitories on a map to find ones near your school!",
                        btnViewOnMap
                    ),

                    // Step 5: Notifications
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "Notifications 🔔",
                        "Tap here to view alerts and updates from landlords and the app",
                        ivNotification
                    ),

                    // Step 6: Help & Guide
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "Help & Guide ❓",
                        "Open the app guide for tips, best practices, and how‑tos",
                        ivGuide
                    ),
                    
                    // Step 7: Featured dorms (if visible)
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "Featured Dorms ⭐",
                        "These are highly-rated dormitories with verified reviews. Swipe to see more!",
                        recyclerViewFeaturedDorms.getVisibility() == View.VISIBLE ? 
                            recyclerViewFeaturedDorms : null
                    ),
                    
                    // Step 8: Messages
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "Chat with Landlords 💬",
                        "Tap here to view your messages and chat with property owners",
                        bottomNavigation != null && bottomNavigation.findViewById(R.id.nav_messages) != null
                            ? bottomNavigation.findViewById(R.id.nav_messages)
                            : bottomNavigation
                    ),
                    
                    // Step 9: Bookings
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "My Bookings 📋",
                        "Track your booking requests and reservations here",
                        bottomNavigation != null && bottomNavigation.findViewById(R.id.nav_bookings) != null
                            ? bottomNavigation.findViewById(R.id.nav_bookings)
                            : bottomNavigation
                    ),
                    
                    // Step 10: Favorites
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "Favorites ❤️",
                        "Save dorms you like by tapping the heart icon. Access them here anytime!",
                        bottomNavigation != null && bottomNavigation.findViewById(R.id.nav_favorites) != null
                            ? bottomNavigation.findViewById(R.id.nav_favorites)
                            : bottomNavigation
                    ),
                    
                    // Step 11: Profile
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "Profile 👤",
                        "View your profile information and log out here",
                        bottomNavigation != null && bottomNavigation.findViewById(R.id.nav_profile) != null
                            ? bottomNavigation.findViewById(R.id.nav_profile)
                            : bottomNavigation
                    ),
                    
                    // Step 12: Ready!
                    new com.rct.dormfinder.utils.TutorialHelper.TutorialStep(
                        "You're All Set! 🏠",
                        "Start searching for your perfect dorm. You can revisit this guide anytime from your profile!"
                    )
                };
                
                tutorialHelper.startTutorial(tutorialKey, steps, () -> {
                    // Tutorial completed - mark as seen
                    tutorialHelper.markAsSeen(tutorialKey);
                    Log.d(TAG, "Tutorial completed and marked as seen");
                });
            });
        }
    }
    
}
