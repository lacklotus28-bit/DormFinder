package com.rct.dormfinder.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.models.Notification;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private FirebaseFirestore db;
    private Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a notification for a user
     */
    public void createNotification(String userId, String title, String message, String type, String relatedId) {
        Notification notification = new Notification(userId, title, message, type);
        notification.setRelatedId(relatedId);
        notification.setCreatedAt(Timestamp.now());

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification created: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating notification", e);
                });
    }

    /**
     * Create a notification with image
     */
    public void createNotificationWithImage(String userId, String title, String message, String type, String relatedId, String imageUrl) {
        Notification notification = new Notification(userId, title, message, type);
        notification.setRelatedId(relatedId);
        notification.setImageUrl(imageUrl);
        notification.setCreatedAt(Timestamp.now());

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification with image created: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating notification with image", e);
                });
    }

    /**
     * Notify about new booking request
     */
    public void notifyBookingRequest(String landlordId, String studentName, String dormitoryName, String bookingId) {
        String title = "New Booking Request";
        String message = studentName + " requested to book " + dormitoryName;
        createNotification(landlordId, title, message, "booking", bookingId);
    }

    /**
     * Notify about booking confirmation
     */
    public void notifyBookingConfirmed(String studentId, String dormitoryName, String bookingId) {
        String title = "Booking Confirmed";
        String message = "Your booking for " + dormitoryName + " has been confirmed!";
        createNotification(studentId, title, message, "booking", bookingId);
    }

    /**
     * Notify about booking rejection
     */
    public void notifyBookingRejected(String studentId, String dormitoryName, String bookingId) {
        String title = "Booking Update";
        String message = "Your booking request for " + dormitoryName + " was not approved.";
        createNotification(studentId, title, message, "booking", bookingId);
    }

    /**
     * Notify about booking cancellation
     */
    public void notifyBookingCancelled(String userId, String dormitoryName, String bookingId, boolean isLandlord) {
        String title = "Booking Cancelled";
        String message = isLandlord ? 
                "A booking for " + dormitoryName + " has been cancelled by the student." :
                "Your booking for " + dormitoryName + " has been cancelled.";
        createNotification(userId, title, message, "booking", bookingId);
    }

    /**
     * Notify about new message
     */
    public void notifyNewMessage(String userId, String senderName, String chatId) {
        String title = "New Message";
        String message = senderName + " sent you a message";
        createNotification(userId, title, message, "message", chatId);
    }

    /**
     * Notify about payment received
     */
    public void notifyPaymentReceived(String landlordId, String studentName, String amount, String dormitoryName, String paymentId) {
        String title = "Payment Received";
        String message = studentName + " submitted a payment of ₱" + amount + " for " + dormitoryName;
        createNotification(landlordId, title, message, "payment", paymentId);
    }

    /**
     * Notify about payment confirmation
     */
    public void notifyPaymentConfirmed(String studentId, String amount, String dormitoryName, String paymentId) {
        String title = "Payment Confirmed";
        String message = "Your payment of ₱" + amount + " for " + dormitoryName + " has been confirmed.";
        createNotification(studentId, title, message, "payment", paymentId);
    }

    /**
     * Notify about payment rejection
     */
    public void notifyPaymentRejected(String studentId, String amount, String dormitoryName, String paymentId, String reason) {
        String title = "Payment Issue";
        String message = "Your payment of ₱" + amount + " for " + dormitoryName + " needs review. Reason: " + reason;
        createNotification(studentId, title, message, "payment", paymentId);
    }

    /**
     * Notify about new review
     */
    public void notifyNewReview(String landlordId, String studentName, String dormitoryName, int rating, String dormitoryId) {
        String title = "New Review";
        String message = studentName + " left a " + rating + "-star review for " + dormitoryName;
        createNotification(landlordId, title, message, "review", dormitoryId);
    }

    /**
     * Notify about review reply
     */
    public void notifyReviewReply(String studentId, String dormitoryName, String dormitoryId) {
        String title = "Review Reply";
        String message = "The landlord replied to your review on " + dormitoryName;
        createNotification(studentId, title, message, "review", dormitoryId);
    }

    /**
     * Notify about dormitory approval (for admin features)
     */
    public void notifyDormitoryApproved(String landlordId, String dormitoryName, String dormitoryId) {
        String title = "Dormitory Approved";
        String message = dormitoryName + " has been approved and is now visible to students.";
        createNotification(landlordId, title, message, "dormitory", dormitoryId);
    }

    /**
     * Notify about dormitory update
     */
    public void notifyDormitoryUpdated(String landlordId, String dormitoryName, String dormitoryId) {
        String title = "Dormitory Updated";
        String message = "Your dormitory " + dormitoryName + " has been successfully updated.";
        createNotification(landlordId, title, message, "dormitory", dormitoryId);
    }

    /**
     * Get unread notification count for a user
     */
    public void getUnreadCount(String userId, OnCountReceivedListener listener) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)  // Changed to match model field name
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (listener != null) {
                        listener.onCountReceived(count);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting unread count", e);
                    if (listener != null) {
                        listener.onCountReceived(0);
                    }
                });
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(String userId, OnCompleteListener listener) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)  // Changed to match model field name
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count == 0) {
                        if (listener != null) listener.onComplete(true);
                        return;
                    }

                    for (int i = 0; i < count; i++) {
                        String notificationId = queryDocumentSnapshots.getDocuments().get(i).getId();
                        db.collection("notifications")
                                .document(notificationId)
                                .update("isRead", true);  // Changed to match model field name
                    }
                    
                    if (listener != null) listener.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking all as read", e);
                    if (listener != null) listener.onComplete(false);
                });
    }

    /**
     * Delete old notifications (older than 30 days)
     */
    public void deleteOldNotifications(String userId) {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        Timestamp cutoffDate = new Timestamp(thirtyDaysAgo / 1000, 0);

        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereLessThan("createdAt", cutoffDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        queryDocumentSnapshots.getDocuments().get(i).getReference().delete();
                    }
                    Log.d(TAG, "Deleted " + queryDocumentSnapshots.size() + " old notifications");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting old notifications", e);
                });
    }

    // Callback interfaces
    public interface OnCountReceivedListener {
        void onCountReceived(int count);
    }

    public interface OnCompleteListener {
        void onComplete(boolean success);
    }
}
