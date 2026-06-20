package com.rct.dormfinder.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rct.dormfinder.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageUploadAdapter extends RecyclerView.Adapter<ImageUploadAdapter.ImageViewHolder> {
    private List<Object> images; // Can be Uri (new images) or String (existing URLs)
    private Context context;
    private OnImageRemoveListener removeListener;
    private OnImageReorderListener reorderListener;

    public interface OnImageRemoveListener {
        void onImageRemove(int position);
    }

    public interface OnImageReorderListener {
        void onImageReorder();
    }

    public ImageUploadAdapter(List<Object> images, Context context) {
        this.images = images;
        this.context = context;
    }

    // Method to add new images (Uri)
    public void addNewImages(List<Uri> newImages) {
        int startPosition = images.size();
        images.addAll(newImages);
        notifyItemRangeInserted(startPosition, newImages.size());
    }

    // Method to set existing images (String URLs)
    public void setExistingImages(List<String> existingUrls) {
        images.clear();
        images.addAll(existingUrls);
        notifyDataSetChanged();
    }

    // Get list of new images only (Uri)
    public List<Uri> getNewImages() {
        List<Uri> newImages = new ArrayList<>();
        for (Object img : images) {
            if (img instanceof Uri) {
                newImages.add((Uri) img);
            }
        }
        return newImages;
    }

    // Get list of existing image URLs only (String)
    public List<String> getExistingImageUrls() {
        List<String> existingUrls = new ArrayList<>();
        for (Object img : images) {
            if (img instanceof String) {
                existingUrls.add((String) img);
            }
        }
        return existingUrls;
    }

    public void setOnImageRemoveListener(OnImageRemoveListener listener) {
        this.removeListener = listener;
    }

    public void setOnImageReorderListener(OnImageReorderListener listener) {
        this.reorderListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_upload, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Object image = images.get(position);
        
        // Show position number
        if (holder.tvPosition != null) {
            holder.tvPosition.setText(String.valueOf(position + 1));
            // Highlight first image as cover
            if (position == 0) {
                holder.tvPosition.setBackgroundResource(R.drawable.circle_background);
                holder.tvPosition.setTextColor(context.getColor(R.color.white));
            } else {
                holder.tvPosition.setBackgroundColor(context.getColor(android.R.color.transparent));
                holder.tvPosition.setTextColor(context.getColor(R.color.gray_text));
            }
        }
        
        // Load image whether it's a Uri (new) or String URL (existing)
        Glide.with(context)
                .load(image) // Glide handles both Uri and String
                .centerCrop()
                .placeholder(R.drawable.placeholder_dorm)
                .error(R.drawable.placeholder_dorm)
                .into(holder.ivImage);

        holder.ivRemove.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                images.remove(adapterPosition);
                notifyItemRemoved(adapterPosition);
                notifyItemRangeChanged(adapterPosition, images.size());
                
                if (removeListener != null) {
                    removeListener.onImageRemove(adapterPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    // Drag and drop methods
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(images, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(images, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        if (reorderListener != null) {
            reorderListener.onImageReorder();
        }
    }

    public void onItemDismiss(int position) {
        images.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, images.size());
        if (removeListener != null) {
            removeListener.onImageRemove(position);
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, ivRemove;
        TextView tvPosition;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            ivRemove = itemView.findViewById(R.id.ivRemove);
            tvPosition = itemView.findViewById(R.id.tvPosition);
        }
    }
}
