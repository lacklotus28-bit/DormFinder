package com.rct.dormfinder.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.User;
import com.rct.dormfinder.utils.ConfirmationDialogHelper;
import com.rct.dormfinder.utils.ImageCompressor;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends BaseActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView ivProfileImage, ivEditProfile;
    private TextView tvUserName, tvUserEmail, tvUserType, tvMemberSince;
    private EditText etName, etContactNumber, etSchool, etCourse;
    private LinearLayout layoutStudentFields;
    private Button btnSaveProfile;
    private com.google.android.material.card.MaterialCardView btnChangePassword, btnSignOut;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigation;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private User currentUser;
    private String currentUserId;
    private boolean isEditMode = false;
    private Uri selectedImageUri;
    private boolean isUploadingImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        setupFirebase();
        loadUserProfile();
        setupListeners();
        
        // Force apply insets after layout is set
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.post(() -> {
                androidx.core.view.ViewCompat.requestApplyInsets(rootView);
            });
        }
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        Log.d("ProfileActivity", "===== APPLYING INSETS =====");
        Log.d("ProfileActivity", "Top inset: " + insets.top);
        Log.d("ProfileActivity", "Bottom inset: " + insets.bottom);
        
        // No padding on ScrollView - let header extend behind status bar
        ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.setPadding(0, 0, 0, 0);
            Log.d("ProfileActivity", "✅ ScrollView padding cleared for edge-to-edge");
        }
        
        // Header already has paddingTop="48dp" in XML which will be adjusted by system
        // No need to add extra padding programmatically
        
        // Apply bottom insets to bottom navigation
        if (bottomNavigation != null) {
            bottomNavigation.setPadding(
                0,
                0,
                0,
                insets.bottom
            );
            Log.d("ProfileActivity", "✅ Bottom nav bottom padding set to: " + insets.bottom + "px");
        } else {
            Log.e("ProfileActivity", "❌ Bottom navigation is NULL!");
        }
    }

    private void initializeViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage);
        ivEditProfile = findViewById(R.id.ivEditProfile);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserType = findViewById(R.id.tvUserType);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        etName = findViewById(R.id.etName);
        etContactNumber = findViewById(R.id.etContactNumber);
        etSchool = findViewById(R.id.etSchool);
        etCourse = findViewById(R.id.etCourse);
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnSignOut = findViewById(R.id.btnSignOut);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void loadUserProfile() {
        if (currentUserId == null) {
            return;
        }

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                        populateUserInfo();
                            // Setup bottom navigation after user data is loaded
                        setupBottomNavigation();
                    }
                    } else {
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void populateUserInfo() {
        tvUserName.setText(currentUser.getName());
        tvUserEmail.setText(currentUser.getEmail());
        String userType = currentUser.getUserType();
        tvUserType.setText(userType.substring(0, 1).toUpperCase() + userType.substring(1));
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMemberSince.setText("Member since " + sdf.format(new Date(currentUser.getCreatedAt())));

        etName.setText(currentUser.getName());
        etContactNumber.setText(currentUser.getContactNumber());

        if ("student".equals(currentUser.getUserType())) {
            layoutStudentFields.setVisibility(View.VISIBLE);
            etSchool.setText(currentUser.getSchool());
            etCourse.setText(currentUser.getCourse());
        } else {
            layoutStudentFields.setVisibility(View.GONE);
        }

        // Load profile image from Cloudinary URL
        if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentUser.getProfileImageUrl())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivProfileImage);
        } else {
            ivProfileImage.setImageResource(R.drawable.ic_person);
        }

        setEditMode(false);
    }

    private void setupListeners() {
        ivEditProfile.setOnClickListener(v -> toggleEditMode());
        ivProfileImage.setOnClickListener(v -> {
            if (isEditMode) {
                selectImage();
            }
        });
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnSignOut.setOnClickListener(v -> signOut());
    }

    private void toggleEditMode() {
        if (isEditMode) {
            // Switching from edit to view mode - check for unsaved changes
            handleCancelEdit();
        } else {
            setEditMode(true);
        }
    }

    private void handleCancelEdit() {
        // Check if any fields have been modified
        boolean hasChanges = hasUnsavedChanges();

        if (hasChanges) {
            ConfirmationDialogHelper.showLeaveProfileEditDialog(this,
                    new ConfirmationDialogHelper.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            // Reload original data and exit edit mode
                            loadUserProfile();
                            selectedImageUri = null;
                        }

                        @Override
                        public void onCancel() {
                            // Stay in edit mode
                        }
                    });
        } else {
            setEditMode(false);
        }
    }

    private boolean hasUnsavedChanges() {
        if (currentUser == null) return false;

        boolean nameChanged = !etName.getText().toString().trim().equals(currentUser.getName());
        boolean contactChanged = !etContactNumber.getText().toString().trim().equals(
                currentUser.getContactNumber() != null ? currentUser.getContactNumber() : "");
        boolean imageChanged = selectedImageUri != null;

        if ("student".equals(currentUser.getUserType())) {
            boolean schoolChanged = !etSchool.getText().toString().trim().equals(
                    currentUser.getSchool() != null ? currentUser.getSchool() : "");
            boolean courseChanged = !etCourse.getText().toString().trim().equals(
                    currentUser.getCourse() != null ? currentUser.getCourse() : "");
            return nameChanged || contactChanged || schoolChanged || courseChanged || imageChanged;
        }

        return nameChanged || contactChanged || imageChanged;
    }

    private void setEditMode(boolean editMode) {
        isEditMode = editMode;
        
        etName.setEnabled(editMode);
        etContactNumber.setEnabled(editMode);
        etSchool.setEnabled(editMode);
        etCourse.setEnabled(editMode);
        
        btnSaveProfile.setVisibility(editMode ? View.VISIBLE : View.GONE);
        ivEditProfile.setImageResource(editMode ? R.drawable.ic_close : R.drawable.ic_edit);
        
        if (editMode) {
            Toast.makeText(this, "Edit mode enabled. Tap profile image to change photo.", 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            
            // Show preview
            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivProfileImage);
            
            Toast.makeText(this, "Image selected. Click Save to upload.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String school = etSchool.getText().toString().trim();
        String course = etCourse.getText().toString().trim();

        if (!validateInput(name, contactNumber, school, course)) {
            return;
        }

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving...");
        progressBar.setVisibility(View.VISIBLE);

        if (selectedImageUri != null) {
            // Upload image to Cloudinary first
            uploadImageToCloudinary(name, contactNumber, school, course);
        } else {
            // No new image, just save profile data
            saveProfileData(name, contactNumber, school, course, null);
        }
    }

    private boolean validateInput(String name, String contactNumber, String school, String course) {
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }

        if (contactNumber.isEmpty()) {
            etContactNumber.setError("Contact number is required");
            etContactNumber.requestFocus();
            return false;
        }

        if (contactNumber.length() < 10) {
            etContactNumber.setError("Please enter a valid contact number");
            etContactNumber.requestFocus();
            return false;
        }

        if ("student".equals(currentUser.getUserType())) {
            if (school.isEmpty()) {
                etSchool.setError("School is required for students");
                etSchool.requestFocus();
                return false;
            }

            if (course.isEmpty()) {
                etCourse.setError("Course is required for students");
                etCourse.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void uploadImageToCloudinary(String name, String contactNumber, String school, String course) {
        try {
            isUploadingImage = true;
            
            // Compress image first - returns Uri of compressed file
            Uri compressedUri = ImageCompressor.compressImage(this, selectedImageUri);
            
            if (compressedUri == null) {
                throw new Exception("Failed to compress image");
            }

            // Get file path from Uri
            String filePath = compressedUri.getPath();
            if (filePath == null) {
                throw new Exception("Failed to get file path");
            }

            // Upload to Cloudinary
            String folder = "profile_images";
            String publicId = "profile_" + currentUserId + "_" + System.currentTimeMillis();
            
            MediaManager.get().upload(filePath)
                    .option("folder", folder)
                    .option("public_id", publicId)
                    .option("resource_type", "image")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.VISIBLE);
                                btnSaveProfile.setText("Uploading image...");
                            });
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // Progress update if needed
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            runOnUiThread(() -> {
                                String imageUrl = (String) resultData.get("secure_url");
                                isUploadingImage = false;
                                
                                // Delete compressed file
                                try {
                                    File tempFile = new File(filePath);
                                    if (tempFile.exists()) {
                                        tempFile.delete();
                                    }
                                } catch (Exception e) {
                                    Log.e("ProfileActivity", "Error deleting temp file: " + e.getMessage());
                                }
                                
                                // Save profile with new image URL
                                saveProfileData(name, contactNumber, school, course, imageUrl);
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            runOnUiThread(() -> {
                                isUploadingImage = false;
                                progressBar.setVisibility(View.GONE);
                                btnSaveProfile.setEnabled(true);
                                btnSaveProfile.setText("Save Profile");
                                
                                // Delete compressed file
                                try {
                                    File tempFile = new File(filePath);
                                    if (tempFile.exists()) {
                                        tempFile.delete();
                                    }
                                } catch (Exception e) {
                                    Log.e("ProfileActivity", "Error deleting temp file: " + e.getMessage());
                                }
                                
                                Toast.makeText(ProfileActivity.this,
                                        "Failed to upload image: " + error.getDescription(),
                                        Toast.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            // Handle reschedule if needed
                        }
                    })
                    .dispatch();
                    
        } catch (Exception e) {
            isUploadingImage = false;
            progressBar.setVisibility(View.GONE);
            btnSaveProfile.setEnabled(true);
            btnSaveProfile.setText("Save Profile");
            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveProfileData(String name, String contactNumber, String school, String course, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("contactNumber", contactNumber);
        updates.put("updatedAt", System.currentTimeMillis());

        if ("student".equals(currentUser.getUserType())) {
            updates.put("school", school);
            updates.put("course", course);
        }

        if (imageUrl != null) {
            updates.put("profileImageUrl", imageUrl);
        }

        db.collection("users").document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("Save Profile");
                    
                    // Update local user object
                    currentUser.setName(name);
                    currentUser.setContactNumber(contactNumber);
                    if ("student".equals(currentUser.getUserType())) {
                        currentUser.setSchool(school);
                        currentUser.setCourse(course);
                    }
                    if (imageUrl != null) {
                        currentUser.setProfileImageUrl(imageUrl);
                    }
                    
                    // Reset selected image URI
                    selectedImageUri = null;
                    
                    // Update UI
                    populateUserInfo();
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("Save Profile");
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView btnChangePassword = dialogView.findViewById(R.id.btnChangePassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set transparent background for dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnChangePassword.setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            
            changePassword(currentPassword, newPassword, confirmPassword);
            dialog.dismiss();
        });

        dialog.show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Ensure profile tab is highlighted
        if (bottomNavigation != null && currentUser != null) {
            bottomNavigation.post(() -> {
                if ("student".equals(currentUser.getUserType())) {
                    bottomNavigation.setSelectedItemId(R.id.nav_profile);
                } else if ("landlord".equals(currentUser.getUserType())) {
                    bottomNavigation.setSelectedItemId(R.id.nav_profile);
                }
            });
        }
        
        // Only reload if not currently uploading or editing
        if (!isUploadingImage && !isEditMode) {
            loadUserProfile();
        }
    }

    private void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
                    
            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(this, "Password changed successfully!", 
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to change password: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void signOut() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sign_out, null);
        View btnCancelSignOut = dialogView.findViewById(R.id.btnCancelSignOut);
        View btnConfirmSignOut = dialogView.findViewById(R.id.btnConfirmSignOut);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set white background for dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
        }

        btnCancelSignOut.setOnClickListener(v -> dialog.dismiss());

        btnConfirmSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            
            android.content.SharedPreferences prefs = getSharedPreferences("DormFinderPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            
            Intent intent = new Intent(this, com.rct.dormfinder.MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Handle back press - no longer needed with bottom navigation
     */
    private void handleBackPress() {
        if (isEditMode) {
            handleCancelEdit();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }
    
    /**
     * Setup bottom navigation based on user type
     */
    private void setupBottomNavigation() {
        if (currentUser != null && bottomNavigation != null) {
            // Clear any existing menu items first
            bottomNavigation.getMenu().clear();
            
            if ("student".equals(currentUser.getUserType())) {
                bottomNavigation.inflateMenu(R.menu.student_bottom_nav_menu);
                com.rct.dormfinder.utils.NavigationHelper.setupStudentBottomNavigation(
                    this, bottomNavigation, R.id.nav_profile
                );
                // Force selection after setup
                bottomNavigation.post(() -> {
                    bottomNavigation.setSelectedItemId(R.id.nav_profile);
                    Log.d("ProfileActivity", "✅ Profile tab selected for student");
                });
            } else if ("landlord".equals(currentUser.getUserType())) {
                bottomNavigation.inflateMenu(R.menu.landlord_bottom_nav_menu);
                com.rct.dormfinder.utils.NavigationHelper.setupLandlordBottomNavigation(
                    this, bottomNavigation, R.id.nav_profile
                );
                // Force selection after setup
                bottomNavigation.post(() -> {
                    bottomNavigation.setSelectedItemId(R.id.nav_profile);
                    Log.d("ProfileActivity", "✅ Profile tab selected for landlord");
                });
            }
        }
    }
}
