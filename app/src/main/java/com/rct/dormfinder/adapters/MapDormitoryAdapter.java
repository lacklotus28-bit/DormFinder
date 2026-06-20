package com.rct.dormfinder.adapters;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.utils.FavoritesManager;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying dormitories in the map screen with distance and GPS navigation.
 * Matches the SearchFilter grid design for consistency.
 * 
 * NEW Features:
 * - Shows distance from user's current location
 * - "Get Directions" button opens Google Maps navigation
 * - Real-time distance calculation
 * 
 * @author DormFinder Team (Enhanced by Jarvis AI)
 * @version 4.0 - Added GPS navigation and distance display
 */
public class MapDormitoryAdapter extends RecyclerView.Adapter<MapDormitoryAdapter.MapDormViewHolder> {
    private static final String TAG = "MapDormitoryAdapter";
    private List<Dormitory> dormitories;
    private Context context;
    private OnDormitoryClickListener listener;
    private FavoritesManager favoritesManager;
    private Location userLocation; // Store user's current location

    /**
     * Interface for handling dormitory item clicks
     */
    public interface OnDormitoryClickListener {
        void onDormitoryClick(Dormitory dormitory);
    }

    /**
     * Constructor for MapDormitoryAdapter
     * @param dormitories List of dormitories to display
     * @param context Android context
     * @param listener Click listener for items
     */
    public MapDormitoryAdapter(List<Dormitory> dormitories, Context context, OnDormitoryClickListener listener) {
        this.dormitories = dormitories;
        this.context = context;
        this.listener = listener;
        this.favoritesManager = new FavoritesManager(context);
    }

    /**
     * Update user's location for distance calculations
     * @param location User's current location
     */
    public void setUserLocation(Location location) {
        this.userLocation = location;
        notifyDataSetChanged(); // Refresh to show distances
        Log.d(TAG, "User location updated: " + (location != null ? 
            "(" + location.getLatitude() + ", " + location.getLongitude() + ")" : "null"));
    }

