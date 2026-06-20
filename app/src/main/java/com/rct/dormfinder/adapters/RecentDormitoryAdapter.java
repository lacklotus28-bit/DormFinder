package com.rct.dormfinder.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.rct.dormfinder.R;
import com.rct.dormfinder.activities.DormitoryDetailActivity;
import com.rct.dormfinder.models.Dormitory;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying recent dormitories in horizontal list
 */
public class RecentDormitoryAdapter extends RecyclerView.Adapter<RecentDormitoryAdapter.RecentDormViewHolder> {
    private static final String TAG = "RecentDormitoryAdapter";
    private List<Dormitory> dormitories;
    private Context context;
    private NumberFormat currencyFormat;

    public RecentDormitoryAdapter(List<Dormitory> dormitories, Context context) {
        this.dormitories = dormitories;
        this.context = context;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    }

    @NonNull
    @Override
    public RecentDormViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_dormitory, parent, false);
        return new RecentDormViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentDormViewHolder holder, int position) {
        Dormitory dormitory = dormitories.get(position);

        holder.tvName.setText(dormitory.getName());
        holder.tvAddress.setText(dormitory.getAddress());
        holder.tvPrice.setText(currencyFormat.format(dormitory.getMonthlyPrice()) + "/mo");
        holder.tvAvailableRooms.setText(dormitory.getAvailableRooms() + " rooms");

        // Load first image if available
        if (dormitory.getImages() != null && !dormitory.getImages().isEmpty()) {
            String imageUrl = dormitory.getImages().get(0);
            Log.d(TAG, "Loading image for " + dormitory.getName() + ": " + imageUrl);
            
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_dorm)
                .error(R.drawable.placeholder_dorm)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.ivImage);
        } else {
            Log.d(TAG, "No images for " + dormitory.getName());
            holder.ivImage.setImageResource(R.drawable.placeholder_dorm);
        }

        // Display rating
        holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f", dormitory.getAverageRating()));

        // Display amenities
        if (dormitory.getAmenities() != null && !dormitory.getAmenities().isEmpty()) {
            StringBuilder amenitiesText = new StringBuilder();
            int maxAmenities = Math.min(3, dormitory.getAmenities().size());
            for (int i = 0; i < maxAmenities; i++) {
                if (i > 0) amenitiesText.append(" • ");
                amenitiesText.append(dormitory.getAmenities().get(i));
            }
            holder.tvAmenities.setText(amenitiesText.toString());
        } else {
            holder.tvAmenities.setText("No amenities listed");
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DormitoryDetailActivity.class);
            intent.putExtra("dormitory_id", dormitory.getDormId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dormitories.size();
    }

    public void updateData(List<Dormitory> newDormitories) {
        this.dormitories = newDormitories;
        notifyDataSetChanged();
    }

    static class RecentDormViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvAddress, tvPrice, tvAvailableRooms, tvAmenities, tvRating;

        public RecentDormViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivDormImage);
            tvName = itemView.findViewById(R.id.tvDormName);
            tvAddress = itemView.findViewById(R.id.tvDormAddress);
            tvPrice = itemView.findViewById(R.id.tvDormPrice);
            tvAvailableRooms = itemView.findViewById(R.id.tvAvailableRooms);
            tvAmenities = itemView.findViewById(R.id.tvAmenities);
            tvRating = itemView.findViewById(R.id.tvRating);
        }
    }
}
