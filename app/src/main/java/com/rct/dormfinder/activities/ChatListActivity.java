package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.ChatListAdapter;
import com.rct.dormfinder.models.Chat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends BaseActivity implements ChatListAdapter.OnChatClickListener {
    private static final String TAG = "ChatListActivity";
    
    private TextView tvTitle;
    private View tvEmptyState;
    private RecyclerView recyclerViewChats;
    private SwipeRefreshLayout swipeRefreshLayout;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ChatListAdapter chatAdapter;
    private List<Chat> chatList;
    private ListenerRegistration studentChatListener;
    private ListenerRegistration landlordChatListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupSwipeRefresh();
        setupListeners();
        loadChats();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to header
        applyTopInsets(insets, R.id.headerLayout);
        
        // Apply bottom insets to bottom navigation
        applyBottomInsets(insets, R.id.bottomNavigation);
    }

    private void initializeViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        recyclerViewChats = findViewById(R.id.recyclerViewChats);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
    }

    private void setupRecyclerView() {
        chatList = new ArrayList<>();
        chatAdapter = new ChatListAdapter(chatList, this, this);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChats.setAdapter(chatAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadChats);
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_primary);
    }

    private void setupListeners() {
        // Setup bottom navigation - will fetch user type first
        loadUserTypeAndSetupNavigation();
    }
    
    /**
     * Load user type from Firestore and then setup navigation
     * This ensures we use the correct navigation menu
     * ✅ ALWAYS fetch fresh from Firestore to avoid stale cache issues
     */
    private void loadUserTypeAndSetupNavigation() {
        // ✅ FIX: Always fetch from Firestore to ensure we have the correct user type
        // This prevents issues where cached user type might be wrong
        Log.d(TAG, "Fetching user type from Firestore...");
        
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String userType = doc.getString("userType");
                    if (userType != null) {
                        // Update cache with fresh data
                        android.content.SharedPreferences prefs = 
                            getSharedPreferences("user_prefs", MODE_PRIVATE);
                        prefs.edit().putString("user_type", userType).apply();
                        
                        Log.d(TAG, "Fetched user type from Firestore: " + userType);
                        
                        // Now setup navigation with correct type
                        setupNavigationForUserType(userType);
                    } else {
                        Log.e(TAG, "User type field is null in Firestore");
                        // Fallback to student
                        setupNavigationForUserType("student");
                    }
                } else {
                    Log.e(TAG, "User document does not exist in Firestore");
                    // Fallback to student
                    setupNavigationForUserType("student");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to fetch user type from Firestore: " + e.getMessage());
                // ✅ FIX: On failure, try to use cache as fallback
                android.content.SharedPreferences prefs = 
                    getSharedPreferences("user_prefs", MODE_PRIVATE);
                String cachedUserType = prefs.getString("user_type", "student");
                Log.d(TAG, "Using cached user type as fallback: " + cachedUserType);
                setupNavigationForUserType(cachedUserType);
            });
    }
    
    /**
     * Setup navigation for the given user type
     */
    private void setupNavigationForUserType(String userType) {
        if (bottomNavigation == null) {
            Log.e(TAG, "bottomNavigation is null, cannot setup navigation");
            return;
        }
        
        Log.d(TAG, "Setting up navigation for user type: " + userType);
        
        if ("landlord".equalsIgnoreCase(userType)) {
            com.rct.dormfinder.utils.NavigationHelper.setupLandlordBottomNavigation(
                this, bottomNavigation, R.id.nav_messages);
        } else {
            com.rct.dormfinder.utils.NavigationHelper.setupStudentBottomNavigation(
                this, bottomNavigation, R.id.nav_messages);
        }
    }

    /**
     * Load chats with proper deduplication using a Map to track unique conversations
     * ✅ FORCE INITIAL FETCH FROM SERVER TO GET FRESH UNREAD COUNTS
     */
    private void loadChats() {
        swipeRefreshLayout.setRefreshing(true);
        
        // Remove previous listeners
        if (studentChatListener != null) {
            studentChatListener.remove();
        }
        if (landlordChatListener != null) {
            landlordChatListener.remove();
        }
        
        // Map to store unique chats
        final Map<String, Chat> uniqueChatsMap = new HashMap<>();
        final Map<String, List<String>> duplicateIdsMap = new HashMap<>();
        // ✅ FIX: Use boolean array to track each query separately
        final boolean[] queriesCompleted = {false, false}; // [studentQuery, landlordQuery]
        
        // ✅ ADD: Log current user role
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    String userType = userDoc.getString("userType");
                    Log.d(TAG, "📱 Current User Type: " + userType);
                }
            });
        
        // Step 1: Force server fetch for student chats
        Log.d(TAG, "🔍 Fetching STUDENT chats for user: " + currentUserId);
        db.collection("chats")
                .whereEqualTo("studentId", currentUserId)
                .get(com.google.firebase.firestore.Source.SERVER) // ✅ FORCE SERVER FETCH
                .addOnSuccessListener(studentSnapshots -> {
                    Log.d(TAG, "📡 Initial student chats from SERVER: " + studentSnapshots.size() + " documents");
                    
                    // Process initial data
                    for (int i = 0; i < studentSnapshots.size(); i++) {
                        // ✅ IMPORTANT: Get data directly and log it BEFORE creating Chat object
                        com.google.firebase.firestore.DocumentSnapshot doc = studentSnapshots.getDocuments().get(i);
                        String docId = doc.getId();
                        
                        // Log raw Firestore data
                        Log.d(TAG, "📄 Raw Firestore data for " + docId + ":");
                        Log.d(TAG, "   studentUnreadCount: " + doc.getLong("studentUnreadCount"));
                        Log.d(TAG, "   landlordUnreadCount: " + doc.getLong("landlordUnreadCount"));
                        Log.d(TAG, "   dormitoryName: " + doc.getString("dormitoryName"));
                        
                        // Now create Chat object
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null) {
                            chat.setChatId(docId);
                            
                            // ✅ VERIFY: Check if deserialization preserved unread counts
                            Log.d(TAG, "✅ After deserialization:");
                            Log.d(TAG, "   studentUnreadCount: " + chat.getStudentUnreadCount());
                            Log.d(TAG, "   landlordUnreadCount: " + chat.getLandlordUnreadCount());
                            
                            processChat(chat, docId, uniqueChatsMap, duplicateIdsMap);
                        } else {
                            Log.e(TAG, "❌ Failed to deserialize chat document: " + docId);
                        }
                    }
                    
                    // Now set up real-time listener
                    setupStudentChatListener(uniqueChatsMap, duplicateIdsMap, queriesCompleted);
                    
                    queriesCompleted[0] = true; // Mark student query as complete
                    checkAndUpdateUI(queriesCompleted, uniqueChatsMap, duplicateIdsMap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch student chats from server: " + e.getMessage());
                    // Fallback to listener if server fails
                    setupStudentChatListener(uniqueChatsMap, duplicateIdsMap, queriesCompleted);
                    queriesCompleted[0] = true; // Mark student query as complete
                    checkAndUpdateUI(queriesCompleted, uniqueChatsMap, duplicateIdsMap);
                });
        
        // Step 2: Force server fetch for landlord chats
        Log.d(TAG, "🔍 Fetching LANDLORD chats for user: " + currentUserId);
        db.collection("chats")
                .whereEqualTo("landlordId", currentUserId)
                .get(com.google.firebase.firestore.Source.SERVER) // ✅ FORCE SERVER FETCH
                .addOnSuccessListener(landlordSnapshots -> {
                    Log.d(TAG, "📡 Initial landlord chats from SERVER: " + landlordSnapshots.size() + " documents");
                    
                    // Process initial data
                    for (int j = 0; j < landlordSnapshots.size(); j++) {
                        // ✅ IMPORTANT: Get data directly and log it BEFORE creating Chat object
                        com.google.firebase.firestore.DocumentSnapshot doc = landlordSnapshots.getDocuments().get(j);
                        String docId = doc.getId();
                        
                        // Log raw Firestore data
                        Log.d(TAG, "📄 Raw Firestore data for " + docId + ":");
                        Log.d(TAG, "   studentUnreadCount: " + doc.getLong("studentUnreadCount"));
                        Log.d(TAG, "   landlordUnreadCount: " + doc.getLong("landlordUnreadCount"));
                        Log.d(TAG, "   dormitoryName: " + doc.getString("dormitoryName"));
                        
                        // Now create Chat object
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null) {
                            chat.setChatId(docId);
                            
                            // ✅ VERIFY: Check if deserialization preserved unread counts
                            Log.d(TAG, "✅ After deserialization:");
                            Log.d(TAG, "   studentUnreadCount: " + chat.getStudentUnreadCount());
                            Log.d(TAG, "   landlordUnreadCount: " + chat.getLandlordUnreadCount());
                            
                            processChat(chat, docId, uniqueChatsMap, duplicateIdsMap);
                        } else {
                            Log.e(TAG, "❌ Failed to deserialize chat document: " + docId);
                        }
                    }
                    
                    // Now set up real-time listener
                    setupLandlordChatListener(uniqueChatsMap, duplicateIdsMap, queriesCompleted);
                    
                    queriesCompleted[1] = true; // Mark landlord query as complete
                    checkAndUpdateUI(queriesCompleted, uniqueChatsMap, duplicateIdsMap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch landlord chats from server: " + e.getMessage());
                    // Fallback to listener if server fails
                    setupLandlordChatListener(uniqueChatsMap, duplicateIdsMap, queriesCompleted);
                    queriesCompleted[1] = true; // Mark landlord query as complete
                    checkAndUpdateUI(queriesCompleted, uniqueChatsMap, duplicateIdsMap);
                });
    }
    
    /**
     * Set up real-time listener for student chats (after initial fetch)
     */
    private void setupStudentChatListener(Map<String, Chat> uniqueChatsMap, 
                                          Map<String, List<String>> duplicateIdsMap, 
                                          boolean[] queriesCompleted) {
        studentChatListener = db.collection("chats")
                .whereEqualTo("studentId", currentUserId)
                .addSnapshotListener((studentSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Student chats listener error: " + error.getMessage());
                        return;
                    }
                    
                    // ✅ FIX: Respond to ALL changes (both server and cache)
                    // This ensures UI updates when ChatActivity resets unread count
                    if (studentSnapshots != null) {
                        Log.d(TAG, "📡 Real-time student chats update (from " + 
                              (studentSnapshots.getMetadata().isFromCache() ? "CACHE" : "SERVER") + ")");
                        
                        // Clear and re-process all student chats
                        for (int i = 0; i < studentSnapshots.size(); i++) {
                            Chat chat = studentSnapshots.getDocuments().get(i).toObject(Chat.class);
                            if (chat != null) {
                                String docId = studentSnapshots.getDocuments().get(i).getId();
                                chat.setChatId(docId);
                                processChat(chat, docId, uniqueChatsMap, duplicateIdsMap);
                            }
                        }
                        
                        checkAndUpdateUI(queriesCompleted, uniqueChatsMap, duplicateIdsMap);
                    }
                });
    }
    
    /**
     * Set up real-time listener for landlord chats (after initial fetch)
     */
    private void setupLandlordChatListener(Map<String, Chat> uniqueChatsMap, 
                                           Map<String, List<String>> duplicateIdsMap, 
                                           boolean[] queriesCompleted) {
        landlordChatListener = db.collection("chats")
                .whereEqualTo("landlordId", currentUserId)
                .addSnapshotListener((landlordSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Landlord chats listener error: " + error.getMessage());
                        return;
                    }
                    
                    // ✅ FIX: Respond to ALL changes (both server and cache)
                    // This ensures UI updates when ChatActivity resets unread count
                    if (landlordSnapshots != null) {
                        Log.d(TAG, "📡 Real-time landlord chats update (from " + 
                              (landlordSnapshots.getMetadata().isFromCache() ? "CACHE" : "SERVER") + ")");
                        
                        // Clear and re-process all landlord chats
                        for (int j = 0; j < landlordSnapshots.size(); j++) {
                            Chat chat = landlordSnapshots.getDocuments().get(j).toObject(Chat.class);
                            if (chat != null) {
                                String docId = landlordSnapshots.getDocuments().get(j).getId();
                                chat.setChatId(docId);
                                processChat(chat, docId, uniqueChatsMap, duplicateIdsMap);
                            }
                        }
                        
                        checkAndUpdateUI(queriesCompleted, uniqueChatsMap, duplicateIdsMap);
                    }
                });
    }
    
    /**
     * Process a chat and handle deduplication
     */
    private void processChat(Chat chat, String docId, Map<String, Chat> uniqueChatsMap, 
                           Map<String, List<String>> duplicateIdsMap) {
        String chatKey = generateChatKey(chat.getStudentId(), chat.getLandlordId(), chat.getDormitoryId());
        
        // 🔍 DEBUG: Log the chat data being processed
        Log.d(TAG, "📝 Processing chat: " + chat.getDormitoryName());
        Log.d(TAG, "   Doc ID: " + docId);
        Log.d(TAG, "   Student Unread: " + chat.getStudentUnreadCount());
        Log.d(TAG, "   Landlord Unread: " + chat.getLandlordUnreadCount());
        Log.d(TAG, "   Last Message: " + chat.getLastMessage());
        Log.d(TAG, "   Timestamp: " + chat.getLastMessageTimestamp());
        
        // Track all document IDs for this conversation
        if (!duplicateIdsMap.containsKey(chatKey)) {
            duplicateIdsMap.put(chatKey, new ArrayList<>());
        }
        duplicateIdsMap.get(chatKey).add(docId);
        
        // Check if we already have this conversation
        if (uniqueChatsMap.containsKey(chatKey)) {
            Chat existingChat = uniqueChatsMap.get(chatKey);
            
            // Keep the chat with the most recent activity OR higher unread count
            boolean useNewChat = chat.getLastMessageTimestamp() > existingChat.getLastMessageTimestamp();
            
            // ✅ ALSO prefer chat with unread messages
            int newUnread = chat.getUnreadCountForUser(currentUserId);
            int existingUnread = existingChat.getUnreadCountForUser(currentUserId);
            
            if (newUnread > existingUnread) {
                useNewChat = true;
                Log.d(TAG, "   ⚠️ Using newer chat because it has more unread: " + newUnread + " vs " + existingUnread);
            }
            
            if (useNewChat) {
                uniqueChatsMap.put(chatKey, chat);
                Log.d(TAG, "✅ Updating chat key: " + chatKey + " with document: " + docId);
            } else {
                Log.d(TAG, "⏭️ Keeping existing chat key: " + chatKey + " (current is newer than: " + docId + ")");
            }
        } else {
            // First time seeing this conversation
            uniqueChatsMap.put(chatKey, chat);
            Log.d(TAG, "🆕 Added new chat key: " + chatKey);
        }
    }
    
    /**
     * Check if both queries completed and update UI
     */
    private void checkAndUpdateUI(boolean[] queriesCompleted, Map<String, Chat> uniqueChatsMap, 
                                  Map<String, List<String>> duplicateIdsMap) {
        // ✅ FIX: Only update UI when BOTH queries have completed
        if (queriesCompleted[0] && queriesCompleted[1]) {
            Log.d(TAG, "✅ Both queries completed, updating UI with " + uniqueChatsMap.size() + " chats");
            // Both queries completed
            
            // Delete duplicate documents (keep only one per conversation)
            deleteDuplicateChats(uniqueChatsMap, duplicateIdsMap);
            
            // Update UI with unique chats
            chatList.clear();
            chatList.addAll(uniqueChatsMap.values());
            
            // Sort by most recent message
            chatList.sort((c1, c2) -> Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
            
            // ✅ FIX: Update adapter's internal list to reflect new data with unread counts
            chatAdapter.updateChats(chatList);
            updateEmptyState();
            swipeRefreshLayout.setRefreshing(false);
            
            Log.d(TAG, "Loaded " + chatList.size() + " unique chats");
        }
    }
    
    /**
     * Delete duplicate chat documents, keeping only the most recent one
     */
    private void deleteDuplicateChats(Map<String, Chat> uniqueChatsMap, 
                                     Map<String, List<String>> duplicateIdsMap) {
        for (Map.Entry<String, List<String>> entry : duplicateIdsMap.entrySet()) {
            String chatKey = entry.getKey();
            List<String> docIds = entry.getValue();
            
            // If more than one document exists for this conversation
            if (docIds.size() > 1) {
                Chat keepChat = uniqueChatsMap.get(chatKey);
                String keepDocId = keepChat.getChatId();
                
                Log.d(TAG, "Found " + docIds.size() + " documents for conversation: " + chatKey);
                Log.d(TAG, "Keeping document: " + keepDocId);
                
                // Delete all other documents
                for (String docId : docIds) {
                    if (!docId.equals(keepDocId)) {
                        Log.d(TAG, "Deleting duplicate document: " + docId);
                        db.collection("chats").document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> 
                                    Log.d(TAG, "Successfully deleted duplicate: " + docId))
                                .addOnFailureListener(e -> 
                                    Log.e(TAG, "Failed to delete duplicate: " + docId + ", error: " + e.getMessage()));
                    }
                }
            }
        }
    }
    
    /**
     * Generate a unique key for a chat conversation
     */
    private String generateChatKey(String studentId, String landlordId, String dormitoryId) {
        return studentId + "_" + landlordId + "_" + (dormitoryId != null ? dormitoryId : "unknown");
    }

    private void updateEmptyState() {
        if (chatList == null || chatList.isEmpty()) {
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
            }
            if (recyclerViewChats != null) {
                recyclerViewChats.setVisibility(View.GONE);
            }
        } else {
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.GONE);
            }
            if (recyclerViewChats != null) {
                recyclerViewChats.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onChatClick(Chat chat) {
        if (chat == null) {
            Toast.makeText(this, "Error: Chat data is invalid", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Use the stored chatId from Firestore document
        String chatId = chat.getChatId();
        
        // Get other participant ID
        String otherUserId = chat.getOtherParticipantId(currentUserId);
        
        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Error: Unable to determine chat partner", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // ✅ RESET UNREAD COUNT ONLY WHEN USER CLICKS TO OPEN CHAT
        resetUnreadCountForCurrentUser(chat);
        
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chat_id", chatId);
        intent.putExtra("partner_id", otherUserId);
        intent.putExtra("dormitory_id", chat.getDormitoryId());
        intent.putExtra("dormitory_name", chat.getDormitoryName());
        startActivity(intent);
    }
    
    /**
     * Reset unread count for the current user when they click to open a chat
     */
    private void resetUnreadCountForCurrentUser(Chat chat) {
        // Determine which unread field to reset based on current user
        String unreadField;
        int currentUnreadCount;
        
        if (currentUserId.equals(chat.getStudentId())) {
            unreadField = "studentUnreadCount";
            currentUnreadCount = chat.getStudentUnreadCount();
        } else {
            unreadField = "landlordUnreadCount";
            currentUnreadCount = chat.getLandlordUnreadCount();
        }
        
        // Only update if there are unread messages
        if (currentUnreadCount > 0) {
            Log.d(TAG, "Resetting " + unreadField + " from " + currentUnreadCount + " to 0 for chat: " + chat.getChatId());
            
            db.collection("chats").document(chat.getChatId())
                    .update(unreadField, 0)
                    .addOnSuccessListener(aVoid -> 
                        Log.d(TAG, "Successfully reset " + unreadField + " to 0"))
                    .addOnFailureListener(e -> 
                        Log.w(TAG, "Failed to reset " + unreadField + ": " + e.getMessage()));
        }
    }

    @Override
    public void onDeleteChat(Chat chat) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to delete this conversation? This will also delete all messages.")
                .setPositiveButton("Delete", (dialog, which) -> deleteChat(chat))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteChat(Chat chat) {
        String chatId = chat.getChatId();
        
        // Delete all messages first
        db.collection("chats").document(chatId)
                .collection("messages")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Delete all messages
                    for (int i = 0; i < querySnapshot.size(); i++) {
                        querySnapshot.getDocuments().get(i).getReference().delete();
                    }
                    
                    // Then delete the chat document
                    db.collection("chats").document(chatId)
                            .delete()
                            .addOnSuccessListener(aVoid -> 
                                Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> 
                                Toast.makeText(this, "Failed to delete conversation: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to delete messages: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (studentChatListener != null) {
            studentChatListener.remove();
        }
        if (landlordChatListener != null) {
            landlordChatListener.remove();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // CRITICAL: Ensure Messages is selected AFTER view is rendered
        if (bottomNavigation != null) {
            bottomNavigation.post(() -> {
                bottomNavigation.setSelectedItemId(R.id.nav_messages);
                Log.d(TAG, "✅ Bottom nav set to Messages in onResume");
            });
        }
        
        // ✅ Refresh chat list when returning from ChatActivity
        // This ensures highlighting updates when unread count changes
        Log.d(TAG, "🔄 Activity resumed, refreshing chat list...");
        // ✅ FIX: Reload chats from Firestore to get updated unread counts
        loadChats();
    }
}