    @NonNull
    @Override
    public MapDormViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_map_dormitory, parent, false);
        return new MapDormViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MapDormViewHolder holder, int position) {
        Dormitory dormitory = dormitories.get(position);

        // Set text fields
        holder.tvName.setText(dormitory.getName());
        holder.tvPrice.setText("₱" + String.format("%,.2f", dormitory.getMonthlyPrice()));
        holder.tvLocation.setText(dormitory.getAddress());
        
        // Set available rooms text
        int availableRooms = dormitory.getAvailableRooms();
        holder.tvRooms.setText(availableRooms + " room" + (availableRooms == 1 ? "" : "s") + " available");
        
        // Set rating - show badge even for 0 rating to maintain layout consistency
        double rating = dormitory.getAverageRating();
        if (rating > 0) {
            holder.tvRating.setText(String.format(Locale.ENGLISH, "%.1f", rating));
            holder.ratingBadge.setVisibility(View.VISIBLE);
            Log.d(TAG, dormitory.getName() + " rating: " + rating);
        } else {
            // Show N/A for dormitories without ratings
            holder.tvRating.setText("N/A");
            holder.ratingBadge.setVisibility(View.VISIBLE);
            Log.d(TAG, dormitory.getName() + " has no rating yet");
        }

        // Calculate and display distance
        if (userLocation != null) {
            double distance = calculateDistance(
                userLocation.getLatitude(),
                userLocation.getLongitude(),
                dormitory.getLatitude(),
                dormitory.getLongitude()
            );
            
            holder.distanceBadge.setVisibility(View.VISIBLE);
            holder.tvDistance.setText(formatDistance(distance));
            Log.d(TAG, dormitory.getName() + " is " + formatDistance(distance) + " away");
        } else {
            holder.distanceBadge.setVisibility(View.GONE);
            Log.d(TAG, "No user location available for distance calculation");
        }

        // Set amenities (top 3)
        if (dormitory.getAmenities() != null && !dormitory.getAmenities().isEmpty()) {
            List<String> amenities = dormitory.getAmenities();
            int count = Math.min(3, amenities.size());
            StringBuilder amenitiesText = new StringBuilder();
            
            for (int i = 0; i < count; i++) {
                amenitiesText.append(amenities.get(i));
                if (i < count - 1) {
                    amenitiesText.append(" • ");
                }
            }
            
            holder.tvAmenities.setText(amenitiesText.toString());
            holder.tvAmenities.setVisibility(View.VISIBLE);
        } else {
            holder.tvAmenities.setVisibility(View.GONE);
        }

        // Set favorite icon state
        boolean isFavorite = favoritesManager.isFavorite(dormitory.getDormId());
        updateFavoriteIcon(holder.ivFavorite, isFavorite);

        // Load image with proper error handling
        if (dormitory.getImages() != null && !dormitory.getImages().isEmpty()) {
            String imageUrl = dormitory.getImages().get(0);
            Log.d(TAG, "Loading image for " + dormitory.getName() + ": " + imageUrl);
            
            // Hide placeholder, show image
            holder.ivPlaceholder.setVisibility(View.GONE);
            holder.ivImage.setVisibility(View.VISIBLE);
            
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_dorm)
                .error(R.drawable.placeholder_dorm)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.ivImage);
        } else {
            Log.d(TAG, "No images for " + dormitory.getName());
            // Show placeholder, hide actual image
            holder.ivPlaceholder.setVisibility(View.VISIBLE);
            holder.ivImage.setVisibility(View.VISIBLE);
            holder.ivImage.setImageResource(R.drawable.placeholder_dorm);
        }

        // Set favorite button click listener
        holder.ivFavorite.setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.isAnonymous()) {
                Toast.makeText(context, "Sign in to manage favorites", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean currentlyFavorite = favoritesManager.isFavorite(dormitory.getDormId());
            
            if (currentlyFavorite) {
                favoritesManager.removeFavorite(dormitory.getDormId());
                updateFavoriteIcon(holder.ivFavorite, false);
                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
            } else {
                favoritesManager.addFavorite(dormitory.getDormId());
                updateFavoriteIcon(holder.ivFavorite, true);
                Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
            }
        });

        // Get Directions button click listener
        holder.btnGetDirections.setOnClickListener(v -> {
            openGoogleMapsNavigation(dormitory);
        });

        // Set click listener for card to view details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDormitoryClick(dormitory);
            }
        });
    }

    /**
     * Opens Google Maps with navigation to the dormitory
     * @param dormitory The dormitory to navigate to
     */
    private void openGoogleMapsNavigation(Dormitory dormitory) {
        try {
            // Create URI for Google Maps navigation
            String uri = String.format(Locale.ENGLISH, 
                "google.navigation:q=%f,%f&mode=d", 
                dormitory.getLatitude(), 
                dormitory.getLongitude());
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            
            // Check if Google Maps is installed
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Log.d(TAG, "Opening Google Maps navigation to: " + dormitory.getName());
            } else {
                // Fallback: Open in browser or show map location
                String fallbackUri = String.format(Locale.ENGLISH,
                    "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                    dormitory.getLatitude(),
                    dormitory.getLongitude());
                
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri));
                context.startActivity(browserIntent);
                Log.d(TAG, "Google Maps not installed, opening in browser");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening navigation: " + e.getMessage(), e);
            Toast.makeText(context, "Unable to open navigation", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Calculate distance between two points using Haversine formula
     * @param lat1 Starting latitude
     * @param lon1 Starting longitude
     * @param lat2 Destination latitude
     * @param lon2 Destination longitude
     * @return Distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in kilometers
    }

    /**
     * Format distance for display
     * @param distanceInKm Distance in kilometers
     * @return Formatted string (e.g., "2.3 km" or "850 m")
     */
    private String formatDistance(double distanceInKm) {
        if (distanceInKm < 1.0) {
            // Show in meters if less than 1 km
            int meters = (int) (distanceInKm * 1000);
            return meters + " m away";
        } else if (distanceInKm < 10.0) {
            // Show one decimal place for distances under 10 km
            return String.format(Locale.ENGLISH, "%.1f km away", distanceInKm);
        } else {
            // Show no decimal places for distances over 10 km
            return String.format(Locale.ENGLISH, "%.0f km away", distanceInKm);
        }
    }

    /**
     * Updates the favorite icon based on favorite status
     * @param imageView The favorite icon ImageView
     * @param isFavorite Whether the dormitory is favorited
     */
    private void updateFavoriteIcon(ImageView imageView, boolean isFavorite) {
        if (isFavorite) {
            imageView.setImageResource(R.drawable.ic_favorite_filled);
            // Add a subtle scale animation for feedback
            imageView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
                .withEndAction(() -> imageView.animate().scaleX(1f).scaleY(1f).setDuration(100).start());
        } else {
            imageView.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    @Override
    public int getItemCount() {
        return dormitories.size();
    }

    /**
     * Updates the list of dormitories and refreshes the display
     * @param newDormitories New list of dormitories
     */
    public void updateDormitories(List<Dormitory> newDormitories) {
        this.dormitories = newDormitories;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for dormitory items
     */
    static class MapDormViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, ivPlaceholder, ivFavorite;
        TextView tvName, tvPrice, tvLocation, tvRooms, tvRating, tvAmenities, tvDistance;
        LinearLayout ratingBadge, distanceBadge;
        MaterialButton btnGetDirections;

        public MapDormViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            ivPlaceholder = itemView.findViewById(R.id.ivPlaceholder);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvRooms = itemView.findViewById(R.id.tvRooms);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvAmenities = itemView.findViewById(R.id.tvAmenities);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            ratingBadge = itemView.findViewById(R.id.ratingBadge);
            distanceBadge = itemView.findViewById(R.id.distanceBadge);
            btnGetDirections = itemView.findViewById(R.id.btnGetDirections);
        }
    }
}
