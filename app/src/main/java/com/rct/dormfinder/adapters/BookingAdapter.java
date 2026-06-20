package com.rct.dormfinder.adapters;

import android.content.Context;
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

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {
    private List<Booking> bookings;
    private Context context;
    private OnBookingActionListener listener;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    public interface OnBookingActionListener {
        void onApproveBooking(Booking booking);
        void onDeclineBooking(Booking booking);
        void onCallStudent(Booking booking);
        void onMessageStudent(Booking booking);
        void onViewDormitory(Booking booking);
    }

    public BookingAdapter(List<Booking> bookings, Context context, OnBookingActionListener listener) {
        this.bookings = bookings;
        this.context = context;
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking_request, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);

        // Basic information
        holder.tvStudentName.setText(booking.getStudentName());
        holder.tvDormitoryName.setText(booking.getDormitoryName());
        holder.tvMonthlyPrice.setText(currencyFormat.format(booking.getMonthlyPrice()));
        
        // Student details
        StringBuilder studentInfo = new StringBuilder();
        if (booking.getStudentSchool() != null) {
            studentInfo.append(booking.getStudentSchool());
        }
        if (booking.getStudentCourse() != null) {
            if (studentInfo.length() > 0) studentInfo.append(" • ");
            studentInfo.append(booking.getStudentCourse());
        }
        holder.tvStudentInfo.setText(studentInfo.toString());
        
        // Contact information
        holder.tvStudentPhone.setText(booking.getStudentPhone());
        holder.tvStudentEmail.setText(booking.getStudentEmail());

        // Message
        holder.tvMessage.setText(booking.getMessage());

        // Dates
        holder.tvRequestDate.setText("Requested: " + dateFormat.format(new Date(booking.getRequestDate())));
        
        if (booking.getMoveInDate() > 0) {
            holder.tvMoveInDate.setText("Move-in: " + dateFormat.format(new Date(booking.getMoveInDate())));
            holder.tvMoveInDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvMoveInDate.setVisibility(View.GONE);
        }

        // Status
        updateStatusDisplay(holder, booking.getStatus());

        // Action buttons
        setupActionButtons(holder, booking);

        // Click listeners
        holder.ivCall.setOnClickListener(v -> listener.onCallStudent(booking));
        holder.ivMessage.setOnClickListener(v -> listener.onMessageStudent(booking));
        holder.tvDormitoryName.setOnClickListener(v -> listener.onViewDormitory(booking));
    }

    private void updateStatusDisplay(BookingViewHolder holder, String status) {
        holder.tvStatus.setText(status.toUpperCase());
        
        int statusColor;
        int backgroundColor;
        
        switch (status.toLowerCase()) {
            case "pending":
                statusColor = ContextCompat.getColor(context, R.color.orange_primary);
                backgroundColor = ContextCompat.getColor(context, R.color.orange_light);
                break;
            case "approved":
                statusColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
                backgroundColor = ContextCompat.getColor(context, R.color.status_approved_bg);
                break;
            case "declined":
                statusColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
                backgroundColor = ContextCompat.getColor(context, R.color.status_declined_bg);
                break;
            default:
                statusColor = ContextCompat.getColor(context, android.R.color.darker_gray);
                backgroundColor = ContextCompat.getColor(context, R.color.gray_light);
                break;
        }
        
        holder.tvStatus.setTextColor(statusColor);
        holder.tvStatus.setBackgroundColor(backgroundColor);
    }

    private void setupActionButtons(BookingViewHolder holder, Booking booking) {
        if ("pending".equals(booking.getStatus())) {
            // Show approve/decline buttons for pending requests
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnDecline.setVisibility(View.VISIBLE);
            holder.tvResponseDate.setVisibility(View.GONE);
            
            holder.btnApprove.setOnClickListener(v -> listener.onApproveBooking(booking));
            holder.btnDecline.setOnClickListener(v -> listener.onDeclineBooking(booking));
        } else {
            // Hide action buttons for responded requests
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnDecline.setVisibility(View.GONE);
            
            // Show response date
            if (booking.getResponseDate() > 0) {
                holder.tvResponseDate.setText("Responded: " + dateFormat.format(new Date(booking.getResponseDate())));
                holder.tvResponseDate.setVisibility(View.VISIBLE);
            } else {
                holder.tvResponseDate.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void updateBookings(List<Booking> newBookings) {
        this.bookings = newBookings;
        notifyDataSetChanged();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvDormitoryName, tvStudentInfo, tvStudentPhone, tvStudentEmail;
        TextView tvMessage, tvMonthlyPrice, tvStatus, tvRequestDate, tvMoveInDate, tvResponseDate;
        Button btnApprove, btnDecline;
        ImageView ivCall, ivMessage;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvDormitoryName = itemView.findViewById(R.id.tvDormitoryName);
            tvStudentInfo = itemView.findViewById(R.id.tvStudentInfo);
            tvStudentPhone = itemView.findViewById(R.id.tvStudentPhone);
            tvStudentEmail = itemView.findViewById(R.id.tvStudentEmail);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvMonthlyPrice = itemView.findViewById(R.id.tvMonthlyPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            tvMoveInDate = itemView.findViewById(R.id.tvMoveInDate);
            tvResponseDate = itemView.findViewById(R.id.tvResponseDate);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            ivCall = itemView.findViewById(R.id.ivCall);
            ivMessage = itemView.findViewById(R.id.ivMessage);
        }
    }
}
