package com.rct.dormfinder.activities;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.FAQAdapter;
import com.rct.dormfinder.models.FAQItem;
import java.util.ArrayList;
import java.util.List;

public class FAQActivity extends BaseActivity {
    private static final String TAG = "FAQActivity";
    
    private ImageView ivBack;
    private RecyclerView recyclerViewFAQ;
    private FAQAdapter faqAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        initializeViews();
        setupRecyclerView();
        setupListeners();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top margin to header to push it below status bar
        android.view.View headerLayout = findViewById(R.id.headerLayout);
        if (headerLayout != null) {
            android.view.ViewGroup.MarginLayoutParams params = 
                (android.view.ViewGroup.MarginLayoutParams) headerLayout.getLayoutParams();
            params.topMargin = insets.top;
            headerLayout.setLayoutParams(params);
        }
        
        // Apply bottom padding to RecyclerView for navigation bar
        applyBottomInsets(insets, R.id.recyclerViewFAQ, 0.8f);
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        recyclerViewFAQ = findViewById(R.id.recyclerViewFAQ);
    }

    private void setupRecyclerView() {
        List<FAQItem> faqItems = createFAQItems();
        faqAdapter = new FAQAdapter(faqItems);
        recyclerViewFAQ.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFAQ.setAdapter(faqAdapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private List<FAQItem> createFAQItems() {
        List<FAQItem> items = new ArrayList<>();

        // General Questions
        items.add(new FAQItem("General Questions", "", FAQItem.TYPE_CATEGORY));
        
        items.add(new FAQItem(
            "What is DormFinder?",
            "DormFinder is a mobile application that connects students looking for dormitories with landlords who have properties available for rent. It makes finding and managing dormitory accommodations easy and convenient.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "Is DormFinder free to use?",
            "Yes! DormFinder is completely free for students to browse, search, and book dormitories. Landlords can also list their properties at no cost.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "Do I need to create an account?",
            "Yes, you need to create an account to access all features. Registration is quick and easy - just provide your email, create a password, and choose whether you're a student or landlord.",
            FAQItem.TYPE_QUESTION
        ));

        // For Students
        items.add(new FAQItem("For Students", "", FAQItem.TYPE_CATEGORY));

        items.add(new FAQItem(
            "How do I search for dormitories?",
            "Use the search bar on the home screen to search by name, location, or city. You can also use filters to narrow down results by price range, amenities, and availability.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "Can I save my favorite dormitories?",
            "Yes! Tap the heart icon on any dormitory listing to add it to your favorites. Access all your saved dorms from the Favorites tab in the bottom navigation.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "How do I book a dormitory?",
            "1. Browse and select a dormitory\n2. Tap 'Book Now'\n3. Fill out the booking form\n4. Submit your request\n5. Wait for landlord approval\n6. Check 'My Bookings' for status updates",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "What happens after I submit a booking request?",
            "The landlord will receive your request and can either approve or decline it. You'll get a notification about their decision. If approved, you can proceed with viewing the property and finalizing the arrangement.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "Can I message the landlord before booking?",
            "Yes! Tap the 'Message' button on any dormitory listing to chat with the landlord. This is a great way to ask questions and clarify details before booking.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "How do I write a review?",
            "After your stay, go to the dormitory's detail page and tap 'Write a Review'. Share your experience honestly to help other students make informed decisions.",
            FAQItem.TYPE_QUESTION
        ));

        // For Landlords
        items.add(new FAQItem("For Landlords", "", FAQItem.TYPE_CATEGORY));

        items.add(new FAQItem(
            "How do I add my dormitory listing?",
            "1. Tap 'Add Dormitory' from your home screen\n2. Fill in all required information\n3. Upload high-quality photos\n4. Set your location on the map\n5. Add amenities and house rules\n6. Publish your listing",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "How many photos can I upload?",
            "You can upload multiple photos (up to 10) for each dormitory. High-quality, well-lit photos attract more students, so include pictures of rooms, common areas, and facilities.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "Can I edit my dormitory listing?",
            "Yes! Go to 'My Dormitories', select the listing you want to edit, and tap the edit icon. You can update any information including photos, price, and availability.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "How do I manage booking requests?",
            "Check 'Booking Requests' regularly to see new applications. Review each student's profile, approve or decline the request, and communicate through the messaging feature.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "What if I need to temporarily close my dormitory?",
            "You can toggle the availability status of your dormitory in 'My Dormitories'. This will hide it from search results without deleting the listing.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "How do I respond to reviews?",
            "Go to 'Reviews' from your landlord home screen. You can view all reviews and reply to them professionally. Responding shows you care about tenant feedback.",
            FAQItem.TYPE_QUESTION
        ));

        // Payments
        items.add(new FAQItem("Payments & Transactions", "", FAQItem.TYPE_CATEGORY));

        items.add(new FAQItem(
            "How do payments work?",
            "DormFinder facilitates communication between students and landlords. Payment terms and methods should be agreed upon directly between both parties. Always get a written receipt for payments.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "Is payment required when booking?",
            "No, submitting a booking request on DormFinder is free. Payment arrangements are made directly with the landlord after your booking is approved.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "What should I do if there's a payment dispute?",
            "Keep all payment receipts and communication records. Try to resolve issues directly with the other party first. If needed, contact DormFinder support for assistance.",
            FAQItem.TYPE_QUESTION
        ));

        // Safety & Security
        items.add(new FAQItem("Safety & Security", "", FAQItem.TYPE_CATEGORY));

        items.add(new FAQItem(
            "Is my personal information safe?",
            "Yes, we take data security seriously. Your personal information is encrypted and stored securely. We never share your data with third parties without your consent.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "How do I report suspicious activity?",
            "If you encounter suspicious users or listings, contact us immediately at support@dormfinder.com. Include details about the issue and any relevant screenshots.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "Should I visit the property before booking?",
            "Absolutely! Always visit the property in person before making any commitments. Bring someone with you and verify that the property matches the listing.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "What safety tips should I follow?",
            "• Meet in public places first\n• Verify property documents\n• Don't share financial information over messages\n• Trust your instincts\n• Keep records of all communications\n• Use the in-app messaging system",
            FAQItem.TYPE_QUESTION
        ));

        // Technical Issues
        items.add(new FAQItem("Technical Issues", "", FAQItem.TYPE_CATEGORY));

        items.add(new FAQItem(
            "The app is not loading. What should I do?",
            "1. Check your internet connection\n2. Close and restart the app\n3. Clear the app cache in your phone settings\n4. Update to the latest version\n5. If issues persist, contact support",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "Images are not displaying properly",
            "This usually happens due to slow internet connection or cache issues. Try clearing the app cache or switching to a better internet connection.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "I can't send or receive messages",
            "1. Check your internet connection\n2. Verify your account is active\n3. Ensure you have message permissions enabled\n4. Update the app to the latest version\n5. Restart the app",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "Location/Map features are not working",
            "Make sure you've granted location permissions to DormFinder in your phone's settings. Also ensure your device's GPS is turned on.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "I forgot my password. How do I reset it?",
            "On the login screen, tap 'Forgot Password?'. Enter your registered email address, and we'll send you instructions to reset your password.",
            FAQItem.TYPE_QUESTION
        ));

        // Account Management
        items.add(new FAQItem("Account Management", "", FAQItem.TYPE_CATEGORY));

        items.add(new FAQItem(
            "How do I update my profile?",
            "Go to the Profile tab in the bottom navigation. Tap the edit icon to update your information, profile picture, and contact details.",
            FAQItem.TYPE_QUESTION
        ));

        items.add(new FAQItem(
            "Can I change my account type (student/landlord)?",
            "Currently, you cannot change your account type directly. If you need to switch, please contact support at support@dormfinder.com.",
            FAQItem.TYPE_QUESTION
        ));

        // Contact Support
        items.add(new FAQItem("Still Need Help?", "", FAQItem.TYPE_CATEGORY));

        items.add(new FAQItem(
            "How can I contact support?",
            "You can reach us through:\n• Email: support@dormfinder.com\n• Phone: +63 123 456 7890\n• In-app Help & Guide\n\nOur support team responds within 24 hours on business days (Monday-Friday, 9AM-6PM).",
            FAQItem.TYPE_QUESTION
        ));

        return items;
    }
}
