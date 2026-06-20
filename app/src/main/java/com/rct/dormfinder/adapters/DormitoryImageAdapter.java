package com.rct.dormfinder.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rct.dormfinder.R;
import com.rct.dormfinder.activities.FullscreenImageActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DormitoryImageAdapter extends RecyclerView.Adapter<DormitoryImageAdapter.ImageViewHolder> {
    private List<String> imagePaths;
    private Context context;

    public DormitoryImageAdapter(List<String> imagePaths, Context context) {
        this.imagePaths = imagePaths;
        this.context = context;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dormitory_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imagePath = imagePaths.get(position);
        
        // Check if it's a local file path or URL
        if (imagePath.startsWith("/") || imagePath.startsWith("file://")) {
            // Local file path
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Glide.with(context)
                        .load(imageFile)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_dorm)
                        .error(R.drawable.placeholder_dorm)
                        .into(holder.ivImage);
            } else {
                // File doesn't exist, show placeholder
                holder.ivImage.setImageResource(R.drawable.placeholder_dorm);
            }
        } else {
            // URL (Cloudinary or other)
            Glide.with(context)
                    .load(imagePath)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_dorm)
                    .error(R.drawable.placeholder_dorm)
                    .into(holder.ivImage);
        }

        // Add click listener to open fullscreen viewer
        holder.ivImage.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullscreenImageActivity.class);
            intent.putStringArrayListExtra("image_urls", new ArrayList<>(imagePaths));
            intent.putExtra("position", position);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return imagePaths != null ? imagePaths.size() : 0;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivDormImage);
        }
    }
}
