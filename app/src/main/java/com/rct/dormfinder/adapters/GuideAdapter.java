package com.rct.dormfinder.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.GuideItem;
import java.util.List;

public class GuideAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<GuideItem> guideItems;
    private Context context;

    public GuideAdapter(List<GuideItem> guideItems, Context context) {
        this.guideItems = guideItems;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return guideItems.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        
        switch (viewType) {
            case GuideItem.TYPE_HEADER:
                View headerView = inflater.inflate(R.layout.item_guide_header, parent, false);
                return new HeaderViewHolder(headerView);
            case GuideItem.TYPE_SECTION:
                View sectionView = inflater.inflate(R.layout.item_guide_section, parent, false);
                return new SectionViewHolder(sectionView);
            case GuideItem.TYPE_STEP:
            default:
                View stepView = inflater.inflate(R.layout.item_guide_step, parent, false);
                return new StepViewHolder(stepView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GuideItem item = guideItems.get(position);
        
        switch (holder.getItemViewType()) {
            case GuideItem.TYPE_HEADER:
                HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
                headerHolder.tvTitle.setText(item.getTitle());
                headerHolder.tvContent.setText(item.getContent());
                break;
            case GuideItem.TYPE_SECTION:
                SectionViewHolder sectionHolder = (SectionViewHolder) holder;
                sectionHolder.tvTitle.setText(item.getTitle());
                break;
            case GuideItem.TYPE_STEP:
                StepViewHolder stepHolder = (StepViewHolder) holder;
                stepHolder.tvTitle.setText(item.getTitle());
                stepHolder.tvContent.setText(item.getContent());
                break;
        }
    }

    @Override
    public int getItemCount() {
        return guideItems.size();
    }

    // ViewHolder classes
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;

        public SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent;

        public StepViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }
}
