package com.rct.dormfinder.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.BookingAdapter;
import com.rct.dormfinder.models.Booking;
import com.rct.dormfinder.utils.NavigationHelper;
import java.util.ArrayList;
import java.util.List;

public class BookingManagementActivity extends BaseActivity implements BookingAdapter.OnBookingActionListener {
    private ImageButton btnBack;
    private TextView tvEmptyState;
    private TabLayout tabLayout;
    private RecyclerView recyclerViewBookings;
    private SwipeRefreshLayout swipeRefreshLayout;
    private BottomNavigationView bottomNavigation;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BookingAdapter bookingAdapter;
    private List<Booking> allBookings;
    private List<Booking> filteredBookings;
    private String currentUserId;
    private String currentFilter = "pending";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_management);

        initializeViews();
        setupFirebase();
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        setupListeners();
        setupBottomNavigation();
        loadBookings();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        applyTopInsets(insets, R.id.headerLayout);
        applyBottomInsets(insets, R.id.bottomNavigation);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tabLayout = findViewById(R.id.tabLayout);
        recyclerViewBookings = findViewById(R.id.recyclerViewBookings);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Approved"));
        tabLayout.addTab(tabLayout.newTab().setText("Declined"));
        tabLayout.addTab(tabLayout.newTab().setText("All"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "pending";
                        break;
                    case 1:
                        currentFilter = "approved";
                        break;
                    case 2:
                        currentFilter = "declined";
                        break;
                    case 3:
                        currentFilter = "all";
                        break;
                }
                filterBookings();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        allBookings = new ArrayList<>();
        filteredBookings = new ArrayList<>();
        bookingAdapter = new BookingAdapter(filteredBookings, this, this);
        recyclerViewBookings.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBookings.setAdapter(bookingAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadBookings();
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_primary);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupBottomNavigation() {
        NavigationHelper.setupLandlordBottomNavigation(this, bottomNavigation, R.id.nav_requests);
    }

    private void loadBookings() {
        swipeRefreshLayout.setRefreshing(true);
        
        // Simplified query without orderBy to avoid index requirement
        db.collection("bookings")
                .whereEqualTo("landlordId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allBookings.clear();
                    
                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        Booking booking = queryDocumentSnapshots.getDocuments().get(i)
                                .toObject(Booking.class);
                        if (booking != null) {
                            booking.setBookingId(queryDocumentSnapshots.getDocuments().get(i).getId());
                            allBookings.add(booking);
                        }
                    }
                    
                    // Sort manually by request date (newest first)
                    allBookings.sort((b1, b2) -> Long.compare(b2.getRequestDate(), b1.getRequestDate()));
                    
                    filterBookings();
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    swipeRefreshLayout.setRefreshing(false);
                    android.util.Log.e("BookingManagement", "Failed to load bookings: " + e.getMessage());
                    Toast.makeText(this, "Failed to load bookings: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void filterBookings() {
        filteredBookings.clear();
        
        for (Booking booking : allBookings) {
            if ("all".equals(currentFilter)) {
                filteredBookings.add(booking);
            } else if ("approved".equals(currentFilter)) {
                // Show both approved and paid bookings in the approved tab
                if ("approved".equals(booking.getStatus()) || "paid".equals(booking.getStatus())) {
                    filteredBookings.add(booking);
                }
            } else if (currentFilter.equals(booking.getStatus())) {
                filteredBookings.add(booking);
            }
        }
        
        bookingAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredBookings.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerViewBookings.setVisibility(View.GONE);
            
            String emptyMessage;
            switch (currentFilter) {
                case "pending":
                    emptyMessage = "No pending booking requests";
                    break;
                case "approved":
                    emptyMessage = "No approved bookings";
                    break;
                case "declined":
                    emptyMessage = "No declined bookings";
                    break;
                default:
                    emptyMessage = "No booking requests yet";
                    break;
            }
            tvEmptyState.setText(emptyMessage);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerViewBookings.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onApproveBooking(Booking booking) {
        updateBookingStatus(booking, "approved", "Booking request approved!");
    }

    @Override
    public void onDeclineBooking(Booking booking) {
        updateBookingStatus(booking, "declined", "Booking request declined.");
    }

    @Override
    public void onCallStudent(Booking booking) {
        if (booking.getStudentPhone() != null && !booking.getStudentPhone().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + booking.getStudentPhone()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Student phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMessageStudent(Booking booking) {
        // Navigate to chat activity
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("other_user_id", booking.getStudentId());
        intent.putExtra("other_user_name", booking.getStudentName());
        intent.putExtra("dormitory_id", booking.getDormitoryId());
        intent.putExtra("dormitory_name", booking.getDormitoryName());
        startActivity(intent);
    }

    @Override
    public void onViewDormitory(Booking booking) {
        Intent intent = new Intent(this, DormitoryDetailActivity.class);
        intent.putExtra("dormitory_id", booking.getDormitoryId());
        startActivity(intent);
    }

    private void updateBookingStatus(Booking booking, String newStatus, String message) {
        booking.setStatus(newStatus);
        booking.setResponseDate(System.currentTimeMillis());
        
        db.collection("bookings").document(booking.getBookingId())
                .update("status", newStatus, "responseDate", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    
                    // Send notification to student
                    sendNotificationToStudent(booking, newStatus);
                    
                    // Update local data
                    filterBookings();
                    
                    // If approved, update dormitory availability
                    if ("approved".equals(newStatus)) {
                        updateDormitoryAvailability(booking.getDormitoryId());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update booking: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotificationToStudent(Booking booking, String status) {
        java.util.Map<String, Object> notification = new java.util.HashMap<>();
        notification.put("recipientId", booking.getStudentId());
        notification.put("senderId", currentUserId);
        notification.put("type", "booking_response");
        notification.put("title", "Booking Request " + status.substring(0, 1).toUpperCase() + status.substring(1));
        
        String notificationMessage;
        if ("approved".equals(status)) {
            notificationMessage = "Your booking request for " + booking.getDormitoryName() + " has been approved!";
        } else {
            notificationMessage = "Your booking request for " + booking.getDormitoryName() + " has been declined.";
        }
        
        notification.put("message", notificationMessage);
        notification.put("bookingId", booking.getBookingId());
        notification.put("dormitoryId", booking.getDormitoryId());
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> {
                    android.util.Log.d("BookingManagement", "Notification sent to student");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("BookingManagement", "Failed to send notification: " + e.getMessage());
                });
    }

    private void updateDormitoryAvailability(String dormitoryId) {
        db.collection("dormitories").document(dormitoryId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        com.rct.dormfinder.models.Dormitory dormitory = 
                                document.toObject(com.rct.dormfinder.models.Dormitory.class);
                        if (dormitory != null && dormitory.getAvailableRooms() > 0) {
                            dormitory.decrementAvailableRooms();
                            
                            db.collection("dormitories").document(dormitoryId)
                                    .set(dormitory)
                                    .addOnSuccessListener(aVoid -> {
                                        android.util.Log.d("BookingManagement", "Dormitory availability updated");
                                    });
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }
}
