package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.PaymentAdapter;
import com.rct.dormfinder.models.Payment;
import com.rct.dormfinder.services.PaymentService;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryActivity extends AppCompatActivity {
    private static final String TAG = "PaymentHistory";

    private ImageButton btnBack;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View tvEmptyState;
    private TextView tvTotalPaid, tvTotalPayments;
    private View layoutStats;

    private PaymentAdapter adapter;
    private List<Payment> payments;
    private PaymentService paymentService;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        auth = FirebaseAuth.getInstance();
        paymentService = new PaymentService(this);
        payments = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        setupListeners();
        loadPayments();
        
        // Check if we need to highlight a specific payment
        handleIntentExtras();
    }
    
    private void handleIntentExtras() {
        Intent intent = getIntent();
        String paymentId = intent.getStringExtra("paymentId");
        String bookingId = intent.getStringExtra("bookingId");
        
        if (paymentId != null) {
            android.util.Log.d(TAG, "Highlighting payment with ID: " + paymentId);
            // Will highlight after payments are loaded
        } else if (bookingId != null) {
            android.util.Log.d(TAG, "Showing payments for booking ID: " + bookingId);
            // Will filter after payments are loaded
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);
        tvTotalPayments = findViewById(R.id.tvTotalPayments);
        layoutStats = findViewById(R.id.layoutStats);
    }

    private void setupRecyclerView() {
        adapter = new PaymentAdapter(this, payments, payment -> showPaymentDetails(payment));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        swipeRefresh.setOnRefreshListener(() -> loadPayments());
    }

    private void loadPayments() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        String userId = auth.getCurrentUser().getUid();

        paymentService.getStudentPayments(userId, new PaymentService.PaymentListCallback() {
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
                    
                    // Scroll to specific payment if requested
                    scrollToRequestedPayment();
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PaymentHistoryActivity.this, 
                    "Error loading payments: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void scrollToRequestedPayment() {
        Intent intent = getIntent();
        String paymentId = intent.getStringExtra("paymentId");
        String bookingId = intent.getStringExtra("bookingId");
        
        if (paymentId != null) {
            // Find and scroll to specific payment
            for (int i = 0; i < payments.size(); i++) {
                if (paymentId.equals(payments.get(i).getPaymentId())) {
                    recyclerView.scrollToPosition(i);
                    // Optionally show the payment details
                    recyclerView.postDelayed(() -> {
                        Toast.makeText(this, "Payment found! Tap to view details.", Toast.LENGTH_SHORT).show();
                    }, 500);
                    return;
                }
            }
            Toast.makeText(this, "Payment record not found in history", Toast.LENGTH_SHORT).show();
        } else if (bookingId != null) {
            // Find payment(s) for this booking
            boolean found = false;
            for (int i = 0; i < payments.size(); i++) {
                if (bookingId.equals(payments.get(i).getBookingId())) {
                    recyclerView.scrollToPosition(i);
                    found = true;
                    recyclerView.postDelayed(() -> {
                        Toast.makeText(this, "Payment found! Tap to view details.", Toast.LENGTH_SHORT).show();
                    }, 500);
                    break;
                }
            }
            if (!found) {
                Toast.makeText(this, "No payment found for this booking yet", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        layoutStats.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        layoutStats.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
    }

    private void calculateStats() {
        double totalPaid = 0;
        int completedPayments = 0;

        for (Payment payment : payments) {
            if ("completed".equals(payment.getStatus())) {
                totalPaid += payment.getAmount();
                completedPayments++;
            }
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        tvTotalPaid.setText(currencyFormat.format(totalPaid));
        tvTotalPayments.setText(String.valueOf(completedPayments));
    }

    private void showPaymentDetails(Payment payment) {
        // Inflate custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_details, null);
        
        // Initialize views
        TextView tvDormitory = dialogView.findViewById(R.id.tvDialogDormitory);
        TextView tvAmount = dialogView.findViewById(R.id.tvDialogAmount);
        TextView tvPaymentMethod = dialogView.findViewById(R.id.tvDialogPaymentMethod);
        TextView tvReference = dialogView.findViewById(R.id.tvDialogReference);
        TextView tvDate = dialogView.findViewById(R.id.tvDialogDate);
        TextView tvStatus = dialogView.findViewById(R.id.tvDialogStatus);
        ImageView ivStatusIcon = dialogView.findViewById(R.id.ivDialogStatusIcon);
        
        TextView tvDescription = dialogView.findViewById(R.id.tvDialogDescription);
        View layoutDescription = dialogView.findViewById(R.id.layoutDescription);
        
        TextView tvTransactionId = dialogView.findViewById(R.id.tvDialogTransactionId);
        View layoutTransactionId = dialogView.findViewById(R.id.layoutTransactionId);
        
        TextView tvCompletedDate = dialogView.findViewById(R.id.tvDialogCompletedDate);
        View layoutCompletedDate = dialogView.findViewById(R.id.layoutCompletedDate);
        
        TextView tvFailureReason = dialogView.findViewById(R.id.tvDialogFailureReason);
        View layoutFailureReason = dialogView.findViewById(R.id.layoutFailureReason);
        
        View btnViewReceipt = dialogView.findViewById(R.id.btnViewReceipt);
        View btnClose = dialogView.findViewById(R.id.btnClose);
        
        // Format helpers
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        
        // Set basic info
        tvDormitory.setText(payment.getDormitoryName());
        tvAmount.setText(currencyFormat.format(payment.getAmount()));
        tvPaymentMethod.setText(payment.getPaymentMethod().toUpperCase());
        tvReference.setText(payment.getReferenceNumber());
        tvDate.setText(dateFormat.format(new Date(payment.getTimestamp())));
        
        // Set status with color
        String status = payment.getStatus();
        tvStatus.setText(getStatusText(status));
        
        switch (status) {
            case "completed":
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                ivStatusIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "pending":
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                ivStatusIcon.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "failed":
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                ivStatusIcon.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
                break;
            case "refunded":
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                ivStatusIcon.setColorFilter(getResources().getColor(android.R.color.holo_blue_dark));
                break;
        }
        
        // Show/hide optional fields
        String description = payment.getDescription();
        if (description != null && !description.isEmpty() && !description.toLowerCase().contains("null")) {
            tvDescription.setText(description);
            layoutDescription.setVisibility(View.VISIBLE);
        } else {
            layoutDescription.setVisibility(View.GONE);
        }
        
        if (payment.getTransactionId() != null && !payment.getTransactionId().isEmpty()) {
            tvTransactionId.setText(payment.getTransactionId());
            layoutTransactionId.setVisibility(View.VISIBLE);
        } else {
            layoutTransactionId.setVisibility(View.GONE);
        }
        
        if ("completed".equals(payment.getStatus()) && payment.getCompletedDate() > 0) {
            tvCompletedDate.setText(dateFormat.format(new Date(payment.getCompletedDate())));
            layoutCompletedDate.setVisibility(View.VISIBLE);
        } else {
            layoutCompletedDate.setVisibility(View.GONE);
        }
        
        if ("failed".equals(payment.getStatus()) && payment.getFailureReason() != null) {
            tvFailureReason.setText(payment.getFailureReason());
            layoutFailureReason.setVisibility(View.VISIBLE);
        } else {
            layoutFailureReason.setVisibility(View.GONE);
        }
        
        // Create and show dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        // Set up button listeners
        btnViewReceipt.setOnClickListener(v -> {
            if (payment.getPaymentProof() != null && !payment.getPaymentProof().isEmpty()) {
                viewPaymentProof(payment);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "No receipt available", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void viewPaymentProof(Payment payment) {
        // You can create a separate activity to view the image or use a dialog
        Intent intent = new Intent(this, ImageViewActivity.class);
        intent.putExtra("imageUrl", payment.getPaymentProof());
        intent.putExtra("title", "Payment Proof");
        startActivity(intent);
    }

    private String getStatusText(String status) {
        switch (status) {
            case "completed":
                return "Completed";
            case "pending":
                return "Pending Verification";
            case "failed":
                return "Failed";
            case "refunded":
                return "Refunded";
            default:
                return status;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh payments when returning to this activity
        loadPayments();
    }
}
