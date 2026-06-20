package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.User;
import com.rct.dormfinder.utils.NotificationHelper;
import com.rct.dormfinder.utils.NavigationHelper;

public class LandlordHomeActivity extends BaseActivity {

    private TextView tvUserName, tvTotalDorms, tvTotalBookings, tvPendingBookings, tvNotificationBadge, tvMessageBadgeCard;
    private ImageView ivHelp, ivNotification;
    private CardView cardBookingRequests, cardMessages, cardPayments, cardReviews;
    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private NotificationHelper notificationHelper;
    private String currentUserId;
    private User currentUser;
    private com.google.firebase.firestore.ListenerRegistration notificationListener;
    private com.google.firebase.firestore.ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landlord_home);

        initializeViews();
        setupFirebase();
        setupListeners();
        setupBottomNavigation();
        loadUserData();
        loadTotalDormsCount();
        loadBookingStats();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply bottom insets to bottom navigation
        applyBottomInsets(insets, R.id.bottomNavigation);
        
        // Apply top insets to the header layout
        View headerLayout = findViewById(R.id.headerLayout);
        if (headerLayout != null) {
            android.view.ViewGroup.MarginLayoutParams params = 
                (android.view.ViewGroup.MarginLayoutParams) headerLayout.getLayoutParams();
            params.topMargin = insets.top;
            headerLayout.setLayoutParams(params);
        }
    }

    private void initializeViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvTotalDorms = findViewById(R.id.tvTotalDorms);
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvPendingBookings = findViewById(R.id.tvPendingBookings);
        ivHelp = findViewById(R.id.ivHelp);
        ivNotification = findViewById(R.id.ivNotification);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        tvMessageBadgeCard = findViewById(R.id.tvMessageBadgeCard);

        cardBookingRequests = findViewById(R.id.cardBookingRequests);
        cardMessages = findViewById(R.id.cardMessages);
        cardPayments = findViewById(R.id.cardPayments);
        cardReviews = findViewById(R.id.cardReviews);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        notificationHelper = new NotificationHelper(this);

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        } else {
            // User not authenticated, redirect to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
    }

    private void setupListeners() {
        cardBookingRequests.setOnClickListener(v -> {
            startActivity(new Intent(this, BookingManagementActivity.class));
        });

        cardMessages.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatListActivity.class));
        });

        cardPayments.setOnClickListener(v -> {
            startActivity(new Intent(this, LandlordPaymentManagementActivity.class));
        });

        cardReviews.setOnClickListener(v -> {
            startActivity(new Intent(this, LandlordReviewsActivity.class));
        });
        
        ivHelp.setOnClickListener(v -> {
            startActivity(new Intent(this, AppGuideActivity.class));
        });
        
        ivNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationActivity.class));
        });
        
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUserData();
            loadTotalDormsCount();
            loadBookingStats();
            // Notification count is already real-time, no need to manually refresh
        });
        
        // Set refresh colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.orange_primary,
            R.color.orange_light,
            R.color.orange_primary
        );
    }
    
    private void setupBottomNavigation() {
        NavigationHelper.setupLandlordBottomNavigation(this, bottomNavigation, R.id.nav_home);
    }

    private void loadUserData() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                            tvUserName.setText(currentUser.getName());

                            // Update welcome message based on time
                            java.util.Calendar calendar = java.util.Calendar.getInstance();
                            int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
                            String greeting;
                            if (hour < 12) {
                                greeting = "Good morning!";
                            } else if (hour < 18) {
                                greeting = "Good afternoon!";
                            } else {
                                greeting = "Good evening!";
                            }

                            TextView tvWelcome = findViewById(R.id.tvWelcome);
                            tvWelcome.setText(greeting);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void loadTotalDormsCount() {
        if (currentUserId == null) {
            android.util.Log.w("LandlordHome", "Cannot load dormitories - currentUserId is null");
            return;
        }

        android.util.Log.d("LandlordHome", "Starting to load dormitory count for user: " + currentUserId);

        // Get total count of dormitories
        db.collection("dormitories")
                .whereEqualTo("landlordId", currentUserId)
                .get()
                .addOnSuccessListener(allDocsSnapshot -> {
                    int totalDorms = allDocsSnapshot.size();
                    tvTotalDorms.setText(String.valueOf(totalDorms));
                    
                    android.util.Log.d("LandlordHome", "Total dormitories found: " + totalDorms);
                    
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LandlordHome", "Failed to get total dorms count", e);
                    tvTotalDorms.setText("0");
                    
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void loadBookingStats() {
        if (currentUserId == null) return;

        // Load total bookings
        db.collection("bookings")
                .whereEqualTo("landlordId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalBookings = queryDocumentSnapshots.size();
                    tvTotalBookings.setText(String.valueOf(totalBookings));

                    // Count pending bookings
                    int pendingCount = 0;
                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        String status = queryDocumentSnapshots.getDocuments().get(i).getString("status");
                        if ("pending".equals(status)) {
                            pendingCount++;
                        }
                    }
                    tvPendingBookings.setText(String.valueOf(pendingCount));
                    
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    tvTotalBookings.setText("0");
                    tvPendingBookings.setText("0");
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    /**
     * Setup real-time listener for notification count
     */
    private void setupNotificationListener() {
        android.util.Log.d("LandlordHome", "===== NOTIFICATION LISTENER SETUP CALLED =====");
        if (currentUserId == null) {
            android.util.Log.e("LandlordHome", "Cannot setup notification listener - currentUserId is NULL!");
            return;
        }
        android.util.Log.d("LandlordHome", "Setting up notification listener for user: " + currentUserId);
        
        // Remove existing listener if any
        if (notificationListener != null) {
            notificationListener.remove();
        }
        
        // Listen for real-time notification updates
        android.util.Log.d("LandlordHome", "Attaching Firestore snapshot listener...");
        // Note: Changed from "read" to "isRead" to match Notification model field name
        notificationListener = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    android.util.Log.d("LandlordHome", "Notification listener triggered!");
                    if (error != null) {
                        android.util.Log.e("LandlordHome", "Notification listener error: " + error.getMessage());
                        return;
                    }
                    
                    if (queryDocumentSnapshots != null) {
                        int unreadCount = queryDocumentSnapshots.size();
                        android.util.Log.d("LandlordHome", "Unread notifications count: " + unreadCount);
                        updateNotificationBadge(unreadCount);
                    } else {
                        android.util.Log.w("LandlordHome", "queryDocumentSnapshots is NULL!");
                    }
                });
    }
    
    /**
     * Update notification badge UI
     */
    private void updateNotificationBadge(int count) {
        android.util.Log.d("LandlordHome", "updateNotificationBadge called with count: " + count);
        runOnUiThread(() -> {
            android.util.Log.d("LandlordHome", "Updating badge on UI thread");
            if (tvNotificationBadge != null) {
                if (count > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }
        });
    }
    
    /**
     * Setup real-time listener for unread message count
     */
    private void setupMessageListener() {
        android.util.Log.d("LandlordHome", "===== MESSAGE LISTENER SETUP CALLED =====");
        if (currentUserId == null) {
            android.util.Log.e("LandlordHome", "Cannot setup message listener - currentUserId is NULL!");
            return;
        }
        android.util.Log.d("LandlordHome", "Setting up message listener for user: " + currentUserId);
        
        // Remove existing listener if any
        if (messageListener != null) {
            messageListener.remove();
        }
        
        // Listen for chats where user is landlord
        android.util.Log.d("LandlordHome", "Attaching Firestore message listener...");
        messageListener = db.collection("chats")
                .whereEqualTo("landlordId", currentUserId)
                .addSnapshotListener((landlordChats, error) -> {
                    android.util.Log.d("LandlordHome", "Message listener triggered!");
                    if (error != null) {
                        android.util.Log.e("LandlordHome", "Message listener error: " + error.getMessage());
                        return;
                    }
                    
                    int totalUnread = 0;
                    
                    if (landlordChats != null) {
                        for (int i = 0; i < landlordChats.size(); i++) {
                            // Changed from "unreadCount" to "landlordUnreadCount"
                            Long unreadCount = landlordChats.getDocuments().get(i).getLong("landlordUnreadCount");
                            if (unreadCount != null) {
                                totalUnread += unreadCount.intValue();
                            }
                        }
                    }
                    
                    android.util.Log.d("LandlordHome", "Total unread messages: " + totalUnread);
                    updateMessageBadge(totalUnread);
                });
    }
    
    /**
     * Update message badge UI
     */
    private void updateMessageBadge(int count) {
        android.util.Log.d("LandlordHome", "updateMessageBadge called with count: " + count);
        runOnUiThread(() -> {
            android.util.Log.d("LandlordHome", "Updating message badge on UI thread");
            if (tvMessageBadgeCard != null) {
                if (count > 0) {
                    tvMessageBadgeCard.setVisibility(View.VISIBLE);
                    tvMessageBadgeCard.setText(count > 99 ? "99+" : String.valueOf(count));
                } else {
                    tvMessageBadgeCard.setVisibility(View.GONE);
                }
            }
            
            // Update bottom navigation badge using NavigationHelper
            if (bottomNavigation != null) {
                NavigationHelper.updateBadge(bottomNavigation, R.id.nav_messages, count);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Re-select Home in bottom navigation when returning to this activity
        // Post to ensure view is fully rendered before setting selection
        if (bottomNavigation != null) {
            bottomNavigation.post(() -> {
                bottomNavigation.setSelectedItemId(R.id.nav_home);
                android.util.Log.d("LandlordHome", "Bottom navigation set to Home");
            });
        }
        
        // Don't reload on resume - user can swipe down to refresh manually
        
        // Start listening for notifications and messages
        setupNotificationListener();
        setupMessageListener();
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
    }
}
