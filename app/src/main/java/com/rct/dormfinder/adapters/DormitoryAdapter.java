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
import com.google.firebase.auth.FirebaseAuth;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.activities.DormitoryDetailActivity;
import com.rct.dormfinder.models.Dormitory;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DormitoryAdapter extends RecyclerView.Adapter<DormitoryAdapter.DormitoryViewHolder> {
    private static final String TAG = "DormitoryAdapter";
    private List<Dormitory> dormitories;
    private Context context;
    private NumberFormat currencyFormat;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public DormitoryAdapter(List<Dormitory> dormitories, Context context) {
        this.dormitories = dormitories;
        this.context = context;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public DormitoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dormitory, parent, false);
        return new DormitoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DormitoryViewHolder holder, int position) {
        Dormitory dormitory = dormitories.get(position);

        holder.tvName.setText(dormitory.getName());
        holder.tvAddress.setText(dormitory.getAddress());
        holder.tvPrice.setText(currencyFormat.format(dormitory.getMonthlyPrice()));
        holder.tvAvailableRooms.setText(dormitory.getAvailableRooms() + " rooms available");

        // Load first image if available
        if (dormitory.getImages() != null && !dormitory.getImages().isEmpty()) {
            String imagePath = dormitory.getImages().get(0);
            Log.d(TAG, "Loading image for " + dormitory.getName() + ": " + imagePath);
            
            Glide.with(context)
                .load(imagePath)
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
            if (dormitory.getAmenities().size() > 3) {
                amenitiesText.append(" • +").append(dormitory.getAmenities().size() - 3).append(" more");
            }
            holder.tvAmenities.setText(amenitiesText.toString());
        } else {
            holder.tvAmenities.setText("No amenities listed");
        }

        // Check if dormitory is favorited
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            checkIfFavorited(dormitory.getDormId(), currentUser.getUid(), holder.ivFavorite);
        }

        // Favorite button click listener
        holder.ivFavorite.setOnClickListener(v -> {
            if (currentUser != null) {
                toggleFavorite(dormitory.getDormId(), currentUser.getUid(), holder.ivFavorite);
            } else {
                Toast.makeText(context, "Please login to add favorites", Toast.LENGTH_SHORT).show();
            }
        });

        // Set click listener to open detail activity
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

    private void checkIfFavorited(String dormId, String userId, ImageView ivFavorite) {
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(dormId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    ivFavorite.setImageResource(R.drawable.ic_favorite_filled);
                    Log.d(TAG, "Dormitory " + dormId + " is favorited");
                } else {
                    ivFavorite.setImageResource(R.drawable.ic_favorite_border);
                    Log.d(TAG, "Dormitory " + dormId + " is NOT favorited");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking favorite status for " + dormId, e);
                ivFavorite.setImageResource(R.drawable.ic_favorite_border);
            });
    }

    private void toggleFavorite(String dormId, String userId, ImageView ivFavorite) {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.isAnonymous()) {
            Toast.makeText(context, "Sign in to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "toggleFavorite called for dormId: " + dormId + ", userId: " + userId);
        
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(dormId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Log.d(TAG, "Removing favorite for dormId: " + dormId);
                    // Remove from favorites
                    db.collection("users")
                        .document(userId)
                        .collection("favorites")
                        .document(dormId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            ivFavorite.setImageResource(R.drawable.ic_favorite_border);
                            Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Successfully removed favorite: " + dormId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error removing favorite: " + dormId, e);
                            Toast.makeText(context, "Failed to remove favorite", Toast.LENGTH_SHORT).show();
                        });
                } else {
                    Log.d(TAG, "Adding favorite for dormId: " + dormId);
                    // Add to favorites
                    Map<String, Object> favoriteData = new HashMap<>();
                    favoriteData.put("dormId", dormId);
                    favoriteData.put("timestamp", System.currentTimeMillis());

                    db.collection("users")
                        .document(userId)
                        .collection("favorites")
                        .document(dormId)
                        .set(favoriteData)
                        .addOnSuccessListener(aVoid -> {
                            ivFavorite.setImageResource(R.drawable.ic_favorite_filled);
                            Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Successfully added favorite: " + dormId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error adding favorite: " + dormId, e);
                            Toast.makeText(context, "Failed to add favorite", Toast.LENGTH_SHORT).show();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking favorite status for toggle: " + dormId, e);
                Toast.makeText(context, "Error checking favorite status", Toast.LENGTH_SHORT).show();
            });
    }

    static class DormitoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, ivFavorite;
        TextView tvName, tvAddress, tvPrice, tvAvailableRooms, tvAmenities;
        TextView tvRating;

        public DormitoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivDormImage);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvName = itemView.findViewById(R.id.tvDormName);
            tvAddress = itemView.findViewById(R.id.tvDormAddress);
            tvPrice = itemView.findViewById(R.id.tvDormPrice);
            tvAvailableRooms = itemView.findViewById(R.id.tvAvailableRooms);
            tvAmenities = itemView.findViewById(R.id.tvAmenities);
            tvRating = itemView.findViewById(R.id.tvRating);
        }
    }
}
