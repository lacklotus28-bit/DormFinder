package com.rct.dormfinder.utils;

/**
 * NOTIFICATION IMPLEMENTATION GUIDE
 * 
 * This guide shows where and how to trigger notifications throughout the app.
 */

public class NotificationImplementationGuide {

    /*
     * ========================================
     * 1. BOOKING FLOW NOTIFICATIONS
     * ========================================
     */

    // In StudentBookingActivity.java - When student creates a booking:
    void onBookingCreated_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get booking details
        String bookingId = "booking123";
        String landlordId = "landlord_user_id";
        String studentName = "John Doe";
        String dormName = "Sample Dorm";
        
        // Notify landlord about new booking
        notificationManager.sendNewBookingNotification(
            landlordId, 
            bookingId, 
            studentName, 
            dormName
        );
    }

    // In LandlordBookingDetailActivity.java - When landlord approves booking:
    void onBookingApproved_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get booking details
        String studentId = "student_user_id";
        String bookingId = "booking123";
        String dormName = "Sample Dorm";
        
        // Notify student about approval
        notificationManager.sendBookingApprovedNotification(
            studentId, 
            bookingId, 
            dormName
        );
    }

    // In LandlordBookingDetailActivity.java - When landlord rejects booking:
    void onBookingRejected_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get booking details
        String studentId = "student_user_id";
        String bookingId = "booking123";
        String dormName = "Sample Dorm";
        String reason = "Rooms are fully booked";
        
        // Notify student about rejection
        notificationManager.sendBookingRejectedNotification(
            studentId, 
            bookingId, 
            dormName, 
            reason
        );
    }

    // In BookingDetailActivity.java - When booking is cancelled:
    void onBookingCancelled_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get booking details
        String otherUserId = "user_to_notify_id";
        String bookingId = "booking123";
        String dormName = "Sample Dorm";
        String cancelledBy = "Student"; // or "Landlord"
        
        // Notify the other party
        notificationManager.sendBookingCancelledNotification(
            otherUserId, 
            bookingId, 
            dormName, 
            cancelledBy
        );
    }

    /*
     * ========================================
     * 2. PAYMENT FLOW NOTIFICATIONS
     * ========================================
     */

    // In PaymentActivity.java - When payment is confirmed:
    void onPaymentConfirmed_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get payment details
        String studentId = "student_user_id";
        String landlordId = "landlord_user_id";
        String bookingId = "booking123";
        String dormName = "Sample Dorm";
        String studentName = "John Doe";
        double amount = 5000.00;
        
        // Notify student
        notificationManager.sendPaymentConfirmedNotification(
            studentId, 
            bookingId, 
            dormName, 
            amount
        );
        
        // Notify landlord
        notificationManager.sendPaymentReceivedNotification(
            landlordId, 
            bookingId, 
            studentName, 
            dormName, 
            amount
        );
    }

    // In Firebase Cloud Function or Background Job - Check for due payments:
    void checkPaymentDueDates_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // For each booking with payment due in 3 days:
        String studentId = "student_user_id";
        String bookingId = "booking123";
        String dormName = "Sample Dorm";
        String dueDate = "2025-11-01";
        double amount = 5000.00;
        
        notificationManager.sendPaymentDueNotification(
            studentId, 
            bookingId, 
            dormName, 
            dueDate, 
            amount
        );
    }

    /*
     * ========================================
     * 3. MESSAGE NOTIFICATIONS
     * ========================================
     */

    // In ChatActivity.java - When message is sent:
    void onMessageSent_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get message details
        String recipientId = "recipient_user_id";
        String chatId = "chat123";
        String senderName = "John Doe";
        String messageText = "Hi, is the room still available?";
        
        // Truncate message preview to 50 chars
        String preview = messageText.length() > 50 
            ? messageText.substring(0, 47) + "..." 
            : messageText;
        
        // Notify recipient
        notificationManager.sendNewMessageNotification(
            recipientId, 
            chatId, 
            senderName, 
            preview
        );
    }

    /*
     * ========================================
     * 4. REVIEW NOTIFICATIONS
     * ========================================
     */

    // In ReviewActivity.java - When review is submitted:
    void onReviewSubmitted_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get review details
        String landlordId = "landlord_user_id";
        String reviewId = "review123";
        String dormName = "Sample Dorm";
        String reviewerName = "John Doe";
        double rating = 4.5;
        
        // Notify landlord
        notificationManager.sendNewReviewNotification(
            landlordId, 
            reviewId, 
            dormName, 
            reviewerName, 
            rating
        );
    }

    // In LandlordReviewDetailActivity.java - When landlord replies:
    void onReviewReply_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get review details
        String studentId = "student_user_id";
        String reviewId = "review123";
        String dormName = "Sample Dorm";
        
        // Notify student
        notificationManager.sendReviewReplyNotification(
            studentId, 
            reviewId, 
            dormName
        );
    }

    // In Admin Panel or Cloud Function - When review is verified:
    void onReviewVerified_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get review details
        String studentId = "student_user_id";
        String reviewId = "review123";
        String dormName = "Sample Dorm";
        
        // Notify student
        notificationManager.sendReviewVerifiedNotification(
            studentId, 
            reviewId, 
            dormName
        );
    }

    /*
     * ========================================
     * 5. DORMITORY UPDATE NOTIFICATIONS
     * ========================================
     */

    // In EditDormitoryActivity.java - When dorm becomes available:
    void onDormAvailable_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get all users who favorited this dorm
        String dormId = "dorm123";
        String dormName = "Sample Dorm";
        
        // For each user who favorited:
        String userId = "user_id";
        notificationManager.sendDormAvailableNotification(
            userId, 
            dormId, 
            dormName
        );
    }

    // In EditDormitoryActivity.java - When price changes:
    void onPriceChange_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Get all users who favorited this dorm
        String dormId = "dorm123";
        String dormName = "Sample Dorm";
        double oldPrice = 5000.00;
        double newPrice = 4500.00;
        
        // For each user who favorited:
        String userId = "user_id";
        notificationManager.sendPriceChangeNotification(
            userId, 
            dormId, 
            dormName, 
            oldPrice, 
            newPrice
        );
    }

    /*
     * ========================================
     * 6. SYSTEM NOTIFICATIONS
     * ========================================
     */

    // In RegistrationActivity.java - After successful registration:
    void onUserRegistered_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        String userId = "new_user_id";
        String userName = "John Doe";
        
        // Send welcome notification
        notificationManager.sendWelcomeNotification(userId, userName);
    }

    // In VerificationActivity.java - After email/phone verification:
    void onAccountVerified_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        String userId = "user_id";
        
        // Send verification confirmation
        notificationManager.sendAccountVerifiedNotification(userId);
    }

    // In LoginActivity.java - After login from new device:
    void onNewDeviceLogin_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        String userId = "user_id";
        String deviceInfo = "Samsung Galaxy S21";
        String location = "Manila, Philippines";
        
        // Send security alert
        notificationManager.sendSecurityAlertNotification(
            userId, 
            deviceInfo, 
            location
        );
    }

    /*
     * ========================================
     * 7. NOTIFICATION ACTIVITY USAGE
     * ========================================
     */

    // In NotificationActivity.java - Mark notification as read when clicked:
    void onNotificationClicked_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        String notificationId = "notif123";
        
        // Mark as read
        notificationManager.markAsRead(notificationId);
        
        // Then navigate to appropriate screen based on actionUrl
    }

    // In NotificationActivity.java - Mark all as read:
    void onMarkAllAsRead_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        String currentUserId = "user_id";
        
        // Mark all notifications as read
        notificationManager.markAllAsRead(currentUserId);
    }

    // In NotificationActivity.java - Delete notification:
    void onDeleteNotification_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        String notificationId = "notif123";
        
        // Delete notification
        notificationManager.deleteNotification(notificationId);
    }

    // In NotificationActivity.java - Clear all read notifications:
    void onClearReadNotifications_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        String currentUserId = "user_id";
        
        // Delete all read notifications
        notificationManager.deleteReadNotifications(currentUserId);
    }

    /*
     * ========================================
     * 8. BACKGROUND JOBS / SCHEDULED TASKS
     * ========================================
     */

    // Create a WorkManager job to send check-in reminders:
    void scheduleCheckInReminders_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Query bookings with check-in date tomorrow
        // For each booking:
        String studentId = "student_user_id";
        String bookingId = "booking123";
        String dormName = "Sample Dorm";
        String checkInDate = "2025-11-01";
        
        notificationManager.sendCheckInReminderNotification(
            studentId,
            bookingId,
            dormName,
            checkInDate
        );
    }

    // Create a WorkManager job to send check-out reminders:
    void scheduleCheckOutReminders_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Query bookings with check-out date in 3 days
        // For each booking:
        String studentId = "student_user_id";
        String bookingId = "booking123";
        String dormName = "Sample Dorm";
        String checkOutDate = "2025-11-30";
        
        notificationManager.sendCheckOutReminderNotification(
            studentId,
            bookingId,
            dormName,
            checkOutDate
        );
    }

    // Create a WorkManager job to check overdue payments:
    void checkOverduePayments_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Query bookings with overdue payments
        // For each overdue payment:
        String studentId = "student_user_id";
        String bookingId = "booking123";
        String dormName = "Sample Dorm";
        double amount = 5000.00;
        
        notificationManager.sendPaymentOverdueNotification(
            studentId,
            bookingId,
            dormName,
            amount
        );
    }

    /*
     * ========================================
     * 9. RECOMMENDATION ENGINE
     * ========================================
     */

    // Send personalized recommendations based on user preferences:
    void sendRecommendations_Example() {
        NotificationManager notificationManager = new NotificationManager();
        
        // Based on user's search history, favorites, etc.
        String userId = "user_id";
        String dormName = "Cozy Student Lodge";
        String reason = "Based on your recent searches for dorms near universities";
        
        notificationManager.sendRecommendationNotification(
            userId,
            dormName,
            reason
        );
    }

    /*
     * ========================================
     * 10. BEST PRACTICES
     * ========================================
     */

    /**
     * WHEN TO SEND NOTIFICATIONS:
     * 
     * ✅ DO:
     * - Send notifications for important user actions (bookings, payments)
     * - Send reminders for upcoming events (check-in, payment due)
     * - Send updates on user's favorited items
     * - Send security alerts
     * 
     * ❌ DON'T:
     * - Spam users with too many notifications
     * - Send notifications at inappropriate times (late night)
     * - Send promotional notifications too frequently
     * - Send notifications without user preference settings
     */

    /**
     * NOTIFICATION TIMING:
     * 
     * - Payment Due: 3 days before
     * - Check-in Reminder: 1 day before
     * - Check-out Reminder: 3 days before
     * - Payment Overdue: On due date + daily until paid
     * - New Message: Immediately (if user not in chat)
     * - New Booking: Immediately
     */

    /**
     * NOTIFICATION PREFERENCES:
     * 
     * Allow users to control:
     * - Which types of notifications they want
     * - Email vs Push vs In-app
     * - Quiet hours (no notifications during sleep)
     * - Notification sound/vibration
     */
}
