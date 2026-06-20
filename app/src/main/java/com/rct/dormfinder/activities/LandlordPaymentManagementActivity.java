package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.rct.dormfinder.adapters.PaymentAdapter;
import com.rct.dormfinder.models.Payment;
import com.rct.dormfinder.services.PaymentService;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LandlordPaymentManagementActivity extends BaseActivity {
    private ImageButton btnBack;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView tvEmptyState, tvTotalEarned, tvPendingCount;

    private PaymentAdapter adapter;
    private List<Payment> payments;
    private PaymentService paymentService;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landlord_payment_management);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        paymentService = new PaymentService(this);
        payments = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        setupListeners();
        loadPayments();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        applyTopInsets(insets, R.id.headerLayout);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvTotalEarned = findViewById(R.id.tvTotalEarned);
        tvPendingCount = findViewById(R.id.tvPendingCount);
    }

    private void setupRecyclerView() {
        adapter = new PaymentAdapter(this, payments, payment -> showPaymentActions(payment));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(this::loadPayments);
    }

    private void loadPayments() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        String landlordId = auth.getCurrentUser().getUid();

        paymentService.getLandlordPayments(landlordId, new PaymentService.PaymentListCallback() {
            @Override
            public void onSuccess(List<Payment> loadedPayments) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                payments.clear();
                payments.addAll(loadedPayments);
                adapter.updatePayments(payments);

                if (payments.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                    calculateStats();
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(LandlordPaymentManagementActivity.this, 
                    "Error loading payments: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
    }

    private void calculateStats() {
        double totalEarned = 0;
        int pendingCount = 0;

        for (Payment payment : payments) {
            if ("completed".equals(payment.getStatus())) {
                totalEarned += payment.getAmount();
            } else if ("pending".equals(payment.getStatus())) {
                pendingCount++;
            }
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        tvTotalEarned.setText(currencyFormat.format(totalEarned));
        tvPendingCount.setText(String.valueOf(pendingCount) + " pending");
    }

    private void showPaymentActions(Payment payment) {
        if ("pending".equals(payment.getStatus()) && "cash".equals(payment.getPaymentMethod())) {
            showCashPaymentVerification(payment);
        } else {
            showPaymentDetails(payment);
        }
    }

    private void showCashPaymentVerification(Payment payment) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        
        String message = "Student: " + payment.getStudentName() + "\n" +
                        "Dormitory: " + payment.getDormitoryName() + "\n" +
                        "Amount: " + currencyFormat.format(payment.getAmount()) + "\n" +
                        "Payment Method: Cash\n" +
                        "Reference: " + payment.getReferenceNumber();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verify Cash Payment");
        builder.setMessage(message);
        
        if (payment.getPaymentProof() != null && !payment.getPaymentProof().isEmpty()) {
            builder.setNeutralButton("View Proof", (dialog, which) -> viewPaymentProof(payment));
        }
        
        builder.setPositiveButton("✓ Approve", (dialog, which) -> approvePayment(payment));
        builder.setNegativeButton("✗ Reject", (dialog, which) -> showRejectDialog(payment));
        builder.show();
    }

    private void viewPaymentProof(Payment payment) {
        Intent intent = new Intent(this, ImageViewActivity.class);
        intent.putExtra("imageUrl", payment.getPaymentProof());
        intent.putExtra("title", "Payment Proof - " + payment.getStudentName());
        startActivity(intent);
    }

    private void approvePayment(Payment payment) {
        progressBar.setVisibility(View.VISIBLE);
        
        paymentService.updatePaymentStatus(payment.getPaymentId(), "completed", 
            new PaymentService.PaymentStatusCallback() {
                @Override
                public void onSuccess() {
                    db.collection("bookings")
                        .document(payment.getBookingId())
                        .update("status", "confirmed",
                               "paymentStatus", "paid",
                               "paymentId", payment.getPaymentId(),
                               "paymentDate", System.currentTimeMillis(),
                               "confirmedDate", System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> decreaseAvailableRooms(payment.getDormitoryId(), payment))
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(LandlordPaymentManagementActivity.this, 
                                "Payment approved but error updating booking: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        });
                }

                @Override
                public void onFailure(String error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LandlordPaymentManagementActivity.this, 
                        "Error approving payment: " + error, Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void decreaseAvailableRooms(String dormitoryId, Payment payment) {
        db.collection("dormitories").document(dormitoryId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long currentRooms = documentSnapshot.getLong("availableRooms");
                    if (currentRooms != null && currentRooms > 0) {
                        db.collection("dormitories").document(dormitoryId)
                            .update("availableRooms", currentRooms - 1)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(LandlordPaymentManagementActivity.this, 
                                    "Payment approved and booking confirmed!", Toast.LENGTH_SHORT).show();
                                sendPaymentApprovedNotification(payment);
                                loadPayments();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(LandlordPaymentManagementActivity.this, 
                                    "Booking confirmed but error updating rooms", Toast.LENGTH_SHORT).show();
                                sendPaymentApprovedNotification(payment);
                                loadPayments();
                            });
                    }
                }
            });
    }

    private void showRejectDialog(Payment payment) {
        final EditText input = new EditText(this);
        input.setHint("Reason for rejection");
        input.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(this)
            .setTitle("Reject Payment")
            .setMessage("Please provide a reason for rejecting this payment:")
            .setView(input)
            .setPositiveButton("Reject", (dialog, which) -> {
                String reason = input.getText().toString().trim();
                if (reason.isEmpty()) reason = "Payment proof not clear or invalid";
                rejectPayment(payment, reason);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void rejectPayment(Payment payment, String reason) {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("payments").document(payment.getPaymentId())
            .update("status", "failed", "failureReason", reason)
            .addOnSuccessListener(aVoid -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Payment rejected", Toast.LENGTH_SHORT).show();
                sendPaymentRejectedNotification(payment, reason);
                loadPayments();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error rejecting payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showPaymentDetails(Payment payment) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());

        StringBuilder details = new StringBuilder();
        details.append("Student: ").append(payment.getStudentName()).append("\n");
        details.append("Email: ").append(payment.getStudentEmail()).append("\n\n");
        details.append("Dormitory: ").append(payment.getDormitoryName()).append("\n\n");
        details.append("Amount: ").append(currencyFormat.format(payment.getAmount())).append("\n");
        details.append("Payment Method: ").append(payment.getPaymentMethod().toUpperCase()).append("\n");
        details.append("Reference: ").append(payment.getReferenceNumber()).append("\n");
        details.append("Date: ").append(dateFormat.format(new Date(payment.getTimestamp()))).append("\n");
        details.append("Status: ").append(getStatusText(payment.getStatus()));

        new AlertDialog.Builder(this)
            .setTitle("Payment Details")
            .setMessage(details.toString())
            .setPositiveButton("OK", null)
            .show();
    }

    private void sendPaymentApprovedNotification(Payment payment) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("recipientId", payment.getStudentId());
        notification.put("senderId", auth.getCurrentUser().getUid());
        notification.put("type", "payment_approved");
        notification.put("title", "Payment Approved!");
        notification.put("message", "Your payment for " + payment.getDormitoryName() + " has been verified and approved.");
        notification.put("paymentId", payment.getPaymentId());
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);
        db.collection("notifications").add(notification);
    }

    private void sendPaymentRejectedNotification(Payment payment, String reason) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("recipientId", payment.getStudentId());
        notification.put("senderId", auth.getCurrentUser().getUid());
        notification.put("type", "payment_rejected");
        notification.put("title", "Payment Verification Failed");
        notification.put("message", "Your payment for " + payment.getDormitoryName() + " was rejected. Reason: " + reason);
        notification.put("paymentId", payment.getPaymentId());
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);
        db.collection("notifications").add(notification);
    }

    private String getStatusText(String status) {
        switch (status) {
            case "completed": return "Completed";
            case "pending": return "Pending Verification";
            case "failed": return "Rejected";
            default: return status;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPayments();
    }
}
