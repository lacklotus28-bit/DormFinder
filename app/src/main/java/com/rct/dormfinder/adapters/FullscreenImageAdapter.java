package com.rct.dormfinder.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.rct.dormfinder.R;
import java.util.List;

public class FullscreenImageAdapter extends RecyclerView.Adapter<FullscreenImageAdapter.ImageViewHolder> {
    private List<String> imageUrls;
    private Context context;

    public FullscreenImageAdapter(List<String> imageUrls, Context context) {
        this.imageUrls = imageUrls;
        this.context = context;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fullscreen_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_dorm)
                .error(R.drawable.placeholder_dorm)
                .into(holder.photoView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }
}
