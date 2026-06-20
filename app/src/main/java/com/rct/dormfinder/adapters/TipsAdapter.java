package com.rct.dormfinder.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rct.dormfinder.R;
import com.rct.dormfinder.models.TipItem;

import java.util.List;

public class TipsAdapter extends RecyclerView.Adapter<TipsAdapter.TipViewHolder> {
    private final List<TipItem> tips;
    private final Context context;
    private final OnTipClickListener listener;

    public interface OnTipClickListener {
        void onExploreGuide();
    }

    public TipsAdapter(Context context, List<TipItem> tips, OnTipClickListener listener) {
        this.context = context;
        this.tips = tips;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tip_card, parent, false);
        return new TipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TipViewHolder holder, int position) {
        TipItem item = tips.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvSubtitle.setText(item.getSubtitle());
        holder.ivIcon.setImageResource(item.getIconResId());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onExploreGuide();
        });
    }

    @Override
    public int getItemCount() {
        return tips.size();
    }

    static class TipViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;
        // Removed dedicated CTA binding; the whole card is clickable

        public TipViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }
    }
}
