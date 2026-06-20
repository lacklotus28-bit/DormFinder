package com.rct.dormfinder.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.AmenitiesAdapter;
import com.rct.dormfinder.adapters.DormitoryImageAdapter;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.models.User;
import com.rct.dormfinder.utils.FavoritesManager;
import java.text.NumberFormat;
import java.util.Locale;

public class DormitoryDetailActivity extends BaseActivity implements OnMapReadyCallback {
    private ImageView ivBack, ivFavorite, ivLandlordProfile;
    private ViewPager2 viewPagerImages;
    private LinearLayout layoutIndicators;
    private TextView tvDormName, tvCity, tvAddress, tvPrice, tvAvailableRooms;
    private TextView tvDescription, tvLandlordName, tvLandlordContact;
    private Button btnCallLandlord, btnMessage, btnBookNow, btnAddReview, btnSeeAllReviews;
    private RecyclerView recyclerViewAmenities, recyclerViewReviews;
    private TextView tvAverageRating, tvTotalReviews, tvNoReviews;
    private android.widget.RatingBar ratingBarAverage;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FavoritesManager favoritesManager;
    private NumberFormat currencyFormat;
    private com.google.firebase.firestore.ListenerRegistration dormitoryListener;
    private com.google.firebase.firestore.ListenerRegistration reviewsListener;

    private Dormitory dormitory;
    private User currentUser;
    private User landlord;
    private String dormitoryId;
    private String currentUserId;
    private GoogleMap mMap;
    private boolean isLandlordView = false;
    private boolean isFavorite = false;
    private boolean isDormitoryLoaded = false; // Track if dormitory is loaded
    private boolean isGuest = false; // Track if user is anonymous guest

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        initializeFirebase();
        getDormitoryId();
        checkUserTypeAndSetLayout();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to header (custom 30% spacing)
        applyTopInsets(insets, R.id.headerLayout, 0.3f);
        
        // Apply bottom insets to bottom button container (uses global 70% spacing)
        applyBottomInsets(insets, R.id.bottomButtonContainer);
        
