package com.rct.dormfinder.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.activities.DormitoryDetailActivity;
import com.rct.dormfinder.models.Dormitory;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AllDormsAdapter extends RecyclerView.Adapter<AllDormsAdapter.AllDormViewHolder> {
    private static final String TAG = "AllDormsAdapter";

    private Context context;
    private List<Dormitory> dormitories;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private boolean isGridLayout = false; // New flag for layout type

    public AllDormsAdapter(Context context, List<Dormitory> dormitories) {
        this.context = context;
        this.dormitories = dormitories;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // Constructor with layout type
    public AllDormsAdapter(Context context, List<Dormitory> dormitories, boolean isGridLayout) {
        this.context = context;
        this.dormitories = dormitories;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.isGridLayout = isGridLayout;
    }

    @NonNull
    @Override
    public AllDormViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isGridLayout ? R.layout.item_all_dorm_grid : R.layout.item_all_dorm;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new AllDormViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AllDormViewHolder holder, int position) {
        Dormitory dormitory = dormitories.get(position);
        
        holder.tvDormName.setText(dormitory.getName());
        holder.tvLocation.setText(dormitory.getAddress());
        
        // Format price
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        holder.tvPrice.setText(formatter.format(dormitory.getMonthlyPrice()));

        // Rating
        if (dormitory.getAverageRating() > 0) {
            holder.tvRating.setText(String.format("%.1f", dormitory.getAverageRating()));
        } else {
            holder.tvRating.setText("N/A");
        }

        // Available rooms (X/Y format)
        int availableRooms = dormitory.getAvailableRooms();
        int totalRooms = dormitory.getTotalRooms();
        if (holder.tvAvailableRooms != null) {
            holder.tvAvailableRooms.setText(availableRooms + "/" + totalRooms + " rooms");
        }

        // Display amenities (only for grid layout)
        if (holder.tvAmenities != null) {
            if (dormitory.getAmenities() != null && !dormitory.getAmenities().isEmpty()) {
                StringBuilder amenitiesText = new StringBuilder();
                int maxAmenities = Math.min(3, dormitory.getAmenities().size());
                for (int i = 0; i < maxAmenities; i++) {
                    if (i > 0) amenitiesText.append(" • ");
                    amenitiesText.append(dormitory.getAmenities().get(i));
                }
                holder.tvAmenities.setText(amenitiesText.toString());
                holder.tvAmenities.setVisibility(View.VISIBLE);
            } else {
                holder.tvAmenities.setVisibility(View.GONE);
            }
        }

        // Load image with proper error handling
        if (dormitory.getImages() != null && !dormitory.getImages().isEmpty()) {
            String imageUrl = dormitory.getImages().get(0);
            Log.d(TAG, "Loading image for " + dormitory.getName() + ": " + imageUrl);
            
            holder.ivPlaceholder.setVisibility(View.GONE);
            holder.ivDormImage.setVisibility(View.VISIBLE);
            
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_dorm)
                .error(R.drawable.placeholder_dorm)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.ivDormImage);
        } else {
            Log.d(TAG, "No images for " + dormitory.getName());
            holder.ivPlaceholder.setVisibility(View.VISIBLE);
            holder.ivDormImage.setVisibility(View.GONE);
            holder.ivDormImage.setImageResource(0);
        }

        // Check if favorited
        checkIfFavorited(holder, dormitory.getDormId());

        // Favorite button click
        holder.ivFavorite.setOnClickListener(v -> toggleFavorite(holder, dormitory.getDormId()));

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DormitoryDetailActivity.class);
            intent.putExtra("dormitory_id", dormitory.getDormId());
            context.startActivity(intent);
        });
    }

    private void checkIfFavorited(AllDormViewHolder holder, String dormitoryId) {
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.d(TAG, "No user logged in, cannot check favorites");
            holder.ivFavorite.setImageResource(R.drawable.ic_favorite_border);
            return;
        }
        if (user.isAnonymous()) {
            Log.d(TAG, "Guest user, favorites disabled");
            holder.ivFavorite.setImageResource(R.drawable.ic_favorite_border);
            return;
        }

        String userId = user.getUid();
        Log.d(TAG, "Checking if favorited - dormId: " + dormitoryId + ", userId: " + userId);
        
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(dormitoryId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    holder.ivFavorite.setImageResource(R.drawable.ic_favorite_filled);
                    Log.d(TAG, "Dormitory " + dormitoryId + " IS FAVORITED");
                } else {
                    holder.ivFavorite.setImageResource(R.drawable.ic_favorite_border);
                    Log.d(TAG, "Dormitory " + dormitoryId + " is NOT favorited");
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error checking favorite status for " + dormitoryId + ": " + e.getMessage());
                holder.ivFavorite.setImageResource(R.drawable.ic_favorite_border);
            });
    }

    private void toggleFavorite(AllDormViewHolder holder, String dormitoryId) {
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        if (user.isAnonymous()) {
            Toast.makeText(context, "Sign in to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        Log.d(TAG, "toggleFavorite called for dormId: " + dormitoryId + ", userId: " + userId);
        
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(dormitoryId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Log.d(TAG, "Removing favorite for dormId: " + dormitoryId);
                    // Remove from favorites
                    db.collection("users")
                        .document(userId)
                        .collection("favorites")
                        .document(dormitoryId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            holder.ivFavorite.setImageResource(R.drawable.ic_favorite_border);
                            Log.d(TAG, "Successfully removed favorite from Firestore: " + dormitoryId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error removing favorite: " + e.getMessage());
                        });
                } else {
                    Log.d(TAG, "Adding favorite for dormId: " + dormitoryId);
                    // Add to favorites
                    java.util.Map<String, Object> favoriteData = new java.util.HashMap<>();
                    favoriteData.put("dormitoryId", dormitoryId);
                    favoriteData.put("timestamp", System.currentTimeMillis());
                    
                    db.collection("users")
                        .document(userId)
                        .collection("favorites")
                        .document(dormitoryId)
                        .set(favoriteData)
                        .addOnSuccessListener(aVoid -> {
                            holder.ivFavorite.setImageResource(R.drawable.ic_favorite_filled);
                            Log.d(TAG, "Successfully added favorite to Firestore: " + dormitoryId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error adding favorite: " + e.getMessage());
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error toggling favorite: " + e.getMessage());
            });
    }

    @Override
    public int getItemCount() {
        return dormitories != null ? dormitories.size() : 0;
    }

    public void updateDormitories(List<Dormitory> newDormitories) {
        this.dormitories = newDormitories;
        notifyDataSetChanged();
    }

    static class AllDormViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDormImage, ivPlaceholder, ivFavorite;
        TextView tvDormName, tvLocation, tvPrice, tvRating, tvAvailableRooms, tvAmenities;

        public AllDormViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDormImage = itemView.findViewById(R.id.ivDormImage);
            ivPlaceholder = itemView.findViewById(R.id.ivPlaceholder);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvDormName = itemView.findViewById(R.id.tvDormName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvAvailableRooms = itemView.findViewById(R.id.tvAvailableRooms);
            tvAmenities = itemView.findViewById(R.id.tvAmenities);
        }
    }
}
