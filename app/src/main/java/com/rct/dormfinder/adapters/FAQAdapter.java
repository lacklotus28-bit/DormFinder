package com.rct.dormfinder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.FAQItem;
import java.util.List;

public class FAQAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<FAQItem> faqItems;

    public FAQAdapter(List<FAQItem> faqItems) {
        this.faqItems = faqItems;
    }

    @Override
    public int getItemViewType(int position) {
        return faqItems.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == FAQItem.TYPE_CATEGORY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_faq_category, parent, false);
            return new CategoryViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_faq_question, parent, false);
            return new QuestionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FAQItem item = faqItems.get(position);
        
        if (holder instanceof CategoryViewHolder) {
            ((CategoryViewHolder) holder).bind(item);
        } else if (holder instanceof QuestionViewHolder) {
            ((QuestionViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return faqItems.size();
    }

    // ViewHolder for category headers
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }

        void bind(FAQItem item) {
            tvCategory.setText(item.getQuestion());
        }
    }

    // ViewHolder for FAQ questions
    class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvAnswer;
        ImageView ivExpand;
        View divider;

        QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            divider = itemView.findViewById(R.id.divider);
        }

        void bind(FAQItem item) {
            tvQuestion.setText(item.getQuestion());
            tvAnswer.setText(item.getAnswer());
            
            // Set initial visibility based on expanded state
            tvAnswer.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
            ivExpand.setRotation(item.isExpanded() ? 180 : 0);
            
            // Toggle expand/collapse on click
            itemView.setOnClickListener(v -> {
                boolean isExpanded = item.isExpanded();
                item.setExpanded(!isExpanded);
                
                // Animate the expand/collapse
                if (item.isExpanded()) {
                    tvAnswer.setVisibility(View.VISIBLE);
                    ivExpand.animate().rotation(180).setDuration(200).start();
                } else {
                    tvAnswer.setVisibility(View.GONE);
                    ivExpand.animate().rotation(0).setDuration(200).start();
                }
            });
        }
    }
}
