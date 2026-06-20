package com.rct.dormfinder.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;



public class LandlordReviewAdapter extends RecyclerView.Adapter<LandlordReviewAdapter.ViewHolder> {
    
    private List<Review> reviews;
    private Context context;
    private Map<String, String> dormIdToNameMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    
    public LandlordReviewAdapter(List<Review> reviews, Context context, Map<String, String> dormIdToNameMap) {
        this.reviews = reviews;
        this.context = context;
        this.dormIdToNameMap = dormIdToNameMap;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_landlord_review, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);
        
        // Student info
        holder.tvStudentName.setText(review.getStudentName());
        
        // Dormitory name
        String dormName = dormIdToNameMap.get(review.getDormId());
        holder.tvDormitoryName.setText(dormName != null ? dormName : "Unknown Dormitory");
        
        // Rating
        holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f", review.getRating()));
        holder.ratingBar.setRating(review.getRating());
        
        // Date
        holder.tvReviewDate.setText(getTimeAgo(review.getDatePosted()));
        
        // Comment
        holder.tvReviewComment.setText(review.getComment());
        
        // Verified badge
        if (review.isVerified()) {
            holder.tvVerifiedBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvVerifiedBadge.setVisibility(View.GONE);
        }
        
        // Profile image
        if (review.getStudentProfileImageUrl() != null && !review.getStudentProfileImageUrl().isEmpty()) {
            // User has a profile image - load it
            Glide.with(context)
                    .load(review.getStudentProfileImageUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.ivStudentProfile);
        } else {
            // No profile image - show default icon
            holder.ivStudentProfile.setImageResource(R.drawable.ic_person);
        }
        
        // Landlord reply section
        if (review.hasReply() && review.getLandlordReply() != null && !review.getLandlordReply().isEmpty()) {
            holder.layoutLandlordReply.setVisibility(View.VISIBLE);
            holder.tvLandlordReply.setText(review.getLandlordReply());
            holder.tvReplyDate.setText(getTimeAgo(review.getReplyDate()));
            
            holder.btnReply.setText("Edit Reply");
            holder.btnEditReply.setVisibility(View.GONE);
        } else {
            holder.layoutLandlordReply.setVisibility(View.GONE);
            holder.btnReply.setText("Reply");
            holder.btnEditReply.setVisibility(View.GONE);
        }
        
        // Reply button click
        holder.btnReply.setOnClickListener(v -> showReplyDialog(review));
    }
    
    @Override
    public int getItemCount() {
        return reviews.size();
    }
    
    private void showReplyDialog(Review review) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reply_review, null);
        
        TextView tvOriginalRating = dialogView.findViewById(R.id.tvOriginalRating);
        TextView tvOriginalComment = dialogView.findViewById(R.id.tvOriginalComment);
        EditText etReply = dialogView.findViewById(R.id.etReply);
        TextView tvCharCount = dialogView.findViewById(R.id.tvCharCount);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSubmitReply = dialogView.findViewById(R.id.btnSubmitReply);
        
        // Show original review
        tvOriginalRating.setText(String.format(Locale.getDefault(), "%.1f", review.getRating()));
        tvOriginalComment.setText(review.getComment());
        
        // Pre-fill if editing existing reply
        if (review.hasReply() && review.getLandlordReply() != null) {
            etReply.setText(review.getLandlordReply());
        }
        
        // Character counter
        etReply.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/500");
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSubmitReply.setOnClickListener(v -> {
            String replyText = etReply.getText().toString().trim();
            
            if (replyText.isEmpty()) {
                Toast.makeText(context, "Please write a reply", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (replyText.length() < 10) {
                Toast.makeText(context, "Reply must be at least 10 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            
            submitReply(review, replyText, dialog);
        });
        
        dialog.show();
    }
    
    private void submitReply(Review review, String replyText, AlertDialog dialog) {
        // Get landlord name
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String landlordNameTemp = documentSnapshot.getString("name");
                    final String landlordName = (landlordNameTemp != null) ? landlordNameTemp : "Landlord";
                    
                    // Update review with reply
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("landlordReply", replyText);
                    updates.put("replyDate", System.currentTimeMillis());
                    updates.put("landlordName", landlordName);
                    updates.put("hasReply", true);
                    
                    db.collection("reviews").document(review.getReviewId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Reply posted successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                
                                // Send notification to student
                                sendReplyNotification(review, landlordName);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to post reply: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to get landlord info", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void sendReplyNotification(Review review, String landlordName) {
        // Create notification for student
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", review.getStudentId());
        notification.put("title", "Landlord Replied to Your Review");
        notification.put("message", landlordName + " replied to your review");
        notification.put("type", "review_reply");
        notification.put("relatedId", review.getDormId()); // Changed from dormId to relatedId
        notification.put("createdAt", com.google.firebase.Timestamp.now()); // Changed from timestamp to createdAt with proper Timestamp
        notification.put("isRead", false);
        
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    android.util.Log.d("LandlordReviewAdapter", "Notification sent to student: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LandlordReviewAdapter", "Failed to send notification: " + e.getMessage());
                });
    }
    
    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
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
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (days < 7) {
            return days + (days == 1 ? " day ago" : " days ago");
        } else if (weeks < 4) {
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        } else if (months < 12) {
            return months + (months == 1 ? " month ago" : " months ago");
        } else {
            return years + (years == 1 ? " year ago" : " years ago");
        }
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStudentProfile;
        TextView tvStudentName, tvDormitoryName, tvReviewDate, tvRating, tvReviewComment;
        TextView tvVerifiedBadge, tvLandlordReply, tvReplyDate;
        RatingBar ratingBar;
        LinearLayout layoutLandlordReply;
        Button btnReply, btnEditReply;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStudentProfile = itemView.findViewById(R.id.ivStudentProfile);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvDormitoryName = itemView.findViewById(R.id.tvDormitoryName);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewComment = itemView.findViewById(R.id.tvReviewComment);
            tvVerifiedBadge = itemView.findViewById(R.id.tvVerifiedBadge);
            tvLandlordReply = itemView.findViewById(R.id.tvLandlordReply);
            tvReplyDate = itemView.findViewById(R.id.tvReplyDate);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            layoutLandlordReply = itemView.findViewById(R.id.layoutLandlordReply);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnEditReply = itemView.findViewById(R.id.btnEditReply);
        }
    }
}
