package com.rct.dormfinder.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.utils.NavigationHelper;
import com.rct.dormfinder.models.Booking;
import com.rct.dormfinder.models.User;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.utils.ConfirmationDialogHelper;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingRequestActivity extends AppCompatActivity {
    private ImageView ivBack;
    private TextView tvDormName, tvDormAddress, tvMonthlyPrice, tvMoveInDate;
    private EditText etMessage, etStudentPhone;
    private Button btnSelectMoveInDate, btnSubmitRequest;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private String dormitoryId;
    private String landlordId;
    private String currentUserId;
    private Dormitory dormitory;
    private User currentUser;
    private Calendar selectedMoveInDate;
    private SimpleDateFormat dateFormat;
    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_request);

        initializeViews();
        setupFirebase();
        setupDateFormat();
        loadIntentData();
        setupListeners();
        loadUserData();
        loadDormitoryData();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        tvDormName = findViewById(R.id.tvDormName);
        tvDormAddress = findViewById(R.id.tvDormAddress);
        tvMonthlyPrice = findViewById(R.id.tvMonthlyPrice);
        tvMoveInDate = findViewById(R.id.tvMoveInDate);
        etMessage = findViewById(R.id.etMessage);
        etStudentPhone = findViewById(R.id.etStudentPhone);
        btnSelectMoveInDate = findViewById(R.id.btnSelectMoveInDate);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
    }

    private void setupDateFormat() {
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        selectedMoveInDate = Calendar.getInstance();
        selectedMoveInDate.add(Calendar.DAY_OF_MONTH, 7); // Default to next week
        updateMoveInDateDisplay();
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        dormitoryId = intent.getStringExtra("dormitory_id");
        landlordId = intent.getStringExtra("landlord_id");

        if (dormitoryId == null || landlordId == null) {
            Toast.makeText(this, "Error: Missing booking information", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> handleBackPress());

        btnSelectMoveInDate.setOnClickListener(v -> showDatePicker());

        btnSubmitRequest.setOnClickListener(v -> {
            if (!isSubmitting) {
                submitBookingRequest();
            }
        });
    }

    private void loadUserData() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                            // Pre-fill phone number if available
                            if (currentUser.getContactNumber() != null && !currentUser.getContactNumber().isEmpty()) {
                                etStudentPhone.setText(currentUser.getContactNumber());
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("BookingRequest", "Failed to load user data: " + e.getMessage());
                });
    }

    private void loadDormitoryData() {
        if (dormitoryId == null) return;

        db.collection("dormitories").document(dormitoryId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        dormitory = document.toObject(Dormitory.class);
                        if (dormitory != null) {
                            populateDormitoryInfo();
                        }
                    } else {
                        Toast.makeText(this, "Dormitory not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load dormitory data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateDormitoryInfo() {
        tvDormName.setText(dormitory.getName());
        tvDormAddress.setText(dormitory.getAddress() + ", " + dormitory.getCity());
        
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        tvMonthlyPrice.setText(currencyFormat.format(dormitory.getMonthlyPrice()) + "/month");
    }

    /**
     * Show date picker with DARK THEME for better visibility
     */
    private void showDatePicker() {
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.DAY_OF_MONTH, 1); // Minimum tomorrow

        // ✅ Apply dark theme with R.style.DarkDatePickerTheme
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.DarkDatePickerTheme,  // <- DARK THEME APPLIED HERE
                (view, year, month, dayOfMonth) -> {
                    selectedMoveInDate.set(Calendar.YEAR, year);
                    selectedMoveInDate.set(Calendar.MONTH, month);
                    selectedMoveInDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateMoveInDateDisplay();
                },
                selectedMoveInDate.get(Calendar.YEAR),
                selectedMoveInDate.get(Calendar.MONTH),
                selectedMoveInDate.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        datePickerDialog.show();
    }

    private void updateMoveInDateDisplay() {
        tvMoveInDate.setText(dateFormat.format(selectedMoveInDate.getTime()));
    }

    private void submitBookingRequest() {
        if (!validateInput()) {
            return;
        }

        isSubmitting = true;
        btnSubmitRequest.setEnabled(false);
        btnSubmitRequest.setText("Checking existing bookings...");

        // STEP 1: Check if user has ANY paid or confirmed booking across ALL dormitories
        // This prevents students from having multiple active commitments
        db.collection("bookings")
                .whereEqualTo("studentId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasPaidOrConfirmedBooking = false;
                    String globalBlockingStatus = "";
                    String blockedDormName = "";
                    
                    // Check for paid/confirmed bookings across ALL dorms
                    for (int i = 0; i < querySnapshot.size(); i++) {
                        String status = querySnapshot.getDocuments().get(i).getString("status");
                        String checkDormId = querySnapshot.getDocuments().get(i).getString("dormitoryId");
                        
                        // CRITICAL: Block if user has paid/confirmed booking in ANY dorm
                        if ("paid".equals(status) || "confirmed".equals(status)) {
                            hasPaidOrConfirmedBooking = true;
                            globalBlockingStatus = status;
                            blockedDormName = querySnapshot.getDocuments().get(i).getString("dormitoryName");
                            break;
                        }
                    }
                    
                    if (hasPaidOrConfirmedBooking) {
                        showGlobalBookingBlockDialog(globalBlockingStatus, blockedDormName);
                        resetSubmitButton();
                        return;
                    }
                    
                    // STEP 2: No paid/confirmed bookings found globally, now check for this specific dormitory
                    checkSpecificDormitoryBooking();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking existing bookings: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
    }
    
    /**
     * Check if user already has pending/approved booking for this specific dormitory
     */
    private void checkSpecificDormitoryBooking() {
        db.collection("bookings")
                .whereEqualTo("studentId", currentUserId)
                .whereEqualTo("dormitoryId", dormitoryId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasActiveBooking = false;
                    String existingStatus = "";
                    
                    for (int i = 0; i < querySnapshot.size(); i++) {
                        String status = querySnapshot.getDocuments().get(i).getString("status");
                        
                        // Check for active statuses (pending/approved only at this point)
                        if ("pending".equals(status) || "approved".equals(status)) {
                            hasActiveBooking = true;
                            existingStatus = status;
                            break;
                        }
                    }
                    
                    if (hasActiveBooking) {
                        String message = getExistingBookingMessage(existingStatus);
                        showBookingExistsDialog(existingStatus, message);
                        resetSubmitButton();
                        return;
                    }

                    // No active booking found, proceed with creating new request
                    btnSubmitRequest.setText("Submitting Request...");
                    createBookingRequest();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking dormitory bookings: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
    }
    
    /**
     * Show dialog when user has paid/confirmed booking in another dormitory
     */
    private void showGlobalBookingBlockDialog(String status, String dormitoryName) {
        androidx.appcompat.app.AlertDialog.Builder builder = 
                new androidx.appcompat.app.AlertDialog.Builder(this);
        
        builder.setTitle("Active Booking Exists");
        builder.setIcon(R.drawable.ic_info);
        
        String message;
        if ("confirmed".equals(status)) {
            message = "You already have a confirmed booking at " + dormitoryName + ".\n\n" +
                     "You can only have one active booking at a time. If you wish to book a different dormitory, " +
                     "please cancel your current booking first.";
        } else if ("paid".equals(status)) {
            message = "You have a paid booking at " + dormitoryName + " awaiting confirmation.\n\n" +
                     "Please wait for the landlord to confirm your payment before booking another dormitory. " +
                     "If you wish to proceed with a different dorm, please cancel your current booking first.";
        } else {
            message = "You already have an active booking at " + dormitoryName + ".\n\n" +
                     "You can only have one active booking at a time.";
        }
        
        builder.setMessage(message);
        
        builder.setPositiveButton("View My Bookings", (dialog, which) -> {
            Intent intent = new Intent(this, StudentBookingsActivity.class);
            startActivity(intent);
            finish();
        });
        
        builder.setNegativeButton("Close", (dialog, which) -> {
            finish();
        });
        
        builder.setCancelable(false);
        builder.show();
    }
    
    /**
     * Get user-friendly message based on existing booking status
     */
    private String getExistingBookingMessage(String status) {
        switch (status) {
            case "pending":
                return "You already have a pending booking request for this dormitory. " +
                       "Please wait for the landlord to review your request.";
            case "approved":
                return "Your booking request has been approved! " +
                       "Please proceed with payment to confirm your booking.";
            case "paid":
                return "Your payment is being processed. " +
                       "The landlord will confirm your payment soon.";
            case "confirmed":
                return "You already have a confirmed booking for this dormitory!";
            default:
                return "You already have an active booking for this dormitory.";
        }
    }
    
    /**
     * Show dialog informing user about existing booking
     */
    private void showBookingExistsDialog(String status, String message) {
        androidx.appcompat.app.AlertDialog.Builder builder = 
                new androidx.appcompat.app.AlertDialog.Builder(this);
        
        builder.setTitle("Existing Booking Found");
        builder.setMessage(message);
        builder.setIcon(R.drawable.ic_info);
        
        // Add appropriate action buttons based on status
        if ("approved".equals(status)) {
            builder.setPositiveButton("Make Payment", (dialog, which) -> {
                // Navigate to payment screen or bookings list
                Intent intent = new Intent(this, StudentBookingsActivity.class);
                intent.putExtra("highlightApprovedBookings", true);
                startActivity(intent);
                finish();
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                finish();
            });
        } else {
            builder.setPositiveButton("View My Bookings", (dialog, which) -> {
                Intent intent = new Intent(this, StudentBookingsActivity.class);
                startActivity(intent);
                finish();
            });
            builder.setNegativeButton("Close", (dialog, which) -> {
                finish();
            });
        }
        
        builder.setCancelable(false);
        builder.show();
    }

    private boolean validateInput() {
        String message = etMessage.getText().toString().trim();
        String phone = etStudentPhone.getText().toString().trim();

        if (phone.isEmpty()) {
            etStudentPhone.setError("Phone number is required");
            etStudentPhone.requestFocus();
            return false;
        }

        if (phone.length() < 10) {
            etStudentPhone.setError("Please enter a valid phone number");
            etStudentPhone.requestFocus();
            return false;
        }

        if (message.isEmpty()) {
            etMessage.setError("Please add a message for the landlord");
            etMessage.requestFocus();
            return false;
        }

        if (message.length() < 10) {
            etMessage.setError("Please provide more details about yourself and your needs");
            etMessage.requestFocus();
            return false;
        }

        return true;
    }

    private void createBookingRequest() {
        Booking booking = new Booking(currentUserId, landlordId, dormitoryId, dormitory.getName());
        
        // Set student information
        booking.setStudentName(currentUser.getName());
        booking.setStudentEmail(currentUser.getEmail());
        booking.setStudentPhone(etStudentPhone.getText().toString().trim());
        booking.setStudentSchool(currentUser.getSchool());
        booking.setStudentCourse(currentUser.getCourse());
        
        // Set booking details
        booking.setMessage(etMessage.getText().toString().trim());
        booking.setMonthlyPrice(dormitory.getMonthlyPrice());
        booking.setMoveInDate(selectedMoveInDate.getTimeInMillis());

        db.collection("bookings")
                .add(booking)
                .addOnSuccessListener(documentReference -> {
                    String bookingId = documentReference.getId();
                    
                    // Update the booking with its ID
                    documentReference.update("bookingId", bookingId)
                            .addOnSuccessListener(aVoid -> {
                                // Send notification to landlord
                                sendNotificationToLandlord(bookingId);
                                
                                Toast.makeText(this, "Booking request submitted successfully!", 
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit booking request: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    resetSubmitButton();
                });
    }

    private void sendNotificationToLandlord(String bookingId) {
        // Create notification for landlord
        Map<String, Object> notification = new HashMap<>();
        // FIXED: Changed from "recipientId" to "userId" to match Notification model
        notification.put("userId", landlordId);
        notification.put("senderId", currentUserId);
        notification.put("type", "booking_request");
        notification.put("title", "New Booking Request");
        notification.put("message", currentUser.getName() + " sent a booking request for " + dormitory.getName());
        notification.put("bookingId", bookingId);
        notification.put("dormitoryId", dormitoryId);
        notification.put("relatedId", bookingId);  // For navigation
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);
        notification.put("createdAt", com.google.firebase.Timestamp.now());  // For ordering in NotificationActivity

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> {
                    android.util.Log.d("BookingRequest", "Notification sent to landlord");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("BookingRequest", "Failed to send notification: " + e.getMessage());
                });
    }

    private void resetSubmitButton() {
        isSubmitting = false;
        btnSubmitRequest.setEnabled(true);
        btnSubmitRequest.setText("Submit Booking Request");
    }

    /**
     * Handle back press with confirmation
     */
    private void handleBackPress() {
        // Check if user has entered any information
        boolean hasData = !etMessage.getText().toString().trim().isEmpty() ||
                         !etStudentPhone.getText().toString().trim().isEmpty();

        if (hasData) {
            ConfirmationDialogHelper.showLeaveFormDialog(this, "Booking Request",
                    new ConfirmationDialogHelper.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            finish();
                        }

                        @Override
                        public void onCancel() {
                            // Stay on page
                        }
                    });
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }
}
