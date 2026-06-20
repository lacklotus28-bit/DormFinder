package com.rct.dormfinder.utils;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rct.dormfinder.R;

/**
 * Jarvis-style confirmation dialog helper
 * Provides elegant confirmation prompts for user actions with white/light backgrounds
 */
public class ConfirmationDialogHelper {

    public interface OnConfirmListener {
        void onConfirm();
        void onCancel();
    }

    /**
     * Show a professional confirmation dialog with white background
     */
    public static void showConfirmationDialog(
            Context context,
            String title,
            String message,
            String positiveButtonText,
            String negativeButtonText,
            OnConfirmListener listener
    ) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    if (listener != null) {
                        listener.onConfirm();
                    }
                })
                .setNegativeButton(negativeButtonText, (dialog, which) -> {
                    if (listener != null) {
                        listener.onCancel();
                    }
                })
                .setBackground(context.getResources().getDrawable(R.drawable.dialog_background, null))
                .setCancelable(false)
                .show();
    }

    /**
     * Show confirmation dialog for deleting dormitory
     */
    public static void showDeleteDormitoryDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Delete Dormitory",
                "Are you sure you want to delete this dormitory? This action cannot be undone.",
                "Delete",
                "Cancel",
                listener
        );
    }

    /**
     * Show confirmation dialog for leaving without saving
     */
    public static void showLeaveWithoutSavingDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Discard Changes?",
                "You have unsaved changes. Are you sure you want to leave? All changes will be lost.",
                "Discard",
                "Keep Editing",
                listener
        );
    }

    /**
     * Show confirmation dialog for canceling booking
     */
    public static void showCancelBookingDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Cancel Booking Request?",
                "Are you sure you want to cancel this booking request? This action cannot be undone.",
                "Yes, Cancel",
                "No, Keep It",
                listener
        );
    }

    /**
     * Show confirmation dialog for leaving form
     */
    public static void showLeaveFormDialog(Context context, String formName, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Leave " + formName + "?",
                "Are you sure you want to go back? All entered information will be lost.",
                "Yes, Leave",
                "Continue",
                listener
        );
    }

    /**
     * Show confirmation dialog for canceling payment
     */
    public static void showCancelPaymentDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Cancel Payment?",
                "Are you sure you want to cancel the payment process? Your booking will remain unpaid.",
                "Yes, Cancel",
                "Continue Payment",
                listener
        );
    }

    /**
     * Show confirmation dialog for canceling review
     */
    public static void showCancelReviewDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Discard Review?",
                "Are you sure you want to discard this review? Your rating and comments will not be saved.",
                "Discard",
                "Continue",
                listener
        );
    }

    /**
     * Show confirmation dialog for exiting filter/search
     */
    public static void showExitFilterDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Exit Search?",
                "Are you sure you want to go back? Your current filters and search will be cleared.",
                "Yes, Exit",
                "Stay",
                listener
        );
    }

    /**
     * Show confirmation dialog for leaving profile edit mode
     */
    public static void showLeaveProfileEditDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Discard Profile Changes?",
                "You have unsaved profile changes. Are you sure you want to discard them?",
                "Discard",
                "Keep Editing",
                listener
        );
    }

    /**
     * Show confirmation dialog for leaving chat
     */
    public static void showLeaveChatDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Leave Chat?",
                "Are you sure you want to leave this conversation? Your message history will remain.",
                "Yes, Leave",
                "Stay",
                listener
        );
    }

    /**
     * Generic dialog for going back from any activity
     */
    public static void showGoBackDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Go Back?",
                "Are you sure you want to go back?",
                "Yes",
                "No",
                listener
        );
    }

    /**
     * Show confirmation with custom message
     */
    public static void showCustomConfirmation(
            Context context,
            String title,
            String message,
            OnConfirmListener listener
    ) {
        showConfirmationDialog(
                context,
                title,
                message,
                "Confirm",
                "Cancel",
                listener
        );
    }

    /**
     * Show confirmation dialog for rejecting booking
     */
    public static void showRejectBookingDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Reject Booking?",
                "Are you sure you want to reject this booking request? The student will be notified.",
                "Reject",
                "Cancel",
                listener
        );
    }

    /**
     * Show confirmation dialog for accepting booking
     */
    public static void showAcceptBookingDialog(Context context, OnConfirmListener listener) {
        showConfirmationDialog(
                context,
                "Accept Booking?",
                "Are you sure you want to accept this booking request? The student will be notified.",
                "Accept",
                "Cancel",
                listener
        );
    }
}
