package com.rct.dormfinder.utils;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rct.dormfinder.models.Chat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to clean up duplicate chat conversations in Firestore
 * This should be run once to fix existing duplicates
 */
public class ChatDuplicateCleanup {
    private static final String TAG = "ChatDuplicateCleanup";
    
    public interface CleanupCallback {
        void onComplete(int duplicatesRemoved, int chatsRetained);
        void onError(String error);
    }
    
    /**
     * Scan all chats and remove duplicates, keeping only the most recent conversation
     */
    public static void cleanupDuplicateChats(CleanupCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("chats")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Map to track unique conversations
                    // Key: studentId_landlordId_dormitoryId
                    // Value: List of Chat objects with same key
                    Map<String, List<ChatInfo>> conversationGroups = new HashMap<>();
                    
                    // Group all chats by their unique key
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Chat chat = document.toObject(Chat.class);
                        if (chat != null) {
                            chat.setChatId(document.getId());
                            
                            String key = generateChatKey(
                                chat.getStudentId(),
                                chat.getLandlordId(),
                                chat.getDormitoryId()
                            );
                            
                            if (!conversationGroups.containsKey(key)) {
                                conversationGroups.put(key, new ArrayList<>());
                            }
                            
                            conversationGroups.get(key).add(new ChatInfo(
                                chat.getChatId(),
                                chat.getLastMessageTimestamp()
                            ));
                        }
                    }
                    
                    // Find and delete duplicates
                    int duplicatesRemoved = 0;
                    int chatsRetained = conversationGroups.size();
                    
                    for (Map.Entry<String, List<ChatInfo>> entry : conversationGroups.entrySet()) {
                        List<ChatInfo> chats = entry.getValue();
                        
                        if (chats.size() > 1) {
                            // Found duplicates! Keep the one with most recent timestamp
                            Log.d(TAG, "Found " + chats.size() + " duplicates for key: " + entry.getKey());
                            
                            // Sort by timestamp descending
                            chats.sort((c1, c2) -> Long.compare(c2.lastMessageTimestamp, c1.lastMessageTimestamp));
                            
                            // Keep the first one (most recent), delete the rest
                            for (int i = 1; i < chats.size(); i++) {
                                String chatIdToDelete = chats.get(i).chatId;
                                Log.d(TAG, "Deleting duplicate chat: " + chatIdToDelete);
                                
                                db.collection("chats")
                                        .document(chatIdToDelete)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Successfully deleted chat: " + chatIdToDelete);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to delete chat: " + chatIdToDelete, e);
                                        });
                                
                                duplicatesRemoved++;
                            }
                        }
                    }
                    
                    final int finalDuplicatesRemoved = duplicatesRemoved;
                    final int finalChatsRetained = chatsRetained;
                    
                    Log.d(TAG, "Cleanup complete. Removed: " + duplicatesRemoved + ", Retained: " + chatsRetained);
                    
                    if (callback != null) {
                        callback.onComplete(finalDuplicatesRemoved, finalChatsRetained);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load chats for cleanup", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }
    
    /**
     * Generate consistent key for a conversation
     */
    private static String generateChatKey(String studentId, String landlordId, String dormitoryId) {
        return studentId + "_" + landlordId + "_" + (dormitoryId != null ? dormitoryId : "unknown");
    }
    
    /**
     * Helper class to store chat info during cleanup
     */
    private static class ChatInfo {
        String chatId;
        long lastMessageTimestamp;
        
        ChatInfo(String chatId, long lastMessageTimestamp) {
            this.chatId = chatId;
            this.lastMessageTimestamp = lastMessageTimestamp;
        }
    }
}
