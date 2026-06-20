package com.rct.dormfinder.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Message;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private static final int TYPE_DATE_SEPARATOR = 3;

    private List<Message> messages;
    private Context context;
    private String currentUserId;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;

    public MessageAdapter(List<Message> messages, Context context) {
        this.messages = messages;
        this.context = context;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        
        // Check if we need a date separator
        if (position == 0 || !isSameDay(message.getTimestamp(), messages.get(position - 1).getTimestamp())) {
            // For now, we'll just use message types. Date separators can be added later
        }
        
        if (message.getSenderId().equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        
        if (holder instanceof SentMessageViewHolder) {
            bindSentMessage((SentMessageViewHolder) holder, message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            bindReceivedMessage((ReceivedMessageViewHolder) holder, message);
        }
    }

    private void bindSentMessage(SentMessageViewHolder holder, Message message) {
        holder.tvMessage.setText(message.getContent());
        holder.tvTimestamp.setText(timeFormat.format(new Date(message.getTimestamp())));
        
        // Show delivery status
        if (message.getIsRead()) {
            holder.tvStatus.setText("Read");
            holder.tvStatus.setTextColor(context.getColor(R.color.orange_primary));
        } else {
            holder.tvStatus.setText("Sent");
            holder.tvStatus.setTextColor(context.getColor(R.color.gray_text));
        }
    }

    private void bindReceivedMessage(ReceivedMessageViewHolder holder, Message message) {
        holder.tvMessage.setText(message.getContent());
        holder.tvTimestamp.setText(timeFormat.format(new Date(message.getTimestamp())));
    }

    private boolean isSameDay(long timestamp1, long timestamp2) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return dayFormat.format(new Date(timestamp1)).equals(dayFormat.format(new Date(timestamp2)));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp, tvStatus;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
