package com.rct.dormfinder.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Payment;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {

    private Context context;
    private List<Payment> payments;
    private OnPaymentClickListener listener;

    public interface OnPaymentClickListener {
        void onPaymentClick(Payment payment);
    }

    public PaymentAdapter(Context context, List<Payment> payments, OnPaymentClickListener listener) {
        this.context = context;
        this.payments = payments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_payment, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment payment = payments.get(position);

        // Set dormitory name
        holder.tvDormitoryName.setText(payment.getDormitoryName());

        // Set amount
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        holder.tvAmount.setText(currencyFormat.format(payment.getAmount()));

        // Set date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.tvDate.setText(dateFormat.format(new Date(payment.getTimestamp())));

        // Set payment method
        String method = payment.getPaymentMethod();
        if (method != null) {
            holder.tvPaymentMethod.setText(method.toUpperCase());
        }

        // Set reference number
        holder.tvReference.setText(payment.getReferenceNumber());

        // Set status with color and rounded background
        String status = payment.getStatus();
        holder.tvStatus.setText(getStatusText(status));
        
        // Create rounded background
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(getStatusBackgroundColor(status));
        background.setCornerRadius(12f);
        holder.tvStatus.setBackground(background);
        holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.white));

        // Set description
        String description = payment.getDescription();
        if (description != null && !description.isEmpty() && !description.toLowerCase().contains("null")) {
            holder.tvDescription.setText(description);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPaymentClick(payment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    public void updatePayments(List<Payment> newPayments) {
        this.payments = newPayments;
        notifyDataSetChanged();
    }

    private String getStatusText(String status) {
        switch (status) {
            case "completed":
                return "Completed";
            case "pending":
                return "Pending";
            case "failed":
                return "Failed";
            case "refunded":
                return "Refunded";
            default:
                return status;
        }
    }

    private int getStatusBackgroundColor(String status) {
        switch (status) {
            case "completed":
                return context.getResources().getColor(android.R.color.holo_green_dark);
            case "pending":
                return context.getResources().getColor(android.R.color.holo_orange_dark);
            case "failed":
                return context.getResources().getColor(android.R.color.holo_red_dark);
            case "refunded":
                return context.getResources().getColor(android.R.color.holo_blue_dark);
            default:
                return context.getResources().getColor(android.R.color.darker_gray);
        }
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "completed":
                return context.getResources().getColor(android.R.color.holo_green_dark);
            case "pending":
                return context.getResources().getColor(android.R.color.holo_orange_dark);
            case "failed":
                return context.getResources().getColor(android.R.color.holo_red_dark);
            case "refunded":
                return context.getResources().getColor(android.R.color.holo_blue_dark);
            default:
                return context.getResources().getColor(android.R.color.darker_gray);
        }
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvDormitoryName;
        TextView tvAmount;
        TextView tvDate;
        TextView tvPaymentMethod;
        TextView tvReference;
        TextView tvStatus;
        TextView tvDescription;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvDormitoryName = itemView.findViewById(R.id.tvDormitoryName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvReference = itemView.findViewById(R.id.tvReference);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}
