package com.rct.dormfinder.services;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.rct.dormfinder.models.Payment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service class for handling payment operations
 * Integrates with Paymongo API for GCash and PayMaya payments
 */
public class PaymentService {
    private static final String TAG = "PaymentService";
    private static final String PAYMONGO_BASE_URL = "https://api.paymongo.com/v1";

    // TODO: Replace with your actual Paymongo API keys
    private static final String PAYMONGO_PUBLIC_KEY = "YOUR_PAYMONGO_PUBLIC_KEY_HERE";
    private static final String PAYMONGO_SECRET_KEY = "YOUR_PAYMONGO_SECRET_KEY_HERE";

    private FirebaseFirestore db;
    private Context context;

    public PaymentService(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a payment intent for GCash or PayMaya
     */
    public interface PaymentIntentCallback {
        void onSuccess(String paymentIntentId, String checkoutUrl);

        void onFailure(String error);
    }

    public void createPaymentIntent(Payment payment, PaymentIntentCallback callback) {
        // Generate reference number
        String referenceNumber = generateReferenceNumber();
        payment.setReferenceNumber(referenceNumber);

        // Save payment to Firestore first
        String paymentId = db.collection("payments").document().getId();
        payment.setPaymentId(paymentId);

        db.collection("payments")
                .document(paymentId)
                .set(payment)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Payment created in Firestore: " + paymentId);

                    // In production, you would call Paymongo API here
                    // For now, we'll simulate with a test mode
                    if (payment.getPaymentMethod().equals("cash")) {
                        // Cash payments don't need payment gateway
                        callback.onSuccess(paymentId, null);
                    } else {
                        // Simulate Paymongo integration
                        simulatePaymongoIntent(payment, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating payment: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Simulates Paymongo payment intent creation
     * In production, replace this with actual Paymongo API call
     */
    private void simulatePaymongoIntent(Payment payment, PaymentIntentCallback callback) {
        // This is a simulation for testing purposes
        // In production, you would make an actual API call to Paymongo

        String simulatedIntentId = "pi_" + UUID.randomUUID().toString().substring(0, 8);
        String simulatedCheckoutUrl = "https://payments.paymongo.com/checkout/" + simulatedIntentId;

        // Update payment with transaction ID
        db.collection("payments")
                .document(payment.getPaymentId())
                .update("transactionId", simulatedIntentId)
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess(simulatedIntentId, simulatedCheckoutUrl);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Update payment status
     */
    public interface PaymentStatusCallback {
        void onSuccess();

        void onFailure(String error);
    }

    public void updatePaymentStatus(String paymentId, String status, PaymentStatusCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        if (status.equals("completed")) {
            updates.put("completedDate", System.currentTimeMillis());
        }

        db.collection("payments")
                .document(paymentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Payment status updated: " + paymentId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating payment: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Verify payment status from Paymongo
     */
    public void verifyPayment(String paymentIntentId, PaymentStatusCallback callback) {
        // In production, you would call Paymongo API to verify payment
        // For now, we'll check Firestore

        db.collection("payments")
                .whereEqualTo("transactionId", paymentIntentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Payment payment = querySnapshot.getDocuments().get(0).toObject(Payment.class);
                        if (payment != null && payment.getStatus().equals("completed")) {
                            callback.onSuccess();
                        } else {
                            callback.onFailure("Payment not completed");
                        }
                    } else {
                        callback.onFailure("Payment not found");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get payments for a student
     */
    public interface PaymentListCallback {
        void onSuccess(java.util.List<Payment> payments);

        void onFailure(String error);
    }

    public void getStudentPayments(String studentId, PaymentListCallback callback) {
        db.collection("payments")
                .whereEqualTo("studentId", studentId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<Payment> payments = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            payments.add(payment);
                        }
                    }
                    callback.onSuccess(payments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting payments: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get payments for a landlord
     */
    public void getLandlordPayments(String landlordId, PaymentListCallback callback) {
        db.collection("payments")
                .whereEqualTo("landlordId", landlordId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<Payment> payments = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            payments.add(payment);
                        }
                    }
                    callback.onSuccess(payments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting payments: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Generate a unique reference number
     */
    private String generateReferenceNumber() {
        return "REF-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /**
     * Process cash payment with proof upload
     */
    public void processCashPayment(String paymentId, String proofUrl, PaymentStatusCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentProof", proofUrl);
        updates.put("status", "pending"); // Landlord needs to verify

        db.collection("payments")
                .document(paymentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cash payment proof uploaded: " + paymentId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading proof: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                });
    }
}
