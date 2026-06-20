package com.rct.dormfinder.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.GuideAdapter;
import com.rct.dormfinder.models.GuideItem;
import com.rct.dormfinder.models.User;
import java.util.ArrayList;
import java.util.List;

public class AppGuideActivity extends AppCompatActivity {
    private static final String TAG = "AppGuideActivity";
    
    private ImageView ivBack;
    private RecyclerView recyclerViewGuide;
    private GuideAdapter guideAdapter;
    private Button btnGetStarted;
    private androidx.cardview.widget.CardView cardFAQ;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userType = "student"; // Default to student
    private boolean isFirstTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_guide);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        loadUserTypeAndSetupGuide();
        setupListeners();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        recyclerViewGuide = findViewById(R.id.recyclerViewGuide);
        btnGetStarted = findViewById(R.id.btnGetStarted);
        cardFAQ = findViewById(R.id.cardFAQ);
        
        // Check if this is first time from registration
        isFirstTime = getIntent().getBooleanExtra("isFirstTime", false);
        String intentUserType = getIntent().getStringExtra("userType");
        if (intentUserType != null) {
            userType = intentUserType;
        }
        
        // Show Get Started button for first-time users
        if (isFirstTime) {
            btnGetStarted.setVisibility(View.VISIBLE);
        }
    }

    private void loadUserTypeAndSetupGuide() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        
        if (userId == null) {
            // User not logged in, show general guide
            setupRecyclerView("student");
            return;
        }

        // Fetch user type from Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        userType = user.getUserType() != null ? user.getUserType() : "student";
                        Log.d(TAG, "User type loaded: " + userType);
                    }
                }
                setupRecyclerView(userType);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading user type", e);
                Toast.makeText(this, "Error loading user information", Toast.LENGTH_SHORT).show();
                setupRecyclerView("student"); // Default to student on error
            });
    }

    private void setupRecyclerView(String userType) {
        List<GuideItem> guideItems = createGuideItems(userType);
        guideAdapter = new GuideAdapter(guideItems, this);
        recyclerViewGuide.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewGuide.setAdapter(guideAdapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> {
            if (isFirstTime) {
                // Navigate to appropriate home screen after guide
                navigateToHome();
            } else {
                finish();
            }
        });
        
        btnGetStarted.setOnClickListener(v -> {
            navigateToHome();
        });
        
        // FAQ button click
        cardFAQ.setOnClickListener(v -> {
            Intent intent = new Intent(this, FAQActivity.class);
            startActivity(intent);
        });
    }
    
    private void navigateToHome() {
        Intent intent;
        if ("student".equalsIgnoreCase(userType)) {
            intent = new Intent(this, StudentHomeActivity.class);
        } else {
            intent = new Intent(this, LandlordHomeActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        if (isFirstTime) {
            navigateToHome();
        } else {
            super.onBackPressed();
        }
    }

    private List<GuideItem> createGuideItems(String userType) {
        List<GuideItem> items = new ArrayList<>();

        // App Introduction
        if (isFirstTime) {
            items.add(new GuideItem("Welcome to DormFinder! 🎉", 
                "Thank you for joining DormFinder! Let's help you get started with a quick guide on how to use the app.", 
                GuideItem.TYPE_HEADER));
        } else {
            items.add(new GuideItem("Welcome to DormFinder! 🏠", 
                "DormFinder helps students find perfect dormitories and landlords manage their properties efficiently.", 
                GuideItem.TYPE_HEADER));
        }

        // Show content based on user type
        if ("student".equalsIgnoreCase(userType)) {
            addStudentGuide(items);
        } else if ("landlord".equalsIgnoreCase(userType)) {
            addLandlordGuide(items);
        }

        // Common sections for all users
        addSafetyTips(items);
        addTroubleshooting(items);
        addContactSupport(items);

        return items;
    }

    private void addStudentGuide(List<GuideItem> items) {
        // For Students
        items.add(new GuideItem("📱 Student Guide", "", GuideItem.TYPE_SECTION));
        
        items.add(new GuideItem("1. Getting Started", 
            "• Register as a Student\n" +
            "• Complete your profile with school and course information\n" +
            "• Add a profile picture to build trust\n" +
            "• Tap the bell icon to view notifications\n" +
            "• Open Help & Guide using the ? icon in the header", 
            GuideItem.TYPE_STEP));

        items.add(new GuideItem("2. Finding Dormitories", 
            "• Use the Search feature to browse available dorms\n" +
            "• Filter by city, price range, and amenities\n" +
            "• Check the Map view to see locations\n" +
            "• View detailed photos and information\n" +
            "• Save favorites by tapping the heart icon", 
            GuideItem.TYPE_STEP));

        items.add(new GuideItem("3. Contacting Landlords", 
            "• Tap 'Message' to chat with landlords\n" +
            "• Ask questions about the dormitory\n" +
            "• Request to visit the property\n" +
            "• Use 'Call' for immediate contact\n" +
            "• Be polite and professional in all communications", 
            GuideItem.TYPE_STEP));

        items.add(new GuideItem("4. Booking a Room", 
            "• Tap 'Book Now' on your chosen dormitory\n" +
            "• Fill out the booking request form\n" +
            "• Wait for landlord approval\n" +
            "• Check 'My Bookings' for status updates\n" +
            "• Visit the property before confirming", 
            GuideItem.TYPE_STEP));

        items.add(new GuideItem("5. Managing Your Bookings", 
            "• View all your bookings in 'My Bookings'\n" +
            "• Check booking status (pending/approved/declined)\n" +
            "• Cancel bookings if needed\n" +
            "• Keep track of booking dates\n" +
            "• Communicate with landlords through messages", 
            GuideItem.TYPE_STEP));

        items.add(new GuideItem("6. Writing Reviews", 
            "• Share your experience after staying\n" +
            "• Rate the dormitory honestly\n" +
            "• Mention both pros and cons\n" +
            "• Help other students make informed decisions\n" +
            "• Be respectful and constructive", 
            GuideItem.TYPE_STEP));

        // Student Best Practices
        items.add(new GuideItem("✨ Student Best Practices", "", GuideItem.TYPE_SECTION));

        items.add(new GuideItem("Tips for Success", 
            "• Keep your profile updated and complete\n" +
            "• Be respectful when messaging landlords\n" +
            "• Ask relevant questions about the dormitory\n" +
            "• Visit the property before making decisions\n" +
            "• Read all terms and conditions carefully\n" +
            "• Respond to landlords promptly\n" +
            "• Check reviews from other students\n" +
            "• Verify amenities and facilities in person", 
            GuideItem.TYPE_STEP));
    }

    private void addLandlordGuide(List<GuideItem> items) {
        // For Landlords
        items.add(new GuideItem("🏠 Landlord Guide", "", GuideItem.TYPE_SECTION));

        items.add(new GuideItem("1. Getting Started", 
            "• Register as a Landlord\n" +
            "• Complete your profile with contact information\n" +
            "• Add a professional profile picture\n" +
            "• Verify your business details", 
            GuideItem.TYPE_STEP));

        items.add(new GuideItem("2. Adding Your Dormitory", 
            "• Tap 'Add Dormitory' on your home screen\n" +
            "• Fill in all required information:\n" +
            "  - Name and description\n" +
            "  - Address and city\n" +
            "  - Monthly price and total rooms\n" +
            "• Add multiple high-quality photos\n" +
            "• Select location on the map\n" +
            "• Choose available amenities\n" +
            "• Write clear house rules", 
            GuideItem.TYPE_STEP));

        items.add(new GuideItem("3. Managing Your Properties", 
            "• View all your dormitories in 'My Dormitories'\n" +
            "• Edit dormitory details anytime\n" +
            "• Toggle availability status\n" +
            "• Monitor booking requests\n" +
            "• Track occupancy rates\n" +
            "• Update photos regularly\n" +
            "• Manage room availability", 
            GuideItem.TYPE_STEP));

        items.add(new GuideItem("4. Handling Bookings", 
            "• Check 'Booking Requests' regularly\n" +
            "• Review student profiles before approving\n" +
            "• Approve or decline requests promptly\n" +
            "• Communicate with potential tenants\n" +
            "• Set clear expectations\n" +
            "• Schedule property viewings\n" +
            "• Keep records of all bookings", 
            GuideItem.TYPE_STEP));

        items.add(new GuideItem("5. Managing Payments", 
            "• Track payment status for all tenants\n" +
            "• Verify payment receipts\n" +
            "• Send payment reminders\n" +
            "• Maintain payment records\n" +
            "• Process refunds when necessary\n" +
            "• Update payment methods if needed", 
            GuideItem.TYPE_STEP));

        items.add(new GuideItem("6. Handling Reviews", 
            "• Monitor reviews from students\n" +
            "• Respond professionally to feedback\n" +
            "• Address concerns raised in reviews\n" +
            "• Reply to reviews to show engagement\n" +
            "• Use feedback to improve services\n" +
            "• Report inappropriate reviews", 
            GuideItem.TYPE_STEP));

        // Landlord Best Practices
        items.add(new GuideItem("✨ Landlord Best Practices", "", GuideItem.TYPE_SECTION));

        items.add(new GuideItem("Tips for Success", 
            "• Use high-quality, well-lit photos\n" +
            "• Write detailed, honest descriptions\n" +
            "• Respond to inquiries quickly (within 24 hours)\n" +
            "• Keep availability status updated\n" +
            "• Be transparent about rules and policies\n" +
            "• Maintain your property well\n" +
            "• Provide clear house rules\n" +
            "• Be professional in all communications\n" +
            "• Schedule regular property maintenance\n" +
            "• Build good relationships with tenants", 
            GuideItem.TYPE_STEP));
    }

    private void addSafetyTips(List<GuideItem> items) {
        // Safety Tips
        items.add(new GuideItem("🔒 Safety Tips", "", GuideItem.TYPE_SECTION));

        items.add(new GuideItem("For Everyone", 
            "• Always meet in person before finalizing deals\n" +
            "• Verify property ownership documents\n" +
            "• Don't share personal financial information\n" +
            "• Trust your instincts about people and places\n" +
            "• Keep records of all communications\n" +
            "• Report suspicious activity immediately\n" +
            "• Use the in-app messaging system\n" +
            "• Bring someone with you for property visits\n" +
            "• Verify identity of all parties\n" +
            "• Read and understand all agreements", 
            GuideItem.TYPE_STEP));
    }

    private void addTroubleshooting(List<GuideItem> items) {
        // Troubleshooting
        items.add(new GuideItem("🔧 Troubleshooting", "", GuideItem.TYPE_SECTION));

        items.add(new GuideItem("Common Issues", 
            "• App not loading: Check internet connection\n" +
            "• Images not showing: Clear app cache\n" +
            "• Can't send messages: Verify account status\n" +
            "• Location not working: Enable GPS permissions\n" +
            "• Profile not saving: Check required fields\n" +
            "• Login issues: Reset password if needed\n" +
            "• Booking issues: Contact landlord directly\n" +
            "• Payment issues: Check payment method\n" +
            "• Notification issues: Check app permissions", 
            GuideItem.TYPE_STEP));
    }

    private void addContactSupport(List<GuideItem> items) {
        // Contact Support
        items.add(new GuideItem("📞 Need Help?", 
            "If you need additional assistance:\n" +
            "• Open Help & Guide from the header (question‑mark icon)\n" +
            "• Email: support@dormfinder.com\n" +
            "• Phone: +63 123 456 7890\n" +
            "• Available: Monday-Friday, 9AM-6PM\n" +
            "• Response time: Within 24 hours\n" +
            "• For urgent issues, use phone support", 
            GuideItem.TYPE_STEP));
    }
}
