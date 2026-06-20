package com.rct.dormfinder.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Booking;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentBookingAdapter extends RecyclerView.Adapter<StudentBookingAdapter.StudentBookingViewHolder> {
    private List<Booking> bookings;
    private Context context;
    private OnStudentBookingActionListener listener;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    public interface OnStudentBookingActionListener {
        void onViewDormitory(Booking booking);
        void onMessageLandlord(Booking booking);
        void onCancelBooking(Booking booking);
        void onPayNow(Booking booking);
        void onViewPayment(Booking booking);
    }

    public StudentBookingAdapter(List<Booking> bookings, Context context, OnStudentBookingActionListener listener) {
        this.bookings = bookings;
        this.context = context;
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public StudentBookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_student_booking, parent, false);
        return new StudentBookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentBookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);

        // Basic information
        holder.tvDormitoryName.setText(booking.getDormitoryName());
        holder.tvMonthlyPrice.setText(currencyFormat.format(booking.getMonthlyPrice()));
        
        // Message preview
        String message = booking.getMessage();
        if (message != null && message.length() > 100) {
            message = message.substring(0, 100) + "...";
        }
        holder.tvMessage.setText(message);

        // Dates
        holder.tvRequestDate.setText("Requested: " + dateFormat.format(new Date(booking.getRequestDate())));
        
        if (booking.getMoveInDate() > 0) {
            holder.tvMoveInDate.setText("Move-in: " + dateFormat.format(new Date(booking.getMoveInDate())));
            holder.tvMoveInDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvMoveInDate.setVisibility(View.GONE);
        }

        // Status
        updateStatusDisplay(holder, booking.getStatus(), booking);
        
        // Payment Status
        updatePaymentDisplay(holder, booking);

        // Action buttons based on status
        setupActionButtons(holder, booking);

        // Click listeners
        holder.tvDormitoryName.setOnClickListener(v -> listener.onViewDormitory(booking));
        holder.ivMessage.setOnClickListener(v -> listener.onMessageLandlord(booking));
    }

    private void updateStatusDisplay(StudentBookingViewHolder holder, String status, Booking booking) {
        holder.tvStatus.setText(status.toUpperCase());
        
        int statusColor;
        int backgroundColor;
        String statusIcon = "";
        
        switch (status.toLowerCase()) {
            case "pending":
                statusColor = ContextCompat.getColor(context, R.color.orange_primary);
                backgroundColor = ContextCompat.getColor(context, R.color.orange_light);
                statusIcon = "⏳";
                break;
            case "approved":
                statusColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
                backgroundColor = ContextCompat.getColor(context, R.color.status_approved_bg);
                statusIcon = "✅";
                break;
            case "confirmed":
                // NEW: Confirmed booking (payment verified by landlord)
                statusColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
                backgroundColor = ContextCompat.getColor(context, R.color.status_approved_bg);
                statusIcon = "🎉";
                break;
            case "declined":
                statusColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
                backgroundColor = ContextCompat.getColor(context, R.color.status_declined_bg);
                statusIcon = "❌";
                break;
            case "cancelled":
                statusColor = ContextCompat.getColor(context, android.R.color.darker_gray);
                backgroundColor = ContextCompat.getColor(context, R.color.gray_light);
                statusIcon = "🚫";
                break;
            default:
                statusColor = ContextCompat.getColor(context, android.R.color.darker_gray);
                backgroundColor = ContextCompat.getColor(context, R.color.gray_light);
                break;
        }
        
        holder.tvStatus.setText(statusIcon + " " + status.toUpperCase());
        holder.tvStatus.setTextColor(statusColor);
        holder.tvStatus.setBackgroundColor(backgroundColor);
        
        // Show response date if available
        if (booking.getResponseDate() > 0 && !"pending".equals(status)) {
            holder.tvResponseDate.setText("Responded: " + dateFormat.format(new Date(booking.getResponseDate())));
            holder.tvResponseDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvResponseDate.setVisibility(View.GONE);
        }
    }

    private void updatePaymentDisplay(StudentBookingViewHolder holder, Booking booking) {
        String paymentStatus = booking.getPaymentStatus();
        
        if (paymentStatus == null || paymentStatus.isEmpty()) {
            holder.tvPaymentStatus.setVisibility(View.GONE);
            return;
        }
        
        holder.tvPaymentStatus.setVisibility(View.VISIBLE);
        
        switch (paymentStatus.toLowerCase()) {
            case "paid":
                holder.tvPaymentStatus.setText("💳 PAID");
                holder.tvPaymentStatus.setTextColor(Color.parseColor("#4CAF50"));
                holder.tvPaymentStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                break;
            case "pending":
                holder.tvPaymentStatus.setText("⏳ PAYMENT PENDING");
                holder.tvPaymentStatus.setTextColor(Color.parseColor("#FF9800"));
                holder.tvPaymentStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                break;
            case "unpaid":
            default:
                holder.tvPaymentStatus.setText("❗ UNPAID");
                holder.tvPaymentStatus.setTextColor(Color.parseColor("#F44336"));
                holder.tvPaymentStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                break;
        }
    }

    private void setupActionButtons(StudentBookingViewHolder holder, Booking booking) {
        String status = booking.getStatus().toLowerCase();
        String paymentStatus = booking.getPaymentStatus();
        
        switch (status) {
            case "pending":
                // Show cancel button for pending requests
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnCancel.setOnClickListener(v -> listener.onCancelBooking(booking));
                holder.btnPayNow.setVisibility(View.GONE);
                holder.btnViewPayment.setVisibility(View.GONE);
                
                holder.btnViewDormitory.setText("View Dormitory");
                holder.tvStatusDescription.setText("Your booking request is being reviewed by the landlord.");
                break;
                
            case "approved":
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnViewDormitory.setText("View My Dorm");
                
                // Payment buttons logic for approved status
                if ("paid".equals(paymentStatus)) {
                    // Payment submitted but NOT yet verified by landlord
                    holder.btnPayNow.setVisibility(View.GONE);
                    // Only show view payment button if payment ID exists
                    if (booking.getPaymentId() != null && !booking.getPaymentId().isEmpty()) {
                        holder.btnViewPayment.setVisibility(View.VISIBLE);
                        holder.btnViewPayment.setOnClickListener(v -> listener.onViewPayment(booking));
                        holder.tvStatusDescription.setText("⏳ Payment submitted! Waiting for landlord to verify your payment.");
                    } else {
                        holder.btnViewPayment.setVisibility(View.GONE);
                        holder.tvStatusDescription.setText("⏳ Payment is being processed. You can check Payment History for details.");
                    }
                } else if ("pending".equals(paymentStatus)) {
                    holder.btnPayNow.setVisibility(View.GONE);
                    // Only show view payment button if payment ID exists
                    if (booking.getPaymentId() != null && !booking.getPaymentId().isEmpty()) {
                        holder.btnViewPayment.setVisibility(View.VISIBLE);
                        holder.btnViewPayment.setOnClickListener(v -> listener.onViewPayment(booking));
                        holder.tvStatusDescription.setText("⏳ Payment verification in progress. Please wait for landlord confirmation.");
                    } else {
                        holder.btnViewPayment.setVisibility(View.GONE);
                        holder.tvStatusDescription.setText("⏳ Payment is being processed. You can check Payment History for details.");
                    }
                } else {
                    // No payment yet
                    holder.btnPayNow.setVisibility(View.VISIBLE);
                    holder.btnViewPayment.setVisibility(View.GONE);
                    holder.btnPayNow.setOnClickListener(v -> listener.onPayNow(booking));
                    holder.tvStatusDescription.setText("🎉 Booking approved! Please proceed with payment to confirm your reservation.");
                }
                break;
                
            case "confirmed":
                // NEW: Confirmed status means payment is verified and booking is active
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnPayNow.setVisibility(View.GONE);
                holder.btnViewDormitory.setText("View My Dorm");
                
                // Show view payment button to see payment details
                if (booking.getPaymentId() != null && !booking.getPaymentId().isEmpty()) {
                    holder.btnViewPayment.setVisibility(View.VISIBLE);
                    holder.btnViewPayment.setOnClickListener(v -> listener.onViewPayment(booking));
                } else {
                    holder.btnViewPayment.setVisibility(View.GONE);
                }
                
                holder.tvStatusDescription.setText("🎉 Booking confirmed! Your payment has been verified. Welcome to your new dorm!");
                break;
                
            case "declined":
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnPayNow.setVisibility(View.GONE);
                holder.btnViewPayment.setVisibility(View.GONE);
                holder.btnViewDormitory.setText("View Dormitory");
                holder.tvStatusDescription.setText("Your booking request was declined. You can try other dormitories or contact the landlord for more information.");
                break;
                
            case "cancelled":
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnPayNow.setVisibility(View.GONE);
                holder.btnViewPayment.setVisibility(View.GONE);
                holder.btnViewDormitory.setText("View Dormitory");
                holder.tvStatusDescription.setText("You cancelled this booking request.");
                break;
                
            default:
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnPayNow.setVisibility(View.GONE);
                holder.btnViewPayment.setVisibility(View.GONE);
                holder.btnViewDormitory.setText("View Dormitory");
                holder.tvStatusDescription.setText("");
                break;
        }
        
        holder.btnViewDormitory.setOnClickListener(v -> listener.onViewDormitory(booking));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void updateBookings(List<Booking> newBookings) {
        this.bookings = newBookings;
        notifyDataSetChanged();
    }

    static class StudentBookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvDormitoryName, tvMonthlyPrice, tvMessage, tvStatus, tvStatusDescription;
        TextView tvRequestDate, tvMoveInDate, tvResponseDate, tvPaymentStatus;
        Button btnViewDormitory, btnCancel, btnPayNow, btnViewPayment;
        ImageView ivMessage;

        public StudentBookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDormitoryName = itemView.findViewById(R.id.tvDormitoryName);
            tvMonthlyPrice = itemView.findViewById(R.id.tvMonthlyPrice);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvStatusDescription = itemView.findViewById(R.id.tvStatusDescription);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            tvMoveInDate = itemView.findViewById(R.id.tvMoveInDate);
            tvResponseDate = itemView.findViewById(R.id.tvResponseDate);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            btnViewDormitory = itemView.findViewById(R.id.btnViewDormitory);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnPayNow = itemView.findViewById(R.id.btnPayNow);
            btnViewPayment = itemView.findViewById(R.id.btnViewPayment);
            ivMessage = itemView.findViewById(R.id.ivMessage);
        }
    }
}
