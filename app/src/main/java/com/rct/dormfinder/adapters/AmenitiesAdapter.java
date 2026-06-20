package com.rct.dormfinder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rct.dormfinder.R;
import java.util.List;

public class AmenitiesAdapter extends RecyclerView.Adapter<AmenitiesAdapter.AmenityViewHolder> {
    private List<String> amenities;

    public AmenitiesAdapter(List<String> amenities) {
        this.amenities = amenities;
    }

    @NonNull
    @Override
    public AmenityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_amenity, parent, false);
        return new AmenityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AmenityViewHolder holder, int position) {
        String amenity = amenities.get(position);
        holder.tvAmenity.setText(amenity);
    }

    @Override
    public int getItemCount() {
        return amenities.size();
    }

    static class AmenityViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmenity;

        public AmenityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmenity = itemView.findViewById(R.id.tvAmenity);
        }
    }
}