package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.NotificationAdapter;
import com.rct.dormfinder.models.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends BaseActivity implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView recyclerViewNotifications;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notifications;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private LinearLayout layoutNoNotifications;
    private TextView tvMarkAllRead;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String userRole = null; // Cache user role (null until loaded)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        initializeViews();
        setupFirebase();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadNotifications();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to AppBarLayout
        View appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            android.view.ViewGroup.MarginLayoutParams params = 
                (android.view.ViewGroup.MarginLayoutParams) appBarLayout.getLayoutParams();
            params.topMargin = insets.top;
            appBarLayout.setLayoutParams(params);
        }
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);
        layoutNoNotifications = findViewById(R.id.tvNoNotifications);
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            loadUserRole(); // Load role immediately
        } else {
            finish();
            return;
        }
    }
    
    /**
     * Load and cache user role
     */
    private void loadUserRole() {
        android.util.Log.d("NotificationActivity", "Loading user role for: " + currentUserId);
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("userType"); // Changed from "role" to "userType"
                        if (role != null) {
                            userRole = role;
                            android.util.Log.d("NotificationActivity", "User role loaded successfully: " + userRole);
                        } else {
                            android.util.Log.e("NotificationActivity", "userType field is NULL in user document");
                            // Set default to prevent infinite loop
                            userRole = "student";
                        }
                    } else {
                        android.util.Log.e("NotificationActivity", "User document does not exist!");
                        // Set default to prevent infinite loop
                        userRole = "student";
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NotificationActivity", "Failed to load user role", e);
                    // Set default to prevent infinite loop
                    userRole = "student";
                });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }
    }

    private void setupRecyclerView() {
        notifications = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notifications, this, this);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(notificationAdapter);
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);

        tvMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void loadNotifications() {
        android.util.Log.d("NotificationActivity", "===== LOADING NOTIFICATIONS =====");
        android.util.Log.d("NotificationActivity", "Current User ID: " + currentUserId);
        
        progressBar.setVisibility(View.VISIBLE);
        layoutNoNotifications.setVisibility(View.GONE);

        // NOTE: Removed orderBy("createdAt") because some notifications have null timestamps
        // which causes Firestore to exclude them from results
        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("NotificationActivity", "Query SUCCESS! Documents found: " + queryDocumentSnapshots.size());
                    notifications.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        android.util.Log.d("NotificationActivity", "--- Processing notification ---");
                        android.util.Log.d("NotificationActivity", "Doc ID: " + document.getId());
                        android.util.Log.d("NotificationActivity", "Title: " + document.getString("title"));
                        android.util.Log.d("NotificationActivity", "Message: " + document.getString("message"));
                        android.util.Log.d("NotificationActivity", "Type: " + document.getString("type"));
                        android.util.Log.d("NotificationActivity", "isRead: " + document.getBoolean("isRead"));
                        android.util.Log.d("NotificationActivity", "userId: " + document.getString("userId"));
                        android.util.Log.d("NotificationActivity", "createdAt: " + document.getTimestamp("createdAt"));
                        
                        Notification notification = document.toObject(Notification.class);
                        notification.setNotificationId(document.getId());
                        
                        // Debug: Log the actual isRead value after deserialization
                        android.util.Log.d("NotificationActivity", "After deserialization - isRead(): " + notification.isRead());
                        android.util.Log.d("NotificationActivity", "After deserialization - getIsRead(): " + notification.getIsRead());
                        
                        notifications.add(notification);
                        
                        android.util.Log.d("NotificationActivity", "Notification object created and added to list");
                        
                        // NOTE: Notifications are now only marked as read when clicked
                        // This allows the badge counter to stay accurate
                    }

                    android.util.Log.d("NotificationActivity", "Total notifications in list: " + notifications.size());
                    
                    // Sort by createdAt manually (handle null timestamps)
                    notifications.sort((n1, n2) -> {
                        if (n1.getCreatedAt() == null && n2.getCreatedAt() == null) return 0;
                        if (n1.getCreatedAt() == null) return 1; // null timestamps go to end
                        if (n2.getCreatedAt() == null) return -1;
                        return n2.getCreatedAt().compareTo(n1.getCreatedAt()); // Descending order
                    });
                    
                    android.util.Log.d("NotificationActivity", "Adapter item count BEFORE notify: " + notificationAdapter.getItemCount());
                    
                    notificationAdapter.notifyDataSetChanged();
                    
                    android.util.Log.d("NotificationActivity", "Adapter item count AFTER notify: " + notificationAdapter.getItemCount());
                    android.util.Log.d("NotificationActivity", "RecyclerView visibility: " + recyclerViewNotifications.getVisibility());
                    
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    if (notifications.isEmpty()) {
                        android.util.Log.d("NotificationActivity", "No notifications - showing empty view");
                        layoutNoNotifications.setVisibility(View.VISIBLE);
                        tvMarkAllRead.setVisibility(View.GONE);
                    } else {
                        android.util.Log.d("NotificationActivity", "Has notifications - hiding empty view");
                        layoutNoNotifications.setVisibility(View.GONE);
                        // Show mark all read only if there are unread notifications
                        boolean hasUnread = notifications.stream().anyMatch(n -> !n.isRead());
                        tvMarkAllRead.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NotificationActivity", "Query FAILED!", e);
                    android.util.Log.e("NotificationActivity", "Error message: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    layoutNoNotifications.setVisibility(View.VISIBLE);
                });
    }

    private void markAllAsRead() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Update all unread notifications
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                db.collection("notifications")
                        .document(notification.getNotificationId())
                        .update("isRead", true)  // Changed to match model field name
                        .addOnSuccessListener(aVoid -> {
                            notification.setRead(true);
                        });
            }
        }

        // Update UI after a short delay
        new android.os.Handler().postDelayed(() -> {
            notificationAdapter.notifyDataSetChanged();
            tvMarkAllRead.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
        }, 500);
    }

    @Override
    public void onNotificationClick(Notification notification) {
        android.util.Log.d("NotificationActivity", "===== NOTIFICATION CLICKED =====");
        android.util.Log.d("NotificationActivity", "Type: " + notification.getType());
        android.util.Log.d("NotificationActivity", "Related ID: " + notification.getRelatedId());
        android.util.Log.d("NotificationActivity", "Title: " + notification.getTitle());
        
        // Mark as read
        if (!notification.isRead()) {
            db.collection("notifications")
                    .document(notification.getNotificationId())
                    .update("isRead", true)
                    .addOnSuccessListener(aVoid -> {
                        notification.setRead(true);
                        notificationAdapter.notifyDataSetChanged();
                    });
        }

        // Navigate based on notification type and relatedId
        navigateToNotificationTarget(notification);
    }
    
    /**
     * Navigate to the appropriate screen based on notification type
     */
    private void navigateToNotificationTarget(Notification notification) {
        // If userRole is not loaded yet, wait for it (with timeout)
        if (userRole == null) {
            android.util.Log.w("NotificationActivity", "User role not loaded yet, delaying navigation");
            
            // Check if we've been waiting too long (more than 5 seconds)
            final long MAX_WAIT_TIME = 5000; // 5 seconds
            final String startTimeKey = "navigation_start_" + notification.getNotificationId();
            
            // Get or set start time
            Long startTime = (Long) getIntent().getSerializableExtra(startTimeKey);
            if (startTime == null) {
                startTime = System.currentTimeMillis();
                getIntent().putExtra(startTimeKey, startTime);
            }
            
            // If timeout exceeded, use default and proceed
            if (System.currentTimeMillis() - startTime > MAX_WAIT_TIME) {
                android.util.Log.e("NotificationActivity", "Timeout waiting for user role, defaulting to student");
                userRole = "student"; // Default to student to prevent infinite loop
                Toast.makeText(this, "Navigation may not be accurate. Please try again.", Toast.LENGTH_SHORT).show();
                // Don't return, continue to navigation
            } else {
                // Retry after a short delay
                new android.os.Handler().postDelayed(() -> navigateToNotificationTarget(notification), 300);
                return;
            }
        }
        
        Intent intent = null;
        String type = notification.getType();
        String relatedId = notification.getRelatedId();
        
        android.util.Log.d("NotificationActivity", "Navigating for type: " + type + " as role: " + userRole);
        
        if (type == null) {
            android.util.Log.w("NotificationActivity", "Notification type is NULL, no navigation");
            Toast.makeText(this, "Notification details unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        
        switch (type.toLowerCase()) {
            case "booking_request":
            case "booking_approved":
            case "booking_rejected":
            case "booking_cancelled":
            case "booking":
                // Navigate to bookings list
                // For landlords -> BookingManagementActivity
                // For students -> StudentBookingsActivity
                intent = userRole.equals("landlord") ? 
                        new Intent(this, BookingManagementActivity.class) : 
                        new Intent(this, StudentBookingsActivity.class);
                
                // If we have specific booking ID, pass it to highlight/scroll to it
                if (relatedId != null && !relatedId.isEmpty()) {
                    intent.putExtra("bookingId", relatedId);
                    intent.putExtra("highlightBooking", true);
                }
                android.util.Log.d("NotificationActivity", "Navigating to booking screen with ID: " + relatedId);
                break;
                
            case "payment_received":
            case "payment_confirmed":
            case "payment_pending":
            case "payment":
                // Navigate to payment history/management
                if (userRole.equals("landlord")) {
                    intent = new Intent(this, LandlordPaymentManagementActivity.class);
                } else {
                    intent = new Intent(this, PaymentHistoryActivity.class);
                }
                
                if (relatedId != null && !relatedId.isEmpty()) {
                    intent.putExtra("paymentId", relatedId);
                    intent.putExtra("highlightPayment", true);
                }
                android.util.Log.d("NotificationActivity", "Navigating to payment screen");
                break;
                
            case "message":
            case "new_message":
            case "chat":
                // Navigate to chat
                if (relatedId != null && !relatedId.isEmpty()) {
                    // relatedId could be chatId or userId
                    // Try to open specific chat
                    intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("chatId", relatedId);
                    android.util.Log.d("NotificationActivity", "Navigating to specific chat: " + relatedId);
                } else {
                    // Open chat list
                    intent = new Intent(this, ChatListActivity.class);
                    android.util.Log.d("NotificationActivity", "Navigating to chat list");
                }
                break;
                
            case "review":
            case "new_review":
            case "review_added":
            case "review_reply": // Handle review replies
                // For landlords: Navigate to LandlordReviewsActivity with specific dorm filter
                if (userRole.equals("landlord")) {
                    if (relatedId != null && !relatedId.isEmpty()) {
                        // relatedId is dormitoryId - navigate to landlord reviews filtered by this dorm
                        intent = new Intent(this, LandlordReviewsActivity.class);
                        intent.putExtra("dormitoryId", relatedId);
                        intent.putExtra("filterByDormitory", true);
                        android.util.Log.d("NotificationActivity", "Navigating to landlord reviews for dorm: " + relatedId);
                    } else {
                        // No specific dorm, show all reviews
                        intent = new Intent(this, LandlordReviewsActivity.class);
                        android.util.Log.d("NotificationActivity", "Navigating to all landlord reviews");
                    }
                } else {
                    // For students: Navigate to dormitory detail with reviews tab
                    if (relatedId != null && !relatedId.isEmpty()) {
                        intent = new Intent(this, DormitoryDetailActivity.class);
                        intent.putExtra("dormitory_id", relatedId);  // Fixed: Use correct key
                        intent.putExtra("scrollToReviews", true);
                        android.util.Log.d("NotificationActivity", "Navigating to dorm reviews: " + relatedId);
                    } else {
                        // No relatedId available - this shouldn't happen, but handle it gracefully
                        android.util.Log.w("NotificationActivity", "Review notification missing dormitoryId - cannot navigate");
                        Toast.makeText(this, "Unable to open review details. Dormitory information is missing.", Toast.LENGTH_LONG).show();
                        // Just mark as read without navigation
                        return;
                    }
                }
                break;
                
            case "dormitory":
            case "dormitory_update":
            case "dormitory_approved":
            case "dormitory_rejected":
                if (relatedId != null && !relatedId.isEmpty()) {
                    // Navigate to dormitory details
                    intent = new Intent(this, DormitoryDetailActivity.class);
                    intent.putExtra("dormitory_id", relatedId);  // Fixed: Use correct key
                    android.util.Log.d("NotificationActivity", "Navigating to dorm detail: " + relatedId);
                } else if (userRole.equals("landlord")) {
                    // Navigate to my dormitories
                    intent = new Intent(this, MyDormitoriesActivity.class);
                }
                break;
                
            case "favorite":
            case "wishlist":
                // Navigate to favorites
                intent = new Intent(this, FavoritesActivity.class);
                android.util.Log.d("NotificationActivity", "Navigating to favorites");
                break;
                
            case "general":
            case "announcement":
            case "system":
            default:
                // For general notifications, just mark as read
                android.util.Log.d("NotificationActivity", "General notification, no navigation");
                Toast.makeText(this, "Notification marked as read", Toast.LENGTH_SHORT).show();
                return;
        }

        if (intent != null) {
            android.util.Log.d("NotificationActivity", "Starting activity: " + intent.getComponent());
            try {
                startActivity(intent);
            } catch (Exception e) {
                android.util.Log.e("NotificationActivity", "Failed to start activity", e);
                Toast.makeText(this, "Unable to open notification", Toast.LENGTH_SHORT).show();
            }
        } else {
            android.util.Log.w("NotificationActivity", "No intent created for notification type: " + type);
        }
    }
    


    @Override
    public void onNotificationDelete(Notification notification) {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("notifications")
                .document(notification.getNotificationId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    notifications.remove(notification);
                    notificationAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();

                    if (notifications.isEmpty()) {
                        layoutNoNotifications.setVisibility(View.VISIBLE);
                        tvMarkAllRead.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to delete notification", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }
}
