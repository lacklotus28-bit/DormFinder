package com.rct.dormfinder.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.MessageAdapter;
import com.rct.dormfinder.models.Chat;
import com.rct.dormfinder.models.Message;
import com.rct.dormfinder.models.User;
import com.rct.dormfinder.utils.ConfirmationDialogHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends BaseActivity {
    private TextView tvChatPartnerName, tvDormitoryName;
    private ImageView ivBack, ivCallPhone, btnSend;
    private RecyclerView recyclerViewMessages;
    private EditText etMessage;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private ListenerRegistration messageListener;

    private String chatId;
    private String partnerId;
    private String partnerName;
    private String partnerPhone;
    private String dormitoryId;
    private String dormitoryName;
    private String currentUserId;
    private boolean isDeduplicationChecked = false;
    private boolean isCurrentUserStudent = false; // Track if current user is student or landlord

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_chat);

        setupWindowInsets();
        initializeViews();
        getIntentData();
        setupFirestore();
        setupRecyclerView();
        setupListeners();
        loadPartnerInfo();
        checkForDuplicatesAndLoadMessages();
    }
    
    private void setupWindowInsets() {
        // Enable edge-to-edge display with proper system UI handling
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getColor(R.color.mint_primary));
            getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
            );
        }
        
        // Set light status bar icons (dark text) for better visibility on mint background
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }
        
        // Apply WindowInsets to header and message input for proper positioning
        android.view.View headerLayout = findViewById(R.id.headerLayout);
        android.view.View messageInputContainer = findViewById(R.id.messageInputContainer);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewMessages);
        android.view.View rootView = findViewById(android.R.id.content);
        
        if (headerLayout != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                androidx.core.graphics.Insets ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
                
                // Base padding values
                int basePaddingHorizontal = (int) (12 * getResources().getDisplayMetrics().density);
                int basePaddingVertical = (int) (12 * getResources().getDisplayMetrics().density);
                
                // Apply padding to header: account for status bar
                headerLayout.setPadding(
                    basePaddingHorizontal,
                    systemBars.top + basePaddingVertical,
                    basePaddingHorizontal,
                    basePaddingVertical
                );
                
                // Apply padding to message input container: account for navigation bar
                if (messageInputContainer != null) {
                    int bottomPadding = systemBars.bottom + (int) (4 * getResources().getDisplayMetrics().density);
                    messageInputContainer.setPadding(
                        basePaddingHorizontal,
                        basePaddingVertical,
                        basePaddingHorizontal,
                        bottomPadding
                    );
                }
                
                // Auto-scroll to bottom when keyboard appears
                boolean isKeyboardVisible = ime.bottom > 0;
                if (isKeyboardVisible && recyclerView != null && messageAdapter != null && !messages.isEmpty()) {
                    recyclerView.post(() -> {
                        recyclerView.smoothScrollToPosition(messages.size() - 1);
                    });
                }
                
                return insets;
            });
            
            // Request insets to be applied
            androidx.core.view.ViewCompat.requestApplyInsets(rootView);
        }
    }
    
    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return (int) (24 * getResources().getDisplayMetrics().density);
    }
    
    private int getNavigationBarHeight() {
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private void initializeViews() {
        tvChatPartnerName = findViewById(R.id.tvChatPartnerName);
        tvDormitoryName = findViewById(R.id.tvDormitoryName);
        ivBack = findViewById(R.id.ivBack);
        ivCallPhone = findViewById(R.id.ivCallPhone);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        chatId = intent.getStringExtra("chat_id");
        partnerId = intent.getStringExtra("partner_id");
        dormitoryId = intent.getStringExtra("dormitory_id");
        dormitoryName = intent.getStringExtra("dormitory_name");

        if (chatId == null || partnerId == null) {
            Toast.makeText(this, "Invalid chat data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDormitoryName.setText("Regarding: " + dormitoryName);
    }

    private void setupFirestore() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        
        // Determine if current user is student or landlord by checking chatId format
        String[] parts = chatId.split("_");
        if (parts.length >= 2) {
            isCurrentUserStudent = currentUserId.equals(parts[0]);
        }
        
        android.util.Log.d("ChatActivity", "Current user is " + (isCurrentUserStudent ? "STUDENT" : "LANDLORD"));
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> handleBackPress());

        btnSend.setOnClickListener(v -> sendMessage());

        ivCallPhone.setOnClickListener(v -> {
            if (partnerPhone != null && !partnerPhone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + partnerPhone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Enhanced EditText behavior for Messenger-like experience
        etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && messageAdapter != null && !messages.isEmpty()) {
                // Auto-scroll to bottom when user focuses on input (like Messenger)
                recyclerViewMessages.post(() -> {
                    recyclerViewMessages.smoothScrollToPosition(messages.size() - 1);
                });
            }
        });

        // Handle Enter key to send message (optional enhancement)
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && 
                 event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void loadPartnerInfo() {
        android.util.Log.d("ChatActivity", "Loading partner info for ID: " + partnerId);
        
        db.collection("users").document(partnerId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User partner = document.toObject(User.class);
                        if (partner != null) {
                            partnerName = partner.getName();
                            partnerPhone = partner.getContactNumber();
                            tvChatPartnerName.setText(partnerName);
                            android.util.Log.d("ChatActivity", "Partner info loaded successfully: " + partnerName);
                        } else {
                            setDefaultPartnerInfo();
                        }
                    } else {
                        setDefaultPartnerInfo();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Failed to load partner info: " + e.getMessage());
                    setDefaultPartnerInfo();
                });
    }
    
    private void setDefaultPartnerInfo() {
        partnerName = "Chat Partner";
        tvChatPartnerName.setText(partnerName);
        ivCallPhone.setEnabled(false);
        ivCallPhone.setAlpha(0.5f);
    }

    /**
     * Check for duplicate chats ONCE before loading messages
     */
    private void checkForDuplicatesAndLoadMessages() {
        if (isDeduplicationChecked) {
            loadMessages();
            return;
        }

        android.util.Log.d("ChatActivity", "Checking for duplicate chats...");
        isDeduplicationChecked = true;

        // Parse chatId to get participants
        String[] parts = chatId.split("_");
        if (parts.length < 3) {
            android.util.Log.e("ChatActivity", "Invalid chatId format: " + chatId);
            loadMessages();
            return;
        }
        
        String studentId = parts[0];
        String landlordId = parts[1];
        String dormId = parts[2];
        
        // Check for existing chats with same participants and dormitory
        db.collection("chats")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("landlordId", landlordId)
                .whereEqualTo("dormitoryId", dormId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        android.util.Log.d("ChatActivity", "No existing chats found, will create new one");
                        createNewChatDocument(studentId, landlordId, dormId);
                        loadMessages();
                    } else if (querySnapshot.size() == 1) {
                        String existingChatId = querySnapshot.getDocuments().get(0).getId();
                        
                        if (!existingChatId.equals(chatId)) {
                            android.util.Log.d("ChatActivity", "Using existing chat: " + existingChatId);
                            chatId = existingChatId;
                        }
                        
                        // ✅ RESET UNREAD COUNT FOR CURRENT USER WHEN OPENING CHAT
                        resetMyUnreadCount();
                        loadMessages();
                    } else {
                        android.util.Log.w("ChatActivity", "Found " + querySnapshot.size() + " duplicate chats");
                        
                        // Find the most recent chat
                        String mostRecentChatId = null;
                        long mostRecentTimestamp = 0;
                        
                        for (int i = 0; i < querySnapshot.size(); i++) {
                            String docId = querySnapshot.getDocuments().get(i).getId();
                            Long timestamp = querySnapshot.getDocuments().get(i).getLong("lastMessageTimestamp");
                            
                            if (timestamp != null && timestamp > mostRecentTimestamp) {
                                mostRecentTimestamp = timestamp;
                                mostRecentChatId = docId;
                            }
                        }
                        
                        // Delete duplicates (keep the most recent)
                        for (int i = 0; i < querySnapshot.size(); i++) {
                            String docId = querySnapshot.getDocuments().get(i).getId();
                            if (!docId.equals(mostRecentChatId)) {
                                db.collection("chats").document(docId).delete();
                                android.util.Log.d("ChatActivity", "Deleted duplicate: " + docId);
                            }
                        }
                        
                        if (mostRecentChatId != null) {
                            chatId = mostRecentChatId;
                        }
                        
                        // ✅ RESET UNREAD COUNT FOR CURRENT USER
                        resetMyUnreadCount();
                        loadMessages();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Failed to check for duplicates: " + e.getMessage());
                    loadMessages();
                });
    }
    
    /**
     * ✅ Reset unread count for current user when they open the chat
     */
    private void resetMyUnreadCount() {
        String unreadField = isCurrentUserStudent ? "studentUnreadCount" : "landlordUnreadCount";
        
        db.collection("chats").document(chatId)
                .update(unreadField, 0)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ChatActivity", "Reset " + unreadField + " to 0");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Failed to reset " + unreadField + ": " + e.getMessage());
                });
    }

    private void loadMessages() {
        android.util.Log.d("ChatActivity", "Setting up message listener for chatId: " + chatId);
        
        if (messageListener != null) {
            messageListener.remove();
        }
        
        messageListener = db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("ChatActivity", "Failed to load messages: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Message message = dc.getDocument().toObject(Message.class);
                            message.setMessageId(dc.getDocument().getId());

                            switch (dc.getType()) {
                                case ADDED:
                                    messages.add(message);
                                    messageAdapter.notifyItemInserted(messages.size() - 1);
                                    recyclerViewMessages.scrollToPosition(messages.size() - 1);

                                    // Mark message as read if from partner
                                    if (!message.getSenderId().equals(currentUserId)) {
                                        markMessageAsRead(message.getMessageId());
                                    }
                                    break;
                                case MODIFIED:
                                    break;
                                case REMOVED:
                                    break;
                            }
                        }
                    }
                });
    }
    
    private void createNewChatDocument(String studentId, String landlordId, String dormId) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("studentId", studentId);
        chatData.put("landlordId", landlordId);
        chatData.put("dormitoryId", dormId);
        chatData.put("dormitoryName", dormitoryName != null ? dormitoryName : "");
        chatData.put("createdAt", System.currentTimeMillis());
        chatData.put("lastMessage", "");
        chatData.put("lastMessageTimestamp", System.currentTimeMillis());
        chatData.put("studentUnreadCount", 0);  // ✅ Separate unread counts
        chatData.put("landlordUnreadCount", 0); // ✅ Separate unread counts
        chatData.put("isActive", true);
        
        db.collection("chats").document(chatId)
                .set(chatData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ChatActivity", "Chat document created successfully");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Failed to create chat: " + e.getMessage());
                });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }

        etMessage.setText("");

        Message message = new Message(chatId, currentUserId, partnerId, content);

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    android.util.Log.d("ChatActivity", "Message sent successfully");
                    updateChatLastMessage(content);
                    
                    // ✅ INCREMENT PARTNER'S UNREAD COUNT
                    incrementPartnerUnreadCount();
                    
                    // NOTE: We don't create notification documents for messages
                    // Messages have their own badge counter in the UI
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Failed to send message: " + e.getMessage());
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * ✅ Increment the partner's unread count when we send a message
     */
    private void incrementPartnerUnreadCount() {
        // If current user is student, increment landlord's unread count
        // If current user is landlord, increment student's unread count
        String unreadField = isCurrentUserStudent ? "landlordUnreadCount" : "studentUnreadCount";
        
        android.util.Log.d("ChatActivity", "===== INCREMENTING UNREAD COUNT =====");
        android.util.Log.d("ChatActivity", "Current user is: " + (isCurrentUserStudent ? "STUDENT" : "LANDLORD"));
        android.util.Log.d("ChatActivity", "Incrementing field: " + unreadField);
        android.util.Log.d("ChatActivity", "Chat ID: " + chatId);
        
        db.collection("chats").document(chatId)
                .update(unreadField, FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ChatActivity", "✅ Successfully incremented " + unreadField);
                    
                    // 🔍 VERIFY: Check what the actual value is now
                    db.collection("chats").document(chatId).get()
                            .addOnSuccessListener(doc -> {
                                android.util.Log.d("ChatActivity", "🔍 VERIFY AFTER INCREMENT:");
                                android.util.Log.d("ChatActivity", "   studentUnreadCount = " + doc.getLong("studentUnreadCount"));
                                android.util.Log.d("ChatActivity", "   landlordUnreadCount = " + doc.getLong("landlordUnreadCount"));
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "❌ Failed to increment " + unreadField + ": " + e.getMessage());
                    
                    // If chat document doesn't exist, create it
                    android.util.Log.d("ChatActivity", "Attempting to create chat document...");
                    String[] parts = chatId.split("_");
                    if (parts.length >= 3) {
                        createNewChatDocument(parts[0], parts[1], parts[2]);
                    }
                });
    }
    


    private void updateChatLastMessage(String lastMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("lastMessageTimestamp", System.currentTimeMillis());

        db.collection("chats").document(chatId)
                .update(updates)
                .addOnFailureListener(e -> {
                    String[] parts = chatId.split("_");
                    if (parts.length >= 3) {
                        createNewChatDocument(parts[0], parts[1], parts[2]);
                    }
                });
    }

    private void markMessageAsRead(String messageId) {
        db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update("isRead", true)
                .addOnFailureListener(e -> {
                    // Silently fail - not critical
                });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Reset unread count whenever user returns to this chat
        // This ensures highlighting disappears even if chat was already open
        if (chatId != null && !chatId.isEmpty()) {
            resetMyUnreadCount();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }

    private void handleBackPress() {
        boolean hasUnsentMessage = !etMessage.getText().toString().trim().isEmpty();

        if (hasUnsentMessage) {
            ConfirmationDialogHelper.showLeaveChatDialog(this,
                    new ConfirmationDialogHelper.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            finish();
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }
}