        // Calculate header and bottom container heights and apply padding to ScrollView
        ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            findViewById(R.id.headerLayout).post(() -> {
                int headerHeight = findViewById(R.id.headerLayout).getHeight();
                int bottomHeight = findViewById(R.id.bottomButtonContainer).getHeight();
                
                // Apply padding to ScrollView to account for fixed header and bottom buttons
                scrollView.setPadding(
                    scrollView.getPaddingLeft(),
                    headerHeight,
                    scrollView.getPaddingRight(),
                    bottomHeight
                );
            });
        }
    }

    private void getDormitoryId() {
        Intent intent = getIntent();
        dormitoryId = intent.getStringExtra("dormitory_id");

        if (dormitoryId == null) {
            Toast.makeText(this, "Invalid dormitory data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void populateDormitoryInfo() {
        android.util.Log.d("DormitoryDetail", "Populating dormitory info for: " + dormitory.getName());
        
        tvDormName.setText(dormitory.getName());
        tvCity.setText(dormitory.getCity());
        tvAddress.setText(dormitory.getAddress());
        tvPrice.setText(currencyFormat.format(dormitory.getMonthlyPrice()));
        tvAvailableRooms.setText(dormitory.getAvailableRooms() + " rooms");
        tvDescription.setText(dormitory.getDescription());

        // Setup images
        setupImages();

        // Setup amenities
        if (dormitory.getAmenities() != null && !dormitory.getAmenities().isEmpty()) {
            AmenitiesAdapter amenitiesAdapter = new AmenitiesAdapter(dormitory.getAmenities());
            recyclerViewAmenities.setLayoutManager(new LinearLayoutManager(this,
                    LinearLayoutManager.HORIZONTAL, false));
            recyclerViewAmenities.setAdapter(amenitiesAdapter);
        }

        // Update map
        if (mMap != null) {
            LatLng dormLocation = new LatLng(dormitory.getLatitude(), dormitory.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(dormLocation)
                    .title(dormitory.getName()));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dormLocation, 15));
        }

        // Enable/disable booking based on availability (only for student view)
        if (!isLandlordView && btnBookNow != null && dormitory.getAvailableRooms() <= 0) {
            btnBookNow.setEnabled(false);
            btnBookNow.setText("No Rooms Available");
            btnBookNow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor(R.color.gray_text)));
        }
        
        // Load reviews (only for student view)
        if (!isLandlordView) {
            loadReviews();
        }
        
        // Mark dormitory as loaded
        isDormitoryLoaded = true;
        android.util.Log.d("DormitoryDetail", "✅ Dormitory fully loaded and ready");
    }
    
    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
        currentUserId = user != null ? user.getUid() : null;
        isGuest = (user != null && user.isAnonymous());
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        favoritesManager = new FavoritesManager(this);
    }
    
    private void checkUserTypeAndSetLayout() {
        if (currentUserId == null) {
            finish();
            return;
        }
        
        // Get current user data to determine if they're viewing their own dormitory
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        loadDormitoryAndCheckOwnership();
                    } else {
                        setStudentLayout();
                        loadDormitoryAsStudent();
                    }
                })
                .addOnFailureListener(e -> {
                    setStudentLayout();
                    loadDormitoryAsStudent();
                });
    }
    
    private void loadDormitoryAsStudent() {
        // Remove old listener if exists
        if (dormitoryListener != null) {
            dormitoryListener.remove();
        }
        
        // Setup real-time listener for dormitory
        dormitoryListener = db.collection("dormitories").document(dormitoryId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        android.util.Log.e("DormitoryDetail", "Dormitory listener error: " + error.getMessage());
                        Toast.makeText(this, "Failed to load dormitory: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (document != null && document.exists()) {
                        Dormitory oldDormitory = dormitory;
                        dormitory = document.toObject(Dormitory.class);
                        if (dormitory != null) {
                            android.util.Log.d("DormitoryDetail", "Dormitory updated - Rating: " + 
                                    dormitory.getAverageRating() + ", Reviews: " + dormitory.getTotalReviews());
                            
                            // Only reload landlord info if it's the first load
                            if (oldDormitory == null) {
                                populateDormitoryInfo();
                                if (landlord == null) {
                                    loadLandlordInfo();
                                }
                            } else {
                                // Just update the info without reloading everything
                                populateDormitoryInfo();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Dormitory not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }
    
    private void loadLandlordInfo() {
        if (dormitory.getLandlordId() != null) {
            android.util.Log.d("DormitoryDetail", "Loading landlord info for ID: " + dormitory.getLandlordId());
            
            db.collection("users").document(dormitory.getLandlordId())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            landlord = document.toObject(User.class);
                            if (landlord != null) {
                                android.util.Log.d("DormitoryDetail", "Landlord info loaded successfully");
                                if (tvLandlordName != null) {
                                    tvLandlordName.setText(landlord.getName());
                                }
                                if (tvLandlordContact != null) {
                                    tvLandlordContact.setText(landlord.getContactNumber());
                                }
                                // Load landlord's profile image if available
                                loadLandlordProfileImage(landlord.getProfileImageUrl());
                            } else {
                                android.util.Log.w("DormitoryDetail", "Failed to parse landlord document");
                                setDefaultLandlordInfo();
                            }
                        } else {
                            android.util.Log.w("DormitoryDetail", "Landlord document does not exist");
                            setDefaultLandlordInfo();
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("DormitoryDetail", "Failed to load landlord info: " + e.getMessage());
                        if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                            android.util.Log.w("DormitoryDetail", "Permission denied - using fallback landlord info");
                            setDefaultLandlordInfo();
                        } else {
                            Toast.makeText(this, "Failed to load landlord info", Toast.LENGTH_SHORT).show();
                            setDefaultLandlordInfo();
                        }
                    });
        } else {
            setDefaultLandlordInfo();
        }
    }
    
    private void setDefaultLandlordInfo() {
        if (tvLandlordName != null) {
            tvLandlordName.setText("Property Owner");
        }
        if (tvLandlordContact != null) {
            tvLandlordContact.setText("Contact via Message");
        }
        // Disable call button if no contact info
        if (btnCallLandlord != null) {
            btnCallLandlord.setEnabled(false);
            btnCallLandlord.setText("Contact via Message");
        }
    }
    
    private void loadLandlordProfileImage(String imagePath) {
        // Use the class field instead of findViewById
        if (ivLandlordProfile != null) {
            android.util.Log.d("DormitoryDetail", "Loading landlord profile image: " + imagePath);
            
            if (imagePath != null && !imagePath.isEmpty()) {
                // Check if it's a local file path or URL
                if (imagePath.startsWith("/") || imagePath.startsWith("file://")) {
                    // Local file path
                    java.io.File imageFile = new java.io.File(imagePath);
                    if (imageFile.exists()) {
                        com.bumptech.glide.Glide.with(this)
                                .load(imageFile)
                                .placeholder(R.drawable.ic_person)
                                .centerCrop()
                                .into(ivLandlordProfile);
                        android.util.Log.d("DormitoryDetail", "Loaded local landlord profile image successfully");
                    } else {
                        android.util.Log.w("DormitoryDetail", "Landlord profile image file not found: " + imagePath);
                        ivLandlordProfile.setImageResource(R.drawable.ic_person);
                    }
                } else {
                    // URL (for future Firebase Storage integration)
                    com.bumptech.glide.Glide.with(this)
                            .load(imagePath)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .centerCrop()
                            .into(ivLandlordProfile);
                    android.util.Log.d("DormitoryDetail", "Loaded URL landlord profile image successfully");
                }
            } else {
                android.util.Log.d("DormitoryDetail", "No landlord profile image URL, using default");
                ivLandlordProfile.setImageResource(R.drawable.ic_person);
            }
        } else {
            android.util.Log.w("DormitoryDetail", "Landlord profile ImageView not found in layout");
        }
    }
    
    private void setupStudentListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        // Student-specific listeners
        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> {
                if (isGuest) {
                    Toast.makeText(this, "Sign in to book a dormitory", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, com.rct.dormfinder.activities.LoginActivity.class));
                    return;
                }
                if (!isDormitoryLoaded || dormitory == null) {
                    Toast.makeText(this, "Please wait, dormitory data is loading...", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (dormitory.getAvailableRooms() > 0) {
                    // Check for existing bookings before opening booking form
                    checkExistingBookingsBeforeBooking();
                } else {
                    Toast.makeText(this, "No rooms available for booking", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (btnMessage != null) {
            btnMessage.setOnClickListener(v -> {
                if (isGuest) {
                    Toast.makeText(this, "Sign in to message the landlord", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, com.rct.dormfinder.activities.LoginActivity.class));
                    return;
                }
                android.util.Log.d("DormitoryDetail", "Message button clicked");
                android.util.Log.d("DormitoryDetail", "isDormitoryLoaded: " + isDormitoryLoaded);
                android.util.Log.d("DormitoryDetail", "dormitory null: " + (dormitory == null));
                android.util.Log.d("DormitoryDetail", "currentUserId null: " + (currentUserId == null));
                
                // Check if dormitory is loaded
                if (!isDormitoryLoaded || dormitory == null) {
                    android.util.Log.w("DormitoryDetail", "❌ Dormitory not loaded yet");
                    Toast.makeText(this, "Please wait, dormitory data is loading...", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Check if currentUserId is valid
                if (currentUserId == null || currentUserId.isEmpty()) {
                    android.util.Log.e("DormitoryDetail", "❌ Current user ID is null or empty");
                    Toast.makeText(this, "Unable to start chat - user not authenticated", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Check if landlordId is valid
                if (dormitory.getLandlordId() == null || dormitory.getLandlordId().isEmpty()) {
                    android.util.Log.e("DormitoryDetail", "❌ Landlord ID is null or empty");
                    Toast.makeText(this, "Unable to start chat - landlord information not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Check if dormitoryId is valid
                if (dormitoryId == null || dormitoryId.isEmpty()) {
                    android.util.Log.e("DormitoryDetail", "❌ Dormitory ID is null or empty");
                    Toast.makeText(this, "Unable to start chat - dormitory information not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                android.util.Log.d("DormitoryDetail", "✅ All checks passed, starting chat");
                android.util.Log.d("DormitoryDetail", "  currentUserId: " + currentUserId);
                android.util.Log.d("DormitoryDetail", "  landlordId: " + dormitory.getLandlordId());
                android.util.Log.d("DormitoryDetail", "  dormitoryId: " + dormitoryId);
                android.util.Log.d("DormitoryDetail", "  dormitoryName: " + dormitory.getName());
                
                try {
                    String chatId = currentUserId + "_" + dormitory.getLandlordId() + "_" + dormitoryId;
                    Intent chatIntent = new Intent(this, ChatActivity.class);
                    chatIntent.putExtra("chat_id", chatId);
                    chatIntent.putExtra("partner_id", dormitory.getLandlordId());
                    chatIntent.putExtra("dormitory_id", dormitoryId);
                    chatIntent.putExtra("dormitory_name", dormitory.getName());
                    
                    android.util.Log.d("DormitoryDetail", "✅ Starting ChatActivity with chatId: " + chatId);
                    startActivity(chatIntent);
                } catch (Exception e) {
                    android.util.Log.e("DormitoryDetail", "❌ Exception starting chat: " + e.getMessage(), e);
                    Toast.makeText(this, "Error starting chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (btnCallLandlord != null) {
            btnCallLandlord.setOnClickListener(v -> {
                if (landlord != null && landlord.getContactNumber() != null) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + landlord.getContactNumber()));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Contact number not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (ivFavorite != null) {
            // Check if already favorited
            isFavorite = favoritesManager.isFavorite(dormitoryId);
            updateFavoriteIcon();
            
            ivFavorite.setOnClickListener(v -> {
                toggleFavorite();
            });
        }
        
        if (btnAddReview != null) {
            btnAddReview.setOnClickListener(v -> {
                if (!isDormitoryLoaded || dormitory == null) {
                    Toast.makeText(this, "Please wait, dormitory data is loading...", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Intent intent = new Intent(this, AddReviewActivity.class);
                intent.putExtra("dormId", dormitoryId);
                intent.putExtra("dormName", dormitory.getName());
                startActivity(intent);
            });
        }
        
        if (btnSeeAllReviews != null) {
            btnSeeAllReviews.setOnClickListener(v -> {
                Intent intent = new Intent(this, AllReviewsActivity.class);
                intent.putExtra("dormitory_id", dormitoryId);
                startActivity(intent);
            });
        }
    }
    
    private void setupLandlordListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        // Edit dormitory button
        Button btnEditDormitory = findViewById(R.id.btnEditDormitory);
        if (btnEditDormitory != null) {
            btnEditDormitory.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddDormitoryActivity.class);
                intent.putExtra("dormitory_id", dormitoryId);
                startActivity(intent);
            });
        }
        
        // Edit dormitory icon
        ImageView ivEditDormitory = findViewById(R.id.ivEditDormitory);
        if (ivEditDormitory != null) {
            ivEditDormitory.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddDormitoryActivity.class);
                intent.putExtra("dormitory_id", dormitoryId);
                startActivity(intent);
            });
        }
        
        // View All Bookings button
        TextView tvViewAllBookings = findViewById(R.id.tvViewAllBookings);
        if (tvViewAllBookings != null) {
            tvViewAllBookings.setOnClickListener(v -> {
                Intent intent = new Intent(this, BookingManagementActivity.class);
                intent.putExtra("dormitory_id", dormitoryId);
                startActivity(intent);
            });
        }
    }
    
    private void loadDormitoryAndCheckOwnership() {
        // Remove old listener if exists
        if (dormitoryListener != null) {
            dormitoryListener.remove();
        }
        
        // Setup real-time listener for dormitory
        dormitoryListener = db.collection("dormitories").document(dormitoryId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        android.util.Log.e("DormitoryDetail", "Dormitory listener error: " + error.getMessage());
                        Toast.makeText(this, "Failed to load dormitory: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (document != null && document.exists()) {
                        Dormitory oldDormitory = dormitory;
                        dormitory = document.toObject(Dormitory.class);
                        if (dormitory != null) {
                            android.util.Log.d("DormitoryDetail", "Dormitory updated - Rating: " + 
                                    dormitory.getAverageRating() + ", Reviews: " + dormitory.getTotalReviews());
                            
                            // Only set layout on first load
                            if (oldDormitory == null) {
                                // Check if current user is the landlord of this dormitory
                                if (currentUserId.equals(dormitory.getLandlordId())) {
                                    setLandlordLayout();
                                } else {
                                    setStudentLayout();
                                }
                                populateDormitoryInfo();
                                if (!isLandlordView) {
                                    loadLandlordInfo(); // Load landlord info for student view
                                    loadReviews(); // Setup reviews listener for student view
                                }
                            } else {
                                // Just update the info without reloading everything
                                populateDormitoryInfo();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Dormitory not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }
    
    private void setLandlordLayout() {
        isLandlordView = true;
        setContentView(R.layout.activity_landlord_dormitory_detail);
        initializeLandlordViews();
        setupLandlordListeners();
        setupMap();
    }
    
    private void setStudentLayout() {
        isLandlordView = false;
        setContentView(R.layout.activity_dormitory_detail);
        initializeStudentViews();
        setupStudentListeners();
        setupMap();
    }

    private void initializeStudentViews() {
        ivBack = findViewById(R.id.ivBack);
        ivFavorite = findViewById(R.id.ivFavorite);
        viewPagerImages = findViewById(R.id.viewPagerImages);
        layoutIndicators = findViewById(R.id.layoutIndicators);
        tvDormName = findViewById(R.id.tvDormName);
        tvCity = findViewById(R.id.tvCity);
        tvAddress = findViewById(R.id.tvAddress);
        tvPrice = findViewById(R.id.tvPrice);
        tvAvailableRooms = findViewById(R.id.tvAvailableRooms);
        tvDescription = findViewById(R.id.tvDescription);
        tvLandlordName = findViewById(R.id.tvLandlordName);
        tvLandlordContact = findViewById(R.id.tvLandlordContact);
        ivLandlordProfile = findViewById(R.id.ivLandlordProfile);
        btnCallLandlord = findViewById(R.id.btnCallLandlord);
        btnMessage = findViewById(R.id.btnMessage);
        btnBookNow = findViewById(R.id.btnBookNow);
        recyclerViewAmenities = findViewById(R.id.recyclerViewAmenities);
        
        // Review views
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        ratingBarAverage = findViewById(R.id.ratingBarAverage);
        btnAddReview = findViewById(R.id.btnAddReview);
        btnSeeAllReviews = findViewById(R.id.btnSeeAllReviews);
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
    }
    
    private void initializeLandlordViews() {
        ivBack = findViewById(R.id.ivBack);
        viewPagerImages = findViewById(R.id.viewPagerImages);
        layoutIndicators = findViewById(R.id.layoutIndicators);
        tvDormName = findViewById(R.id.tvDormName);
        tvCity = findViewById(R.id.tvCity);
        tvAddress = findViewById(R.id.tvAddress);
        tvPrice = findViewById(R.id.tvPrice);
        tvAvailableRooms = findViewById(R.id.tvAvailableRooms);
        tvDescription = findViewById(R.id.tvDescription);
        recyclerViewAmenities = findViewById(R.id.recyclerViewAmenities);
        
        // Landlord-specific views
        ImageView ivEditDormitory = findViewById(R.id.ivEditDormitory);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvTotalBookings = findViewById(R.id.tvTotalBookings);
        Button btnToggleAvailability = findViewById(R.id.btnToggleAvailability);
        Button btnEditDormitory = findViewById(R.id.btnEditDormitory);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // If dormitory data is already loaded, update map
        if (dormitory != null) {
            LatLng dormLocation = new LatLng(dormitory.getLatitude(), dormitory.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(dormLocation)
                    .title(dormitory.getName()));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dormLocation, 15));
        }
    }

    private void setupImages() {
        android.util.Log.d("DormitoryDetail", "Setting up images");
        
        if (dormitory.getImages() != null && !dormitory.getImages().isEmpty()) {
            android.util.Log.d("DormitoryDetail", "Found " + dormitory.getImages().size() + " images");
            
            // Setup ViewPager with images
            DormitoryImageAdapter imageAdapter = new DormitoryImageAdapter(dormitory.getImages(), this);
            viewPagerImages.setAdapter(imageAdapter);
            
            // Setup indicators
            setupImageIndicators(dormitory.getImages().size());
            
            // Log each image path for debugging
            for (int i = 0; i < dormitory.getImages().size(); i++) {
                android.util.Log.d("DormitoryDetail", "Image " + i + ": " + dormitory.getImages().get(i));
            }
        } else {
            android.util.Log.w("DormitoryDetail", "No images found for dormitory");
            // Show placeholder or hide image section
            java.util.List<String> placeholderList = new java.util.ArrayList<>();
            placeholderList.add("placeholder"); // This will trigger the placeholder in adapter
            DormitoryImageAdapter imageAdapter = new DormitoryImageAdapter(placeholderList, this);
            viewPagerImages.setAdapter(imageAdapter);
        }
    }
    
    private void setupImageIndicators(int imageCount) {
        layoutIndicators.removeAllViews();
        
        if (imageCount <= 1) {
            // Don't show indicators for single image
            return;
        }
        
        ImageView[] indicators = new ImageView[imageCount];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 0, 4, 0);
        
        for (int i = 0; i < imageCount; i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageResource(R.drawable.ic_indicator_inactive);
            indicators[i].setLayoutParams(params);
            layoutIndicators.addView(indicators[i]);
        }
        
        // Set first indicator as active
        if (imageCount > 0) {
            indicators[0].setImageResource(R.drawable.ic_indicator_active);
        }
        
        // Setup ViewPager change listener
        viewPagerImages.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                for (int i = 0; i < imageCount; i++) {
                    indicators[i].setImageResource(
                            i == position ? R.drawable.ic_indicator_active : R.drawable.ic_indicator_inactive
                    );
                }
            }
        });
    }
    
    private void toggleFavorite() {
        if (isGuest) {
            Toast.makeText(this, "Sign in to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFavorite) {
            favoritesManager.removeFavorite(dormitoryId);
            isFavorite = false;
            Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
        } else {
            favoritesManager.addFavorite(dormitoryId);
            isFavorite = true;
            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        }
        animateFavoriteIcon();
    }
    
    private void updateFavoriteIcon() {
        if (ivFavorite != null) {
            if (isFavorite) {
                // Use filled white heart
                ivFavorite.setImageResource(R.drawable.ic_favorite_filled);
                ivFavorite.setColorFilter(getColor(R.color.white));
                ivFavorite.setAlpha(1.0f);
            } else {
                // Use outline white heart with transparency
                ivFavorite.setImageResource(R.drawable.ic_favorite_border);
                ivFavorite.setColorFilter(getColor(R.color.white));
                ivFavorite.setAlpha(0.7f);
            }
        }
    }
    
    private void animateFavoriteIcon() {
        if (ivFavorite != null) {
            // Scale animation
            ivFavorite.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(100)
                .withEndAction(() -> {
                    updateFavoriteIcon();
                    ivFavorite.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start();
                })
                .start();
        }
    }
    
    private void loadReviews() {
        if (dormitoryId == null) return;
        
        // Remove old listener if exists
        if (reviewsListener != null) {
            reviewsListener.remove();
        }
        
        // Setup real-time listener for reviews
        reviewsListener = db.collection("reviews")
                .whereEqualTo("dormId", dormitoryId)
                .orderBy("datePosted", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        android.util.Log.e("DormitoryDetail", "Failed to load reviews: " + error.getMessage());
                        if (tvNoReviews != null) {
                            tvNoReviews.setVisibility(android.view.View.VISIBLE);
                        }
                        return;
                    }
                    
                    if (queryDocumentSnapshots != null) {
                        java.util.List<com.rct.dormfinder.models.Review> reviews = new java.util.ArrayList<>();
                        
                        for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                            com.rct.dormfinder.models.Review review = queryDocumentSnapshots.getDocuments()
                                    .get(i).toObject(com.rct.dormfinder.models.Review.class);
                            if (review != null) {
                                reviews.add(review);
                            }
                        }
                        
                        displayReviews(reviews);
                    }
                });
    }
    
    private void displayReviews(java.util.List<com.rct.dormfinder.models.Review> reviews) {
        if (reviews.isEmpty()) {
            if (tvNoReviews != null) {
                tvNoReviews.setVisibility(android.view.View.VISIBLE);
            }
            if (recyclerViewReviews != null) {
                recyclerViewReviews.setVisibility(android.view.View.GONE);
            }
        } else {
            if (tvNoReviews != null) {
                tvNoReviews.setVisibility(android.view.View.GONE);
            }
            if (recyclerViewReviews != null) {
                recyclerViewReviews.setVisibility(android.view.View.VISIBLE);
                
                com.rct.dormfinder.adapters.ReviewAdapter reviewAdapter = 
                        new com.rct.dormfinder.adapters.ReviewAdapter(reviews, this);
                recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
                recyclerViewReviews.setAdapter(reviewAdapter);
            }
        }
        
        // Update rating display
        updateRatingDisplay();
    }
    
    private void updateRatingDisplay() {
        if (dormitory == null) {
            android.util.Log.w("DormitoryDetail", "Cannot update rating - dormitory is null");
            return;
        }
        
        float avgRating = dormitory.getAverageRating();
        int totalReviews = dormitory.getTotalReviews();
        
        android.util.Log.d("DormitoryDetail", "Updating rating display - Rating: " + avgRating + ", Reviews: " + totalReviews);
        
        if (tvAverageRating != null) {
            tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", avgRating));
        }
        
        if (ratingBarAverage != null) {
            ratingBarAverage.setRating(avgRating);
        }
        
        if (tvTotalReviews != null) {
            tvTotalReviews.setText(totalReviews + (totalReviews == 1 ? " review" : " reviews"));
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Real-time listeners will handle updates automatically
        // No need to manually reload
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners to prevent memory leaks
        if (dormitoryListener != null) {
            dormitoryListener.remove();
        }
        if (reviewsListener != null) {
            reviewsListener.remove();
        }
    }
    
    /**
     * Check if user already has an active booking before allowing new booking
     * STEP 1: Global check for approved/paid/confirmed across ALL dorms
     * STEP 2: Per-dormitory check for pending for THIS dorm only
     */
    private void checkExistingBookingsBeforeBooking() {
        if (currentUserId == null || dormitoryId == null) {
            Toast.makeText(this, "Unable to process booking", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.util.Log.d("DormitoryDetail", "Checking existing bookings for user: " + currentUserId);
        
        // Show loading
        if (btnBookNow != null) {
            btnBookNow.setEnabled(false);
            btnBookNow.setText("Checking...");
        }
        
        // STEP 1: Check for paid/confirmed bookings across ALL dormitories
        db.collection("bookings")
                .whereEqualTo("studentId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasPaidOrConfirmedBooking = false;
                    String globalBlockingStatus = "";
                    String blockedDormName = "";
                    
                    // Check for paid/confirmed bookings across ALL dorms
                    for (int i = 0; i < querySnapshot.size(); i++) {
                        String status = querySnapshot.getDocuments().get(i).getString("status");
                        String checkDormId = querySnapshot.getDocuments().get(i).getString("dormitoryId");
                        
                        android.util.Log.d("DormitoryDetail", "Found booking for dorm " + checkDormId + " with status: " + status);
                        
                        // CRITICAL: Block if user has approved/paid/confirmed booking in ANY dorm
                        if ("approved".equals(status) || "paid".equals(status) || "confirmed".equals(status)) {
                            hasPaidOrConfirmedBooking = true;
                            globalBlockingStatus = status;
                            blockedDormName = querySnapshot.getDocuments().get(i).getString("dormitoryName");
                            android.util.Log.d("DormitoryDetail", "GLOBAL BLOCK: Found " + status + " booking at " + blockedDormName);
                            break;
                        }
                    }
                    
                    if (hasPaidOrConfirmedBooking) {
                        // Reset button
                        if (btnBookNow != null) {
                            btnBookNow.setEnabled(true);
                            btnBookNow.setText("Book Now");
                        }
                        showGlobalBookingBlockDialog(globalBlockingStatus, blockedDormName);
                        return;
                    }
                    
                    // STEP 2: No paid/confirmed found globally, check THIS specific dormitory for pending/approved
                    checkSpecificDormitoryBooking();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DormitoryDetail", "Failed to check global bookings: " + e.getMessage());
                    
                    // Reset button
                    if (btnBookNow != null) {
                        btnBookNow.setEnabled(true);
                        btnBookNow.setText("Book Now");
                    }
                    
                    Toast.makeText(this, "Error checking bookings. Please try again.", 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Check for pending/approved bookings for this specific dormitory
     */
    private void checkSpecificDormitoryBooking() {
        db.collection("bookings")
                .whereEqualTo("studentId", currentUserId)
                .whereEqualTo("dormitoryId", dormitoryId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasActiveBooking = false;
                    String existingStatus = "";
                    
                    for (int i = 0; i < querySnapshot.size(); i++) {
                        String status = querySnapshot.getDocuments().get(i).getString("status");
                        android.util.Log.d("DormitoryDetail", "Found booking for THIS dorm with status: " + status);
                        
                        // Check for pending only (approved/paid/confirmed already handled globally)
                        if ("pending".equals(status)) {
                            hasActiveBooking = true;
                            existingStatus = status;
                            break;
                        }
                    }
                    
                    android.util.Log.d("DormitoryDetail", hasActiveBooking ? 
                            "Found booking for THIS dorm with status: " + existingStatus : 
                            "No active booking found, proceeding with booking");
                    
                    // Reset button
                    if (btnBookNow != null) {
                        btnBookNow.setEnabled(true);
                        btnBookNow.setText("Book Now");
                    }
                    
                    if (hasActiveBooking) {
                        showExistingBookingDialog(existingStatus);
                    } else {
                        android.util.Log.d("DormitoryDetail", "No active booking found, proceeding with booking");
                        // No active booking, proceed to booking form
                        Intent bookingIntent = new Intent(this, BookingRequestActivity.class);
                        bookingIntent.putExtra("dormitory_id", dormitoryId);
                        bookingIntent.putExtra("landlord_id", dormitory.getLandlordId());
                        startActivity(bookingIntent);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DormitoryDetail", "Failed to check specific dorm bookings: " + e.getMessage());
                    
                    // Reset button
                    if (btnBookNow != null) {
                        btnBookNow.setEnabled(true);
                        btnBookNow.setText("Book Now");
                    }
                    
                    Toast.makeText(this, "Error checking bookings. Please try again.", 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Show dialog when user has paid/confirmed booking in another dormitory
     */
    private void showGlobalBookingBlockDialog(String status, String dormitoryName) {
        // Inflate custom dialog layout
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_active_booking, null);
        
        // Get views from custom layout
        ImageView ivDialogIcon = dialogView.findViewById(R.id.ivDialogIcon);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDormitoryName = dialogView.findViewById(R.id.tvDormitoryName);
        TextView tvDialogMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnPositive = dialogView.findViewById(R.id.btnPositive);
        Button btnNegative = dialogView.findViewById(R.id.btnNegative);
        
        // Set content based on status
        String title, message;
        if ("confirmed".equals(status)) {
            title = "Active Booking Exists";
            message = "You already have a confirmed booking at " + dormitoryName + ".\n\n" +
                     "You can only have one active booking at a time. If you wish to book a different dormitory, " +
                     "please cancel your current booking first.";
        } else if ("paid".equals(status)) {
            title = "Payment Pending Confirmation";
            message = "You have a paid booking at " + dormitoryName + " awaiting confirmation.\n\n" +
                     "Please wait for the landlord to confirm your payment before booking another dormitory. " +
                     "If you wish to proceed with a different dorm, please cancel your current booking first.";
        } else if ("approved".equals(status)) {
            title = "Booking Awaiting Payment";
            message = "Your booking at " + dormitoryName + " has been approved and is awaiting payment.\n\n" +
                     "Please complete the payment for your current booking before booking another dormitory. " +
                     "If you wish to book a different dorm, please cancel your current booking first.";
        } else {
            title = "Active Booking Exists";
            message = "You already have an active booking at " + dormitoryName + ".\n\n" +
                     "You can only have one active booking at a time.";
        }
        
        tvDialogTitle.setText(title);
        tvDormitoryName.setText(dormitoryName);
        tvDialogMessage.setText(message);
        
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder builder = 
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        // Set transparent background to show rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        // Set button click listeners
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, StudentBookingsActivity.class);
            startActivity(intent);
        });
        
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    /**
     * Show dialog about existing booking for this specific dormitory
     */
    private void showExistingBookingDialog(String status) {
        // Inflate custom dialog layout
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_active_booking, null);
        
        // Get views from custom layout
        ImageView ivDialogIcon = dialogView.findViewById(R.id.ivDialogIcon);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDormitoryName = dialogView.findViewById(R.id.tvDormitoryName);
        TextView tvDialogMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnPositive = dialogView.findViewById(R.id.btnPositive);
        Button btnNegative = dialogView.findViewById(R.id.btnNegative);
        
        // Set content based on status
        String title, message, positiveButtonText;
        
        switch (status) {
            case "pending":
                title = "Pending Booking Request";
                message = "You already have a pending booking request for this dormitory. " +
                         "Please wait for the landlord to review your request.";
                positiveButtonText = "View My Bookings";
                break;
            case "approved":
                title = "Booking Approved!";
                message = "Your booking request has been approved! " +
                         "Please proceed with payment to confirm your booking.";
                positiveButtonText = "Make Payment";
                break;
            default:
                title = "Active Booking";
                message = "You already have an active booking for this dormitory.";
                positiveButtonText = "View My Bookings";
                break;
        }
        
        tvDialogTitle.setText(title);
        tvDormitoryName.setText(dormitory != null ? dormitory.getName() : "This Dormitory");
        tvDialogMessage.setText(message);
        btnPositive.setText(positiveButtonText);
        
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder builder = 
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        // Set transparent background to show rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        // Set button click listeners
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, StudentBookingsActivity.class);
            if ("approved".equals(status)) {
                intent.putExtra("highlightApprovedBookings", true);
            }
            startActivity(intent);
        });
        
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}
