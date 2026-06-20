package com.rct.dormfinder.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Review;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> reviews;
    private Context context;

    public ReviewAdapter(List<Review> reviews, Context context) {
        this.reviews = reviews;
        this.context = context;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        
        holder.tvStudentName.setText(review.getStudentName());
        holder.ratingBar.setRating(review.getRating());
        holder.tvComment.setText(review.getComment());
        
        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateStr = sdf.format(new Date(review.getDatePosted()));
        holder.tvDate.setText(dateStr);
        
        // Show verified badge if applicable
        holder.tvVerified.setVisibility(review.isVerified() ? View.VISIBLE : View.GONE);
        
        // Load profile image
        if (review.getStudentProfileImageUrl() != null && !review.getStudentProfileImageUrl().isEmpty()) {
            // User has a profile image - load it
            Glide.with(context)
                    .load(review.getStudentProfileImageUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.ivProfile);
            
            // Remove tint when showing actual image
            holder.ivProfile.setColorFilter(null);
        } else {
            // No profile image - show default icon with styling
            holder.ivProfile.setImageResource(R.drawable.ic_person);
            // Apply tint to the default icon
            holder.ivProfile.setColorFilter(context.getResources().getColor(R.color.mint_primary));
        }
        
        // Show landlord reply if exists
        if (review.hasReply() && review.getLandlordReply() != null && !review.getLandlordReply().isEmpty()) {
            holder.layoutLandlordReply.setVisibility(View.VISIBLE);
            holder.tvLandlordReply.setText(review.getLandlordReply());
            
            // Set landlord name
            String landlordName = review.getLandlordName();
            if (landlordName != null && !landlordName.isEmpty()) {
                holder.tvLandlordName.setText(landlordName + " replied");
            } else {
                holder.tvLandlordName.setText("Landlord replied");
            }
            
            // Format reply date
            if (review.getReplyDate() > 0) {
                holder.tvReplyDate.setText(getTimeAgo(review.getReplyDate()));
            } else {
                holder.tvReplyDate.setText("");
            }
        } else {
            holder.layoutLandlordReply.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
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
        
        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + (minutes == 1 ? " min ago" : " mins ago");
        } else if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (days < 7) {
            return days + (days == 1 ? " day ago" : " days ago");
        } else if (weeks < 4) {
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        } else if (months < 12) {
            return months + (months == 1 ? " month ago" : " months ago");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvStudentName, tvDate, tvComment, tvVerified;
        TextView tvLandlordName, tvLandlordReply, tvReplyDate;
        RatingBar ratingBar;
        LinearLayout layoutLandlordReply;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvVerified = itemView.findViewById(R.id.tvVerified);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            layoutLandlordReply = itemView.findViewById(R.id.layoutLandlordReply);
            tvLandlordName = itemView.findViewById(R.id.tvLandlordName);
            tvLandlordReply = itemView.findViewById(R.id.tvLandlordReply);
            tvReplyDate = itemView.findViewById(R.id.tvReplyDate);
        }
    }
}
