package com.rct.dormfinder.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Chat;
import com.rct.dormfinder.models.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    private List<Chat> chatList;
    private Context context;
    private OnChatClickListener listener;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private FirebaseFirestore db;
    private String currentUserId;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);

        void onDeleteChat(Chat chat);
    }

    public ChatListAdapter(List<Chat> chatList, Context context, OnChatClickListener listener) {
        this.chatList = chatList;
        this.context = context;
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        // Set dormitory name
        holder.tvDormitoryName.setText(chat.getDormitoryName());

        // Set last message
        String lastMessage = chat.getLastMessage();
        if (lastMessage != null && !lastMessage.isEmpty()) {
            if (lastMessage.length() > 50) {
                lastMessage = lastMessage.substring(0, 50) + "...";
            }
            holder.tvLastMessage.setText(lastMessage);
        } else {
            holder.tvLastMessage.setText("Start a conversation");
        }

        // Set timestamp
        if (chat.getLastMessageTimestamp() > 0) {
            String timeText = formatTimestamp(chat.getLastMessageTimestamp());
            holder.tvTimestamp.setText(timeText);
        } else {
            holder.tvTimestamp.setText("");
        }

        // ✅ FIX: Use the chat object's CURRENT unread count, with defensive checks
        int unreadCount = 0;
        try {
            unreadCount = chat.getUnreadCountForUser(currentUserId);
            
            android.util.Log.d("ChatListAdapter", "========== BINDING CHAT ==========");
            android.util.Log.d("ChatListAdapter", "Position: " + position);
            android.util.Log.d("ChatListAdapter", "Chat: " + chat.getDormitoryName());
            android.util.Log.d("ChatListAdapter", "Current User ID: " + currentUserId);
            android.util.Log.d("ChatListAdapter", "Student ID: " + chat.getStudentId());
            android.util.Log.d("ChatListAdapter", "Landlord ID: " + chat.getLandlordId());
            android.util.Log.d("ChatListAdapter", "Student Unread Count: " + chat.getStudentUnreadCount());
            android.util.Log.d("ChatListAdapter", "Landlord Unread Count: " + chat.getLandlordUnreadCount());
            android.util.Log.d("ChatListAdapter", "Calculated Unread for Current User: " + unreadCount);
            
            // ✅ FORCE CHECK: Is current user the student?
            if (currentUserId.equals(chat.getStudentId())) {
                android.util.Log.d("ChatListAdapter", "🎓 Current user IS the STUDENT");
                unreadCount = chat.getStudentUnreadCount();  // Force use studentUnreadCount
            } else if (currentUserId.equals(chat.getLandlordId())) {
                android.util.Log.d("ChatListAdapter", "🏠 Current user IS the LANDLORD");
                unreadCount = chat.getLandlordUnreadCount();  // Force use landlordUnreadCount
            } else {
                android.util.Log.e("ChatListAdapter", "❌ Current user is NEITHER student NOR landlord!");
            }
            
            android.util.Log.d("ChatListAdapter", "FINAL unreadCount: " + unreadCount);
            android.util.Log.d("ChatListAdapter", "==================================");
            
        } catch (Exception e) {
            android.util.Log.e("ChatListAdapter", "❌ Error getting unread count: " + e.getMessage());
            unreadCount = 0;
        }

        // ✅ UPDATE UI BASED ON UNREAD COUNT - ENHANCED HIGHLIGHTING
        if (unreadCount > 0) {
            // Show unread indicators
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(unreadCount));
            holder.viewUnreadRing.setVisibility(View.VISIBLE);

            // Apply unread background - FORCE SET WITH PADDING
            holder.chatItemContainer.setBackgroundResource(R.drawable.unread_chat_background);
            // Ensure padding is preserved
            int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
            holder.chatItemContainer.setPadding(padding, padding, padding, padding);

            // Make last message bold and darker for unread
            holder.tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.tvLastMessage.setTextColor(context.getResources().getColor(R.color.black));
            
            // Make dormitory name bold too
            holder.tvDormitoryName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.tvDormitoryName.setTextColor(context.getResources().getColor(R.color.orange_primary));

            // Log that we're highlighting this chat
            android.util.Log.d("ChatListAdapter", "✅ HIGHLIGHTING CHAT AS UNREAD: " + chat.getDormitoryName() + " (Count: " + unreadCount + ")");
        } else {
            // Hide unread indicators
            holder.tvUnreadCount.setVisibility(View.GONE);
            holder.viewUnreadRing.setVisibility(View.GONE);

            // White background for read messages
            holder.chatItemContainer.setBackgroundColor(context.getResources().getColor(android.R.color.white));
            // Ensure padding is preserved
            int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
            holder.chatItemContainer.setPadding(padding, padding, padding, padding);

            // Normal text for read messages
            holder.tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.tvLastMessage.setTextColor(context.getResources().getColor(R.color.gray_text));
            
            // Normal dormitory name
            holder.tvDormitoryName.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.tvDormitoryName.setTextColor(context.getResources().getColor(R.color.orange_primary));

            android.util.Log.d("ChatListAdapter", "Chat shown as read: " + chat.getDormitoryName());
        }

        // Load other participant info
        String otherParticipantId = chat.getOtherParticipantId(currentUserId);
        loadParticipantInfo(holder, otherParticipantId);

        // Set click listener
        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));

        // Set long click listener for delete option
        holder.itemView.setOnLongClickListener(v -> {
            listener.onDeleteChat(chat);
            return true;
        });
    }

    private void loadParticipantInfo(ChatViewHolder holder, String participantId) {
        db.collection("users").document(participantId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User participant = document.toObject(User.class);
                        if (participant != null) {
                            holder.tvParticipantName.setText(participant.getName());

                            // Load profile image
                            loadParticipantImage(holder.ivProfileImage, participant.getProfileImageUrl());

                            // Show user type indicator
                            if ("student".equals(participant.getUserType())) {
                                holder.tvUserType.setText("Student");
                                holder.tvUserType.setBackgroundResource(R.drawable.student_tag_background);
                            } else if ("landlord".equals(participant.getUserType())) {
                                holder.tvUserType.setText("Landlord");
                                holder.tvUserType.setBackgroundResource(R.drawable.landlord_tag_background);
                            }
                        }
                    } else {
                        holder.tvParticipantName.setText("User");
                        holder.ivProfileImage.setImageResource(R.drawable.ic_person);
                        holder.tvUserType.setText("");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.w("ChatListAdapter", "Failed to load participant info: " + e.getMessage());
                    holder.tvParticipantName.setText("User");
                    holder.ivProfileImage.setImageResource(R.drawable.ic_person);
                    holder.tvUserType.setText("");
                });
    }

    private void loadParticipantImage(ImageView imageView, String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            if (imagePath.startsWith("/") || imagePath.startsWith("file://")) {
                // Local file path
                java.io.File imageFile = new java.io.File(imagePath);
                if (imageFile.exists()) {
                    Glide.with(context)
                            .load(imageFile)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(imageView);
                } else {
                    imageView.setImageResource(R.drawable.ic_person);
                }
            } else {
                // URL
                Glide.with(context)
                        .load(imagePath)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(imageView);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_person);
        }
    }

    private String formatTimestamp(long timestamp) {
        Date messageDate = new Date(timestamp);
        Date today = new Date();

        // Check if message is from today
        if (isSameDay(messageDate, today)) {
            return timeFormat.format(messageDate);
        } else {
            return dateFormat.format(messageDate);
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return dayFormat.format(date1).equals(dayFormat.format(date2));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public void updateChats(List<Chat> newChatList) {
        android.util.Log.d("ChatListAdapter", "========== UPDATE CHATS CALLED ==========");
        android.util.Log.d("ChatListAdapter", "Received " + newChatList.size() + " chats");

        // Log each chat's unread count
        for (int i = 0; i < newChatList.size(); i++) {
            Chat chat = newChatList.get(i);
            android.util.Log.d("ChatListAdapter", "Chat " + (i + 1) + ": " + chat.getDormitoryName());
            android.util.Log.d("ChatListAdapter", "   Student Unread: " + chat.getStudentUnreadCount());
            android.util.Log.d("ChatListAdapter", "   Landlord Unread: " + chat.getLandlordUnreadCount());
        }

        this.chatList = newChatList;
        notifyDataSetChanged();
        android.util.Log.d("ChatListAdapter", "=========================================");
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        View chatItemContainer;
        View viewUnreadRing;
        ImageView ivProfileImage;
        TextView tvParticipantName, tvUserType, tvDormitoryName, tvLastMessage;
        TextView tvTimestamp, tvUnreadCount;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatItemContainer = itemView.findViewById(R.id.chatItemContainer);
            viewUnreadRing = itemView.findViewById(R.id.viewUnreadRing);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            tvParticipantName = itemView.findViewById(R.id.tvParticipantName);
            tvUserType = itemView.findViewById(R.id.tvUserType);
            tvDormitoryName = itemView.findViewById(R.id.tvDormitoryName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}
