package com.rct.dormfinder.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.rct.dormfinder.models.Message;
import com.rct.dormfinder.utils.NotificationHelper;

public class MessageNotificationService extends Service {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration messageListener;
    private NotificationHelper notificationHelper;
    private String currentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        notificationHelper = new NotificationHelper(this);
        
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            startListeningForMessages();
        }
    }

    private void startListeningForMessages() {
        // Listen for new messages across all chats where user is a participant
        messageListener = db.collectionGroup("messages")
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("MessageService", "Failed to listen for messages: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message message = dc.getDocument().toObject(Message.class);
                                message.setMessageId(dc.getDocument().getId());
                                
                                // Only show notification for new unread messages
                                if (!message.getIsRead() && !message.getSenderId().equals(currentUserId)) {
                                    showMessageNotification(message);
                                }
                            }
                        }
                    }
                });
    }

    private void showMessageNotification(Message message) {
        // Get sender name from users collection
        db.collection("users").document(message.getSenderId())
                .get()
                .addOnSuccessListener(document -> {
                    String senderName = "New Message";
                    if (document.exists()) {
                        com.rct.dormfinder.models.User sender = document.toObject(com.rct.dormfinder.models.User.class);
                        if (sender != null) {
                            senderName = sender.getName();
                        }
                    }
                    
                    String content = message.getContent();
                    if (content.length() > 50) {
                        content = content.substring(0, 50) + "...";
                    }
                    
                    notificationHelper.notifyNewMessage(
                            currentUserId,
                            senderName,
                            message.getChatId()
                    );
                })
                .addOnFailureListener(e -> {
                    // Fallback notification
                    notificationHelper.notifyNewMessage(
                            currentUserId,
                            "New Message",
                            message.getChatId()
                    );
                });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Restart service if it gets killed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
