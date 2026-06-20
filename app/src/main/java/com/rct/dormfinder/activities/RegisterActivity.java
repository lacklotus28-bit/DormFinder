package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.TipsAdapter;
import com.rct.dormfinder.models.TipItem;
import com.rct.dormfinder.models.User;
import com.rct.dormfinder.utils.ConfirmationDialogHelper;

public class RegisterActivity extends BaseActivity {
    private RadioGroup radioGroupUserType;
    private RadioButton rbStudent, rbLandlord;
    private EditText etName, etEmail, etContactNumber, etSchool, etCourse, etPassword, etConfirmPassword;
    private LinearLayout layoutStudentFields;
    private Button btnRegister;
    private TextView tvLogin;
    private RecyclerView rvQuickTips;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupFirebase();
        setupListeners();
    }

    private void initializeViews() {
        radioGroupUserType = findViewById(R.id.radioGroupUserType);
        rbStudent = findViewById(R.id.rbStudent);
        rbLandlord = findViewById(R.id.rbLandlord);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etContactNumber = findViewById(R.id.etContactNumber);
        etSchool = findViewById(R.id.etSchool);
        etCourse = findViewById(R.id.etCourse);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        rvQuickTips = findViewById(R.id.rvQuickTips);

        // Setup tips carousel
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvQuickTips.setLayoutManager(lm);
        setupTipsDashboard(rbStudent.isChecked() ? "student" : "landlord");
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void setupListeners() {
        // Handle user type selection
        radioGroupUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbStudent) {
                layoutStudentFields.setVisibility(View.VISIBLE);
                setupTipsDashboard("student");
            } else {
                layoutStudentFields.setVisibility(View.GONE);
                setupTipsDashboard("landlord");
            }
        });

        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> handleBackToLogin());
    }

    private void setupTipsDashboard(String userType) {
        java.util.List<TipItem> tips = new java.util.ArrayList<>();
        if ("student".equalsIgnoreCase(userType)) {
            tips.add(new TipItem("Search dorms", "Find places by city or school", R.drawable.ic_search));
            tips.add(new TipItem("Use filters", "Price, rooms, amenities, and more", R.drawable.ic_filter));
            tips.add(new TipItem("Map view", "See nearby dorms on a map", R.drawable.ic_map));
            tips.add(new TipItem("Favorites", "Save dorms you like", R.drawable.ic_favorite_filled));
            tips.add(new TipItem("Bookings", "Request to book instantly", R.drawable.ic_booking));
            tips.add(new TipItem("Messages", "Chat with landlords", R.drawable.ic_message));
        } else {
            tips.add(new TipItem("List a dorm", "Add photos, price, and details", R.drawable.ic_home));
            tips.add(new TipItem("Booking requests", "Approve or decline quickly", R.drawable.ic_booking));
            tips.add(new TipItem("Messages", "Reply to student inquiries", R.drawable.ic_message));
            tips.add(new TipItem("Payments", "Track student payments", R.drawable.ic_payment));
            tips.add(new TipItem("Reviews", "See feedback from students", R.drawable.ic_reviews));
            tips.add(new TipItem("Notifications", "Stay updated with alerts", R.drawable.ic_notifications));
        }

        TipsAdapter adapter = new TipsAdapter(this, tips, () -> {
            // Navigate to App Guide
            Intent intent = new Intent(RegisterActivity.this, com.rct.dormfinder.activities.AppGuideActivity.class);
            intent.putExtra("isFirstTime", true);
            startActivity(intent);
        });
        rvQuickTips.setAdapter(adapter);
    }

    private void registerUser() {
        // Get form data
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String userType = rbStudent.isChecked() ? "student" : "landlord";
        String school = etSchool.getText().toString().trim();
        String course = etCourse.getText().toString().trim();

        // Validate input
        if (!validateInput(name, email, contactNumber, password, confirmPassword, userType, school, course)) {
            return;
        }

        // Show loading state
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        // Create Firebase Auth user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        saveUserToFirestore(firebaseUser.getUid(), name, email, contactNumber, userType, school, course);
                    }
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Create Account");
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInput(String name, String email, String contactNumber,
                                  String password, String confirmPassword, String userType,
                                  String school, String course) {
        // Name validation
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }

        // Email validation
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return false;
        }

        // Contact number validation
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

        // Student-specific validation
        if ("student".equals(userType)) {
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

        // Password validation
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void saveUserToFirestore(String userId, String name, String email, String contactNumber,
                                     String userType, String school, String course) {
        User user = new User(userId, email, name, userType);
        user.setContactNumber(contactNumber);

        if ("student".equals(userType)) {
            user.setSchool(school);
            user.setCourse(course);
        }

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                    // Show app guide for first-time users
                    Intent guideIntent = new Intent(this, AppGuideActivity.class);
                    guideIntent.putExtra("isFirstTime", true);
                    guideIntent.putExtra("userType", userType);
                    startActivity(guideIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Create Account");
                    Toast.makeText(this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();

                    // Delete the auth user since Firestore save failed
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        currentUser.delete();
                    }
                });
    }

    /**
     * Handle navigation back to login with confirmation
     */
    private void handleBackToLogin() {
        // Check if user has entered any data
        boolean hasData = !etName.getText().toString().trim().isEmpty() ||
                         !etEmail.getText().toString().trim().isEmpty() ||
                         !etPassword.getText().toString().trim().isEmpty() ||
                         !etContactNumber.getText().toString().trim().isEmpty();

        if (hasData) {
            ConfirmationDialogHelper.showLeaveFormDialog(this, "Registration",
                    new ConfirmationDialogHelper.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        }

                        @Override
                        public void onCancel() {
                            // Stay on registration page
                        }
                    });
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackToLogin();
    }
}
