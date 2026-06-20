package com.rct.dormfinder.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.StudentBookingAdapter;
import com.rct.dormfinder.models.Booking;
import com.rct.dormfinder.utils.NavigationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class StudentBookingsActivity extends BaseActivity {
    private static final String TAG = "StudentBookings";
    private static final int REQUEST_CODE_PAYMENT = 1001;
    
    private RecyclerView recyclerView;
    private StudentBookingAdapter adapter;
    private List<Booking> bookings;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private ImageView ivPayments;
    
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_bookings);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        bookings = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
        loadBookings();
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        NavigationHelper.setupStudentBottomNavigation(this, bottomNavigation, R.id.nav_bookings);
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to header
        applyTopInsets(insets, R.id.headerLayout);
        
        // Apply bottom insets to bottom navigation
        applyBottomInsets(insets, R.id.bottomNavigation);
        
        // Add padding to SwipeRefreshLayout to account for header and bottom nav
        View swipeRefresh = findViewById(R.id.swipeRefresh);
        View headerCard = findViewById(R.id.headerCard);
        View bottomNav = findViewById(R.id.bottomNavigation);
        
        if (swipeRefresh != null && headerCard != null && bottomNav != null) {
            headerCard.post(() -> {
                int headerHeight = headerCard.getHeight();
                bottomNav.post(() -> {
                    int bottomNavHeight = bottomNav.getHeight();
                    
                    swipeRefresh.setPadding(
                        0,
                        headerHeight,  // Top padding to clear header
                        0,
                        bottomNavHeight  // Bottom padding to clear bottom nav
                    );
                    
                    android.util.Log.d("StudentBookings", "Applied padding - Top: " + headerHeight + "px, Bottom: " + bottomNavHeight + "px");
                });
            });
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ivPayments = findViewById(R.id.ivPayments);
    }

    private void setupRecyclerView() {
        adapter = new StudentBookingAdapter(bookings, this, new StudentBookingAdapter.OnStudentBookingActionListener() {
            @Override
            public void onViewDormitory(Booking booking) {
                Intent intent = new Intent(StudentBookingsActivity.this, DormitoryDetailActivity.class);
                intent.putExtra("dormitory_id", booking.getDormitoryId());
                startActivity(intent);
            }

            @Override
            public void onMessageLandlord(Booking booking) {
                Intent intent = new Intent(StudentBookingsActivity.this, ChatActivity.class);
                intent.putExtra("recipientId", booking.getLandlordId());
                intent.putExtra("recipientName", "Landlord");
                startActivity(intent);
            }

            @Override
            public void onCancelBooking(Booking booking) {
                showCancelConfirmation(booking);
            }

            @Override
            public void onPayNow(Booking booking) {
                openPaymentActivity(booking);
            }

            @Override
            public void onViewPayment(Booking booking) {
                viewPaymentDetails(booking);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        ivPayments.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentHistoryActivity.class);
            startActivity(intent);
        });
        
        swipeRefresh.setOnRefreshListener(this::loadBookings);
    }

    private void loadBookings() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        String userId = auth.getCurrentUser().getUid();
        
        db.collection("bookings")
            .whereEqualTo("studentId", userId)
            .orderBy("requestDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                bookings.clear();
                for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                    Booking booking = document.toObject(Booking.class);
                    if (booking != null) {
                        bookings.add(booking);
                    }
                }
                
                adapter.updateBookings(bookings);
                
                if (bookings.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                }
                
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading bookings: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            });
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText("No bookings yet.\n\nStart exploring dormitories and make your first booking request!");
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
    }

    private void openPaymentActivity(Booking booking) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("bookingId", booking.getBookingId());
        startActivityForResult(intent, REQUEST_CODE_PAYMENT);
    }

    private void viewPaymentDetails(Booking booking) {
        String paymentStatus = booking.getPaymentStatus();
        String paymentId = booking.getPaymentId();
        
        // Log for debugging
        android.util.Log.d(TAG, "viewPaymentDetails - PaymentStatus: " + paymentStatus + ", PaymentId: " + paymentId);
        
        if (paymentId != null && !paymentId.isEmpty()) {
            // Direct navigation to specific payment
            Intent intent = new Intent(this, PaymentHistoryActivity.class);
            intent.putExtra("paymentId", paymentId);
            startActivity(intent);
        } else if ("paid".equalsIgnoreCase(paymentStatus)) {
            // Payment is marked as paid but paymentId is missing - sync issue
            Toast.makeText(this, "Payment completed! Check Payment History for details.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, PaymentHistoryActivity.class);
            intent.putExtra("bookingId", booking.getBookingId());
            startActivity(intent);
        } else if ("pending".equalsIgnoreCase(paymentStatus)) {
            // Payment pending but record might not be synced yet
            Toast.makeText(this, "Payment verification in progress. Check Payment History for details.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, PaymentHistoryActivity.class);
            intent.putExtra("bookingId", booking.getBookingId());
            startActivity(intent);
        } else if ("unpaid".equalsIgnoreCase(paymentStatus) || paymentStatus == null || paymentStatus.isEmpty()) {
            // Booking is approved but payment hasn't been initiated
            Toast.makeText(this, "Please proceed with payment first", Toast.LENGTH_SHORT).show();
        } else {
            // Unknown payment status - still try to help user
            Toast.makeText(this, "Opening Payment History. Status: " + paymentStatus, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, PaymentHistoryActivity.class);
            intent.putExtra("bookingId", booking.getBookingId());
            startActivity(intent);
        }
    }

    private void showCancelConfirmation(Booking booking) {
        new AlertDialog.Builder(this)
            .setTitle("Cancel Booking Request")
            .setMessage("Are you sure you want to cancel this booking request for " + 
                       booking.getDormitoryName() + "?")
            .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelBooking(booking))
            .setNegativeButton("No", null)
            .show();
    }

    private void cancelBooking(Booking booking) {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("bookings")
            .document(booking.getBookingId())
            .update("status", "cancelled")
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show();
                loadBookings(); // Refresh the list
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error cancelling booking: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PAYMENT && resultCode == Activity.RESULT_OK) {
            // Payment completed, refresh bookings
            Toast.makeText(this, "Payment completed successfully!", Toast.LENGTH_SHORT).show();
            loadBookings();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // CRITICAL: Re-select Bookings ONLY after view is rendered to prevent race conditions
        if (bottomNavigation != null) {
            bottomNavigation.post(() -> {
                bottomNavigation.setSelectedItemId(R.id.nav_bookings);
                android.util.Log.d(TAG, "✅ Bottom nav set to Bookings in onResume");
            });
        }
        
        // Refresh bookings when returning to this activity
        loadBookings();
    }
}
