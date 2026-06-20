package com.rct.dormfinder.utils;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.models.Notification;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized notification manager for creating and sending notifications
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";
    private final FirebaseFirestore db;

    public NotificationManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ==================== BOOKING NOTIFICATIONS ====================

    /**
     * Notify student when their booking is approved
     */
    public void sendBookingApprovedNotification(String userId, String bookingId, String dormName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Booking Approved! 🎉");
        notification.put("message", "Your booking for " + dormName + " has been approved!");
        notification.put("type", "booking");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "BookingDetailActivity:" + bookingId);

        createNotification(notification);
    }

    /**
     * Notify student when their booking is rejected
     */
    public void sendBookingRejectedNotification(String userId, String bookingId, String dormName, String reason) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Booking Rejected");
        notification.put("message", "Your booking for " + dormName + " was rejected. Reason: " + reason);
        notification.put("type", "booking");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "BookingDetailActivity:" + bookingId);

        createNotification(notification);
    }

    /**
     * Notify landlord when they receive a new booking
     */
    public void sendNewBookingNotification(String landlordId, String bookingId, String studentName, String dormName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", landlordId);
        notification.put("title", "New Booking Request 📬");
        notification.put("message", studentName + " wants to book " + dormName);
        notification.put("type", "booking");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "LandlordBookingDetailActivity:" + bookingId);

        createNotification(notification);
    }

    /**
     * Notify when booking is cancelled
     */
    public void sendBookingCancelledNotification(String userId, String bookingId, String dormName, String cancelledBy) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Booking Cancelled");
        notification.put("message", "Booking for " + dormName + " was cancelled by " + cancelledBy);
        notification.put("type", "booking");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "BookingDetailActivity:" + bookingId);

        createNotification(notification);
    }

    /**
     * Send check-in reminder
     */
    public void sendCheckInReminderNotification(String userId, String bookingId, String dormName, String checkInDate) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Check-in Tomorrow! 🗓️");
        notification.put("message", "Your check-in at " + dormName + " is on " + checkInDate);
        notification.put("type", "booking");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "BookingDetailActivity:" + bookingId);

        createNotification(notification);
    }

    /**
     * Send check-out reminder
     */
    public void sendCheckOutReminderNotification(String userId, String bookingId, String dormName, String checkOutDate) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Check-out Soon 📦");
        notification.put("message", "Your check-out from " + dormName + " is on " + checkOutDate);
        notification.put("type", "booking");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "BookingDetailActivity:" + bookingId);

        createNotification(notification);
    }

    // ==================== PAYMENT NOTIFICATIONS ====================

    /**
     * Notify when payment is due soon
     */
    public void sendPaymentDueNotification(String userId, String bookingId, String dormName, String dueDate, double amount) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Payment Due Soon 💳");
        notification.put("message", "Payment of ₱" + String.format("%.2f", amount) + " for " + dormName + " is due on " + dueDate);
        notification.put("type", "payment");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "PaymentActivity:" + bookingId);

        createNotification(notification);
    }

    /**
     * Notify when payment is overdue
     */
    public void sendPaymentOverdueNotification(String userId, String bookingId, String dormName, double amount) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Payment Overdue ⚠️");
        notification.put("message", "Your payment of ₱" + String.format("%.2f", amount) + " for " + dormName + " is overdue!");
        notification.put("type", "payment");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "PaymentActivity:" + bookingId);

        createNotification(notification);
    }

    /**
     * Notify when payment is received (for landlords)
     */
    public void sendPaymentReceivedNotification(String landlordId, String bookingId, String studentName, String dormName, double amount) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", landlordId);
        notification.put("title", "Payment Received ✅");
        notification.put("message", studentName + " paid ₱" + String.format("%.2f", amount) + " for " + dormName);
        notification.put("type", "payment");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "LandlordBookingDetailActivity:" + bookingId);

        createNotification(notification);
    }

    /**
     * Notify when payment is confirmed (for students)
     */
    public void sendPaymentConfirmedNotification(String userId, String bookingId, String dormName, double amount) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Payment Confirmed ✅");
        notification.put("message", "Your payment of ₱" + String.format("%.2f", amount) + " for " + dormName + " has been confirmed");
        notification.put("type", "payment");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "BookingDetailActivity:" + bookingId);

        createNotification(notification);
    }

    /**
     * Notify when refund is processed
     */
    public void sendRefundProcessedNotification(String userId, String bookingId, String dormName, double amount) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Refund Processed 💰");
        notification.put("message", "Your refund of ₱" + String.format("%.2f", amount) + " for " + dormName + " has been processed");
        notification.put("type", "payment");
        notification.put("relatedId", bookingId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());

        createNotification(notification);
    }

    // ==================== MESSAGE NOTIFICATIONS ====================

    /**
     * Notify when user receives a new message
     */
    public void sendNewMessageNotification(String userId, String chatId, String senderName, String messagePreview) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "New Message from " + senderName + " 💬");
        notification.put("message", messagePreview);
        notification.put("type", "message");
        notification.put("relatedId", chatId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "ChatActivity:" + chatId);

        createNotification(notification);
    }

    // ==================== REVIEW NOTIFICATIONS ====================

    /**
     * Notify landlord when they receive a new review
     */
    public void sendNewReviewNotification(String landlordId, String reviewId, String dormName, String reviewerName, double rating) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", landlordId);
        notification.put("title", "New Review ⭐");
        notification.put("message", reviewerName + " gave " + dormName + " a " + rating + " star review");
        notification.put("type", "review");
        notification.put("relatedId", reviewId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "ReviewDetailActivity:" + reviewId);

        createNotification(notification);
    }

    /**
     * Notify student when landlord replies to their review
     */
    public void sendReviewReplyNotification(String userId, String reviewId, String dormName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Landlord Replied to Your Review 💬");
        notification.put("message", "The landlord of " + dormName + " replied to your review");
        notification.put("type", "review");
        notification.put("relatedId", reviewId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "ReviewDetailActivity:" + reviewId);

        createNotification(notification);
    }

    /**
     * Notify when review is verified
     */
    public void sendReviewVerifiedNotification(String userId, String reviewId, String dormName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Review Verified ✅");
        notification.put("message", "Your review for " + dormName + " has been verified!");
        notification.put("type", "review");
        notification.put("relatedId", reviewId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "ReviewDetailActivity:" + reviewId);

        createNotification(notification);
    }

    // ==================== DORMITORY NOTIFICATIONS ====================

    /**
     * Notify when favorited dorm becomes available
     */
    public void sendDormAvailableNotification(String userId, String dormId, String dormName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Dorm Now Available! 🏠");
        notification.put("message", dormName + " has rooms available now!");
        notification.put("type", "dormitory");
        notification.put("relatedId", dormId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "DormDetailActivity:" + dormId);

        createNotification(notification);
    }

    /**
     * Notify when price of favorited dorm changes
     */
    public void sendPriceChangeNotification(String userId, String dormId, String dormName, double oldPrice, double newPrice) {
        String priceChange = newPrice < oldPrice ? "decreased" : "increased";
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Price Update 💰");
        notification.put("message", dormName + " price " + priceChange + " from ₱" + String.format("%.2f", oldPrice) + " to ₱" + String.format("%.2f", newPrice));
        notification.put("type", "dormitory");
        notification.put("relatedId", dormId);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "DormDetailActivity:" + dormId);

        createNotification(notification);
    }

    // ==================== SYSTEM NOTIFICATIONS ====================

    /**
     * Notify when account is verified
     */
    public void sendAccountVerifiedNotification(String userId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Account Verified! ✅");
        notification.put("message", "Your account has been successfully verified. You can now access all features.");
        notification.put("type", "system");
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());

        createNotification(notification);
    }

    /**
     * Notify about security alerts
     */
    public void sendSecurityAlertNotification(String userId, String deviceInfo, String location) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "New Login Detected 🔐");
        notification.put("message", "Your account was accessed from " + deviceInfo + " in " + location);
        notification.put("type", "system");
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "SecuritySettingsActivity");

        createNotification(notification);
    }

    /**
     * Send welcome notification to new users
     */
    public void sendWelcomeNotification(String userId, String userName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Welcome to DormFinder! 🎉");
        notification.put("message", "Hi " + userName + "! Start exploring dormitories near you and find your perfect home.");
        notification.put("type", "general");
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("actionUrl", "StudentHomeActivity");

        createNotification(notification);
    }

    // ==================== PROMOTIONAL NOTIFICATIONS ====================

    /**
     * Send personalized dorm recommendations
     */
    public void sendRecommendationNotification(String userId, String dormName, String reason) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "You Might Like This! 🎯");
        notification.put("message", "Check out " + dormName + " - " + reason);
        notification.put("type", "general");
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());

        createNotification(notification);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create notification in Firestore
     */
    private void createNotification(Map<String, Object> notification) {
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification created successfully: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create notification", e);
                });
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(String notificationId) {
        db.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification marked as read: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark notification as read", e);
                });
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(String userId) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update("isRead", true);
                    }
                    Log.d(TAG, "All notifications marked as read for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark all notifications as read", e);
                });
    }

    /**
     * Delete a notification
     */
    public void deleteNotification(String notificationId) {
        db.collection("notifications")
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification deleted: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete notification", e);
                });
    }

    /**
     * Delete all read notifications for a user
     */
    public void deleteReadNotifications(String userId) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                    Log.d(TAG, "All read notifications deleted for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete read notifications", e);
                });
    }
}
