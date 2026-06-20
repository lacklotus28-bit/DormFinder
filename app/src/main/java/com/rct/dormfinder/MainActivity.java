package com.rct.dormfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.activities.LoginActivity;
import com.rct.dormfinder.activities.StudentHomeActivity;
import com.rct.dormfinder.activities.LandlordHomeActivity;

public class MainActivity extends AppCompatActivity {
    private Button btnStudentLogin, btnLandlordLogin, btnGuestLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean hasCheckedLogin = false;
    private boolean isRedirecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views first
        initializeViews();
        setupListeners();

        // Add a small delay to ensure Firebase Auth is fully initialized
        new android.os.Handler().postDelayed(() -> {
            checkUserLogin();
        }, 100);
    }

    private void checkUserLogin() {
        if (hasCheckedLogin) {
            android.util.Log.d("MainActivity", "Login already checked, skipping...");
            return;
        }
        
        hasCheckedLogin = true;
        FirebaseUser currentUser = mAuth.getCurrentUser();
        android.util.Log.d("MainActivity", "Checking user login status...");
        
        if (currentUser != null) {
            android.util.Log.d("MainActivity", "User is logged in: " + currentUser.getEmail());
            // User is logged in, redirect to appropriate home screen
            if (currentUser.isAnonymous()) {
                android.util.Log.d("MainActivity", "Guest user detected, going to StudentHomeActivity");
                Intent intent = new Intent(MainActivity.this, com.rct.dormfinder.activities.StudentHomeActivity.class);
                intent.putExtra("is_guest", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                redirectToHomeScreen(currentUser.getUid());
            }
        } else {
            android.util.Log.d("MainActivity", "No user logged in, showing login screen");
            // User not logged in, show login buttons
            showLoginScreen();
        }
    }
    
    private void showLoginScreen() {
        // Reset the login check flag so we can check again if needed
        hasCheckedLogin = false;
        
        // Make sure the login buttons are visible
        if (btnStudentLogin != null && btnLandlordLogin != null) {
            btnStudentLogin.setVisibility(android.view.View.VISIBLE);
            btnLandlordLogin.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void redirectToHomeScreen(String userId) {
        if (isRedirecting) {
            android.util.Log.d("MainActivity", "Already redirecting, ignoring...");
            return;
        }
        
        isRedirecting = true;
        android.util.Log.d("MainActivity", "Redirecting user to home screen, userID: " + userId);
        
        // Check user type from Firestore and redirect accordingly
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String userType = document.getString("userType");
                        android.util.Log.d("MainActivity", "User type: " + userType);
                        
                        Intent intent;
                        
                        if ("Student".equals(userType) || "student".equals(userType)) {
                            intent = new Intent(MainActivity.this, StudentHomeActivity.class);
                            android.util.Log.d("MainActivity", "Redirecting to StudentHomeActivity");
                        } else if ("Landlord".equals(userType) || "landlord".equals(userType)) {
                            intent = new Intent(MainActivity.this, LandlordHomeActivity.class);
                            android.util.Log.d("MainActivity", "Redirecting to LandlordHomeActivity");
                        } else {
                            android.util.Log.w("MainActivity", "Unknown user type: " + userType + ", showing login screen");
                            isRedirecting = false;
                            showLoginScreen();
                            return;
                        }
                        
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        android.util.Log.w("MainActivity", "User document does not exist, showing login screen");
                        isRedirecting = false;
                        showLoginScreen();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MainActivity", "Failed to get user type: " + e.getMessage());
                    isRedirecting = false;
                    showLoginScreen();
                });
    }

    private void initializeViews() {
        btnStudentLogin = findViewById(R.id.btnStudentLogin);
        btnLandlordLogin = findViewById(R.id.btnLandlordLogin);
        btnGuestLogin = findViewById(R.id.btnGuestLogin);
    }

    private void setupListeners() {
        btnStudentLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.putExtra("userType", "Student");
            startActivity(intent);
        });

        btnLandlordLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.putExtra("userType", "Landlord");
            startActivity(intent);
        });

        if (btnGuestLogin != null) {
            btnGuestLogin.setOnClickListener(v -> {
                btnGuestLogin.setEnabled(false);
                btnGuestLogin.setText("Loading...");

                mAuth.signInAnonymously()
                    .addOnSuccessListener(authResult -> {
                        android.util.Log.d("MainActivity", "Signed in anonymously as guest");
                        Intent intent = new Intent(MainActivity.this, com.rct.dormfinder.activities.StudentHomeActivity.class);
                        intent.putExtra("is_guest", true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("MainActivity", "Anonymous sign-in failed: " + e.getMessage());
                        android.widget.Toast.makeText(
                            MainActivity.this,
                            "Failed to continue as guest. Please try again.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show();
                        btnGuestLogin.setEnabled(true);
                        btnGuestLogin.setText("Continue as Guest");
                    });
            });
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("MainActivity", "onResume called - hasCheckedLogin: " + hasCheckedLogin);
        
        // Only show login screen if we haven't already redirected a logged-in user
        if (!hasCheckedLogin) {
            checkUserLogin();
        }
    }
}
