package com.rct.dormfinder.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private Context context;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
        void onNotificationDelete(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, Context context, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());
        
        // Handle null timestamp gracefully
        if (notification.getCreatedAt() != null) {
            holder.tvTimestamp.setText(getTimeAgo(notification.getCreatedAt().toDate()));
        } else {
            holder.tvTimestamp.setText("Recently");
        }

        // Set notification icon based on type
        int iconResId = getNotificationIcon(notification.getType());
        holder.ivNotificationIcon.setImageResource(iconResId);

        // Set read/unread state
        if (notification.isRead()) {
            holder.cardNotification.setCardBackgroundColor(
                    context.getResources().getColor(android.R.color.white));
            holder.viewUnreadIndicator.setVisibility(View.GONE);
        } else {
            holder.cardNotification.setCardBackgroundColor(
                    context.getResources().getColor(R.color.notification_unread_bg));
            holder.viewUnreadIndicator.setVisibility(View.VISIBLE);
        }

        // Load image if available
        if (notification.getImageUrl() != null && !notification.getImageUrl().isEmpty()) {
            holder.ivNotificationImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(notification.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.ivNotificationImage);
        } else {
            holder.ivNotificationImage.setVisibility(View.GONE);
        }

        // Click listeners
        holder.cardNotification.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });

        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationDelete(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private int getNotificationIcon(String type) {
        switch (type) {
            case "booking":
                return R.drawable.ic_booking;
            case "payment":
                return R.drawable.ic_payment;
            case "message":
                return R.drawable.ic_message;
            case "review":
                return R.drawable.ic_star;
            case "dormitory":
                return R.drawable.ic_home;
            default:
                return R.drawable.ic_notifications;
        }
    }

    private String getTimeAgo(Date date) {
        // Handle null date
        if (date == null) {
            return "Recently";
        }
        
        long timeMillis = date.getTime();
        long now = System.currentTimeMillis();
        long diff = now - timeMillis;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (weeks < 4) {
            return weeks + " week" + (weeks > 1 ? "s" : "") + " ago";
        } else if (months < 12) {
            return months + " month" + (months > 1 ? "s" : "") + " ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(date);
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        CardView cardNotification;
        ImageView ivNotificationIcon;
        ImageView ivNotificationImage;
        ImageView ivDelete;
        TextView tvTitle;
        TextView tvMessage;
        TextView tvTimestamp;
        View viewUnreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            ivNotificationIcon = itemView.findViewById(R.id.ivNotificationIcon);
            ivNotificationImage = itemView.findViewById(R.id.ivNotificationImage);
            ivDelete = itemView.findViewById(R.id.ivDelete);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
        }
    }
}
