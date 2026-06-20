package com.rct.dormfinder.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.activities.AddDormitoryActivity;
import com.rct.dormfinder.activities.DormitoryDetailActivity;
import com.rct.dormfinder.models.Dormitory;
import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class LandlordDormitoryAdapter extends RecyclerView.Adapter<LandlordDormitoryAdapter.DormitoryViewHolder> {
    private List<Dormitory> dormitories;
    private Context context;
    private NumberFormat currencyFormat;
    private FirebaseFirestore db;

    public LandlordDormitoryAdapter(List<Dormitory> dormitories, Context context) {
        this.dormitories = dormitories;
        this.context = context;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public DormitoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_landlord_dormitory, parent, false);
        return new DormitoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DormitoryViewHolder holder, int position) {
        Dormitory dormitory = dormitories.get(position);

        holder.tvName.setText(dormitory.getName());
        holder.tvAddress.setText(dormitory.getAddress());
        holder.tvCity.setText(dormitory.getCity());
        holder.tvPrice.setText(currencyFormat.format(dormitory.getMonthlyPrice()));
        holder.tvRoomInfo.setText(dormitory.getAvailableRooms() + "/" + dormitory.getTotalRooms() + " rooms available");
        
        // Set availability status
        if (!dormitory.isAvailable()) {
            holder.tvStatus.setText("Inactive");
            holder.tvStatus.setTextColor(context.getColor(R.color.error));
        } else if (dormitory.getAvailableRooms() > 0) {
            holder.tvStatus.setText("Active");
            holder.tvStatus.setTextColor(context.getColor(R.color.orange_primary));
        } else {
            holder.tvStatus.setText("Full");
            holder.tvStatus.setTextColor(context.getColor(R.color.warning));
        }

        // Load first image if available
        if (dormitory.getImages() != null && !dormitory.getImages().isEmpty()) {
            String imagePath = dormitory.getImages().get(0);
            
            // Check if it's a local file path or URL
            if (imagePath.startsWith("/") || imagePath.startsWith("file://")) {
                // Local file path
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Glide.with(context)
                            .load(imageFile)
                            .placeholder(R.drawable.placeholder_dorm)
                            .error(R.drawable.placeholder_dorm)
                            .into(holder.ivImage);
                } else {
                    holder.ivImage.setImageResource(R.drawable.placeholder_dorm);
                }
            } else {
                // URL (for future Firebase Storage integration)
                Glide.with(context)
                        .load(imagePath)
                        .placeholder(R.drawable.placeholder_dorm)
                        .error(R.drawable.placeholder_dorm)
                        .into(holder.ivImage);
            }
        } else {
            holder.ivImage.setImageResource(R.drawable.placeholder_dorm);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DormitoryDetailActivity.class);
            intent.putExtra("dormitory_id", dormitory.getDormId());
            context.startActivity(intent);
        });

        holder.ivMenu.setOnClickListener(v -> showPopupMenu(v, dormitory, position));
    }

    private void showPopupMenu(View view, Dormitory dormitory, int position) {
        // Create PopupMenu with a context wrapper to ensure black text
        Context wrapper = new ContextThemeWrapper(context, R.style.PopupMenuStyle);
        PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.menu_dormitory_actions, popup.getMenu());
        
        // Force show icons
        try {
            java.lang.reflect.Field mPopup = popup.getClass().getDeclaredField("mPopup");
            mPopup.setAccessible(true);
            Object menuPopupHelper = mPopup.get(popup);
            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
            java.lang.reflect.Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
            setForceIcons.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit) {
                editDormitory(dormitory);
                return true;
            } else if (itemId == R.id.action_toggle_availability) {
                toggleAvailability(dormitory, position);
                return true;
            } else if (itemId == R.id.action_delete) {
                showDeleteConfirmation(dormitory, position);
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void editDormitory(Dormitory dormitory) {
        Intent intent = new Intent(context, AddDormitoryActivity.class);
        intent.putExtra("dormitory_id", dormitory.getDormId());
        context.startActivity(intent);
    }

    private void toggleAvailability(Dormitory dormitory, int position) {
        boolean newAvailability = !dormitory.isAvailable();
        
        android.util.Log.d("LandlordAdapter", "Toggling availability for " + dormitory.getName() + 
                " from " + dormitory.isAvailable() + " to " + newAvailability);
        
        // Update local state immediately for better UX
        dormitory.setAvailable(newAvailability);
        notifyItemChanged(position);
        
        // Try both field names to ensure compatibility
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("available", newAvailability);  // Standard Java Bean naming
        updates.put("isAvailable", newAvailability); // PropertyName annotation
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        
        // Update Firestore with both field names
        db.collection("dormitories").document(dormitory.getDormId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("LandlordAdapter", "Successfully updated availability in Firestore");
                    String message = newAvailability ? "Dormitory is now active" : "Dormitory is now inactive";
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LandlordAdapter", "Failed to update: " + e.getMessage());
                    // Revert local change on failure
                    dormitory.setAvailable(!newAvailability);
                    notifyItemChanged(position);
                    Toast.makeText(context, "Failed to update availability: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmation(Dormitory dormitory, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Dormitory")
                .setMessage("Are you sure you want to delete \"" + dormitory.getName() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteDormitory(dormitory, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDormitory(Dormitory dormitory, int position) {
        db.collection("dormitories").document(dormitory.getDormId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    dormitories.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Dormitory deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete dormitory: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return dormitories.size();
    }

    static class DormitoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, ivMenu;
        TextView tvName, tvAddress, tvCity, tvPrice, tvRoomInfo, tvStatus;

        public DormitoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivDormImage);
            ivMenu = itemView.findViewById(R.id.ivMenu);
            tvName = itemView.findViewById(R.id.tvDormName);
            tvAddress = itemView.findViewById(R.id.tvDormAddress);
            tvCity = itemView.findViewById(R.id.tvDormCity);
            tvPrice = itemView.findViewById(R.id.tvDormPrice);
            tvRoomInfo = itemView.findViewById(R.id.tvRoomInfo);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
