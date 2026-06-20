package com.rct.dormfinder.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Booking;
import com.rct.dormfinder.models.Payment;
import com.rct.dormfinder.services.PaymentService;
import com.rct.dormfinder.utils.ConfirmationDialogHelper;

import java.text.NumberFormat;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "PaymentActivity";
    
    private TextView tvDormitoryName, tvAmount, tvDescription, tvReferenceNumber;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbGcash, rbPaymaya, rbCash;
    private Button btnProceedPayment, btnUploadProof;
    private ImageView ivPaymentProof;
    private ProgressBar progressBar;
    private View layoutCashPayment;
    
    private String bookingId;
    private Booking booking;
    private Payment payment;
    private PaymentService paymentService;
    private Uri selectedProofImage;
    
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        paymentService = new PaymentService(this);

        // Initialize views
        initializeViews();

        // Get booking data
        bookingId = getIntent().getStringExtra("bookingId");
        if (bookingId == null) {
            Toast.makeText(this, "Booking ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup image picker
        setupImagePicker();

        // Load booking details
        loadBookingDetails();

        // Setup listeners
        setupListeners();
    }

    private void initializeViews() {
        tvDormitoryName = findViewById(R.id.tvDormitoryName);
        tvAmount = findViewById(R.id.tvAmount);
        tvDescription = findViewById(R.id.tvDescription);
        tvReferenceNumber = findViewById(R.id.tvReferenceNumber);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        rbGcash = findViewById(R.id.rbGcash);
        rbPaymaya = findViewById(R.id.rbPaymaya);
        rbCash = findViewById(R.id.rbCash);
        btnProceedPayment = findViewById(R.id.btnProceedPayment);
        btnUploadProof = findViewById(R.id.btnUploadProof);
        ivPaymentProof = findViewById(R.id.ivPaymentProof);
        progressBar = findViewById(R.id.progressBar);
        layoutCashPayment = findViewById(R.id.layoutCashPayment);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedProofImage = result.getData().getData();
                    ivPaymentProof.setImageURI(selectedProofImage);
                    ivPaymentProof.setVisibility(View.VISIBLE);
                }
            }
        );
    }

    private void loadBookingDetails() {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("bookings")
            .document(bookingId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                booking = documentSnapshot.toObject(Booking.class);
                if (booking != null) {
                    displayBookingInfo();
                    createPaymentRecord();
                } else {
                    Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
                progressBar.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading booking: " + e.getMessage());
                Toast.makeText(this, "Error loading booking details", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                finish();
            });
    }

    private void displayBookingInfo() {
        tvDormitoryName.setText(booking.getDormitoryName());
        
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        tvAmount.setText(currencyFormat.format(booking.getMonthlyPrice()));
        
        String description = "Monthly rent for " + booking.getRoomType() + " room";
        tvDescription.setText(description);
    }

    private void createPaymentRecord() {
        payment = new Payment(
            bookingId,
            booking.getStudentId(),
            booking.getLandlordId(),
            booking.getDormitoryId(),
            booking.getMonthlyPrice()
        );
        payment.setDormitoryName(booking.getDormitoryName());
        payment.setStudentName(booking.getStudentName());
        payment.setStudentEmail(booking.getStudentEmail());
        payment.setDescription("Monthly rent - " + booking.getRoomType());
    }

    private void setupListeners() {
        // Setup click listeners for payment option layouts
        setupPaymentOptionClickListeners();
        
        // RadioGroup listener is not needed since we're manually managing radio buttons
        // due to them being wrapped in LinearLayouts

        btnUploadProof.setOnClickListener(v -> selectImage());

        btnProceedPayment.setOnClickListener(v -> processPayment());

        findViewById(R.id.btnBack).setOnClickListener(v -> handleBackPress());
    }

    private void setupPaymentOptionClickListeners() {
        // Get references to the payment option layouts
        View gcashLayout = findViewById(R.id.layoutGcashOption);
        View paymayaLayout = findViewById(R.id.layoutPaymayaOption);
        View cashLayout = findViewById(R.id.layoutCashOption);
        
        Log.d(TAG, "Setting up payment option click listeners");
        Log.d(TAG, "GCash layout found: " + (gcashLayout != null));
        Log.d(TAG, "PayMaya layout found: " + (paymayaLayout != null));
        Log.d(TAG, "Cash layout found: " + (cashLayout != null));
        
        // Set click listeners to select the radio button when the entire layout is clicked
        if (gcashLayout != null) {
            gcashLayout.setOnClickListener(v -> {
                Log.d(TAG, "GCash layout clicked");
                selectPaymentMethod("gcash");
            });
        } else {
            Log.w(TAG, "GCash layout not found! Check XML IDs");
        }
        
        if (paymayaLayout != null) {
            paymayaLayout.setOnClickListener(v -> {
                Log.d(TAG, "PayMaya layout clicked");
                selectPaymentMethod("paymaya");
            });
        } else {
            Log.w(TAG, "PayMaya layout not found! Check XML IDs");
        }
        
        if (cashLayout != null) {
            cashLayout.setOnClickListener(v -> {
                Log.d(TAG, "Cash layout clicked");
                selectPaymentMethod("cash");
            });
        } else {
            Log.w(TAG, "Cash layout not found! Check XML IDs");
        }
        
        // Also add direct listeners to radio buttons as fallback
        rbGcash.setOnClickListener(v -> {
            Log.d(TAG, "GCash radio button clicked directly");
            selectPaymentMethod("gcash");
        });
        
        rbPaymaya.setOnClickListener(v -> {
            Log.d(TAG, "PayMaya radio button clicked directly");
            selectPaymentMethod("paymaya");
        });
        
        rbCash.setOnClickListener(v -> {
            Log.d(TAG, "Cash radio button clicked directly");
            selectPaymentMethod("cash");
        });
    }

    private void selectPaymentMethod(String method) {
        // Uncheck all radio buttons first
        rbGcash.setChecked(false);
        rbPaymaya.setChecked(false);
        rbCash.setChecked(false);
        
        // Check the selected one and update UI
        switch (method) {
            case "gcash":
                rbGcash.setChecked(true);
                layoutCashPayment.setVisibility(View.GONE);
                btnProceedPayment.setText("Proceed to Payment");
                break;
            case "paymaya":
                rbPaymaya.setChecked(true);
                layoutCashPayment.setVisibility(View.GONE);
                btnProceedPayment.setText("Proceed to Payment");
                break;
            case "cash":
                rbCash.setChecked(true);
                layoutCashPayment.setVisibility(View.VISIBLE);
                btnProceedPayment.setText("Submit Payment Proof");
                break;
        }
        
        Log.d(TAG, "Payment method selected: " + method);
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void processPayment() {
        // Check which radio button is actually checked (not using RadioGroup)
        String paymentMethod = null;
        
        if (rbGcash.isChecked()) {
            paymentMethod = "gcash";
        } else if (rbPaymaya.isChecked()) {
            paymentMethod = "paymaya";
        } else if (rbCash.isChecked()) {
            paymentMethod = "cash";
        }
        
        // Debug logging
        Log.d(TAG, "Processing payment. PaymentMethod: " + paymentMethod);
        Log.d(TAG, "GCash Checked: " + rbGcash.isChecked());
        Log.d(TAG, "PayMaya Checked: " + rbPaymaya.isChecked());
        Log.d(TAG, "Cash Checked: " + rbCash.isChecked());
        
        if (paymentMethod == null) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        payment.setPaymentMethod(paymentMethod);

        if (paymentMethod.equals("cash")) {
            processCashPayment();
        } else {
            processOnlinePayment();
        }
    }

    private void processCashPayment() {
        if (selectedProofImage == null) {
            Toast.makeText(this, "Please upload payment proof", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnProceedPayment.setEnabled(false);

        // Upload proof image first
        uploadProofImage(proofUrl -> {
            payment.setPaymentProof(proofUrl);
            
            // Create payment record
            paymentService.createPaymentIntent(payment, new PaymentService.PaymentIntentCallback() {
                @Override
                public void onSuccess(String paymentIntentId, String checkoutUrl) {
                    // Update booking with payment info
                    updateBookingWithPaymentInfo();
                }

                @Override
                public void onFailure(String error) {
                    progressBar.setVisibility(View.GONE);
                    btnProceedPayment.setEnabled(true);
                    Toast.makeText(PaymentActivity.this, 
                        "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void updateBookingWithPaymentInfo() {
        db.collection("bookings")
            .document(bookingId)
            .update(
                "status", "paid",  // Update booking status to "paid" for cash payment
                "paymentStatus", "pending",  // Cash payments are pending until landlord verifies
                "paymentId", payment.getPaymentId(),
                "paymentDate", System.currentTimeMillis()
            )
            .addOnSuccessListener(aVoid -> {
                // Send notification to landlord about payment
                sendPaymentNotificationToLandlord();
                
                progressBar.setVisibility(View.GONE);
                showSuccessDialog(
                    "Payment Proof Submitted",
                    "Your payment proof has been submitted. The landlord will verify your payment.",
                    payment.getReferenceNumber()
                );
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Payment submitted but error updating booking",
                    Toast.LENGTH_SHORT).show();
                // Still finish with success since payment was created
                setResult(RESULT_OK);
                finish();
            });
    }

    private void uploadProofImage(OnImageUploadedListener listener) {
        String fileName = "payment_proofs/" + auth.getCurrentUser().getUid() + 
                         "/" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(selectedProofImage)
            .addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    listener.onUploaded(uri.toString());
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error uploading proof: " + e.getMessage());
                Toast.makeText(this, "Error uploading proof", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                btnProceedPayment.setEnabled(true);
            });
    }

    private interface OnImageUploadedListener {
        void onUploaded(String url);
    }

    private void processOnlinePayment() {
        progressBar.setVisibility(View.VISIBLE);
        btnProceedPayment.setEnabled(false);

        paymentService.createPaymentIntent(payment, new PaymentService.PaymentIntentCallback() {
            @Override
            public void onSuccess(String paymentIntentId, String checkoutUrl) {
                progressBar.setVisibility(View.GONE);
                
                // Show payment instructions
                showOnlinePaymentDialog(paymentIntentId, checkoutUrl);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                btnProceedPayment.setEnabled(true);
                Toast.makeText(PaymentActivity.this, 
                    "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOnlinePaymentDialog(String paymentIntentId, String checkoutUrl) {
        new AlertDialog.Builder(this)
            .setTitle("Payment Instructions")
            .setMessage("Test Mode:\n\n" +
                       "1. In production, you would be redirected to " + payment.getPaymentMethod().toUpperCase() + "\n" +
                       "2. Complete payment in the payment gateway\n" +
                       "3. You will be redirected back to the app\n\n" +
                       "Reference: " + payment.getReferenceNumber() + "\n\n" +
                       "For testing, click 'Simulate Success' to mark payment as completed.")
            .setPositiveButton("Simulate Success", (dialog, which) -> {
                simulatePaymentSuccess(paymentIntentId);
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                btnProceedPayment.setEnabled(true);
            })
            .setCancelable(false)
            .show();
    }

    private void simulatePaymentSuccess(String paymentIntentId) {
        progressBar.setVisibility(View.VISIBLE);
        
        paymentService.updatePaymentStatus(payment.getPaymentId(), "completed", 
            new PaymentService.PaymentStatusCallback() {
                @Override
                public void onSuccess() {
                    // Update booking status to paid
                    updateBookingPaymentStatus();
                }

                @Override
                public void onFailure(String error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PaymentActivity.this, 
                        "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateBookingPaymentStatus() {
        db.collection("bookings")
            .document(bookingId)
            .update(
                "status", "paid",  // Update booking status to "paid"
                "paymentStatus", "paid",
                "paymentId", payment.getPaymentId(),  // Link paymentId to booking!
                "paymentDate", System.currentTimeMillis()
            )
            .addOnSuccessListener(aVoid -> {
                // Send notification to landlord about payment
                sendPaymentNotificationToLandlord();
                
                progressBar.setVisibility(View.GONE);
                showSuccessDialog(
                    "Payment Successful",
                    "Your payment has been processed successfully!",
                    payment.getReferenceNumber()
                );
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Payment completed but error updating booking", 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void showSuccessDialog(String title, String message, String reference) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message + "\n\nReference Number: " + reference)
            .setPositiveButton("OK", (dialog, which) -> {
                setResult(RESULT_OK);
                finish();
            })
            .setCancelable(false)
            .show();
    }

    /**
     * Send notification to landlord when student makes payment
     */
    private void sendPaymentNotificationToLandlord() {
        if (booking == null) {
            Log.e(TAG, "Cannot send notification: booking is null");
            return;
        }

        java.util.Map<String, Object> notification = new java.util.HashMap<>();
        notification.put("userId", booking.getLandlordId());
        notification.put("senderId", auth.getCurrentUser().getUid());
        notification.put("type", "payment_received");
        notification.put("title", "Payment Received");
        notification.put("message", booking.getStudentName() + " has submitted payment for " + 
                        booking.getDormitoryName());
        notification.put("bookingId", bookingId);
        notification.put("dormitoryId", booking.getDormitoryId());
        notification.put("paymentId", payment.getPaymentId());
        notification.put("relatedId", bookingId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);
        notification.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener(docRef -> {
                Log.d(TAG, "Payment notification sent to landlord");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to send payment notification: " + e.getMessage());
            });
    }

    /**
     * Handle back press with payment cancellation confirmation
     */
    private void handleBackPress() {
        // Check if user has selected payment method or uploaded proof
        boolean hasStartedPayment = rbGcash.isChecked() || rbPaymaya.isChecked() || 
                                   rbCash.isChecked() || selectedProofImage != null;

        if (hasStartedPayment) {
            ConfirmationDialogHelper.showCancelPaymentDialog(this,
                    new ConfirmationDialogHelper.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            finish();
                        }

                        @Override
                        public void onCancel() {
                            // Stay on payment page
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
