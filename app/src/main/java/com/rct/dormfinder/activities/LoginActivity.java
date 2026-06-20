package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;

public class LoginActivity extends BaseActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> loginUser());
        
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        tvForgotPassword.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });
    }

    private void showForgotPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        com.google.android.material.textfield.TextInputEditText etResetEmail = 
                dialogView.findViewById(R.id.etResetEmail);
        View btnCancelReset = dialogView.findViewById(R.id.btnCancelReset);
        View btnSendResetLink = dialogView.findViewById(R.id.btnSendResetLink);

        // Pre-fill email if already entered
        String currentEmail = etEmail.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            etResetEmail.setText(currentEmail);
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set transparent background for dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancelReset.setOnClickListener(v -> dialog.dismiss());

        btnSendResetLink.setOnClickListener(v -> {
            String email = etResetEmail.getText().toString().trim();
            
            if (email.isEmpty()) {
                etResetEmail.setError("Please enter your email");
                etResetEmail.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etResetEmail.setError("Please enter a valid email");
                etResetEmail.requestFocus();
                return;
            }

            dialog.dismiss();
            sendPasswordResetEmail(email);
        });

        dialog.show();
    }

    private void sendPasswordResetEmail(String email) {
        // Show loading
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Sending reset link...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    showPasswordResetSuccessDialog(email);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    String errorMessage;
                    String error = e.getMessage().toLowerCase();
                    
                    if (error.contains("no user record") || error.contains("user not found")) {
                        errorMessage = "No account found with this email address";
                    } else if (error.contains("network") || error.contains("connection")) {
                        errorMessage = "Network error. Please check your internet connection";
                    } else if (error.contains("too many requests")) {
                        errorMessage = "Too many attempts. Please try again later";
                    } else {
                        errorMessage = "Failed to send reset email: " + e.getMessage();
                    }
                    
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                });
    }

    private void showPasswordResetSuccessDialog(String email) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_reset_sent, null);
        TextView tvResetEmail = dialogView.findViewById(R.id.tvResetEmail);
        View btnOk = dialogView.findViewById(R.id.btnOk);

        tvResetEmail.setText(email);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set transparent background for dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        // Show loading state
        btnLogin.setText("Signing in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Get user type from Firestore
                    String userId = authResult.getUser().getUid();
                    loadUserAndNavigate(userId);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setText("Login");
                    
                    String errorMessage = getErrorMessage(e.getMessage());
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return false;
        }

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

        return true;
    }

    private void loadUserAndNavigate(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String userType = document.getString("userType");
                        navigateToHome(userType);
                    } else {
                        // Reset button text on error
                        btnLogin.setText("Login");
                        Toast.makeText(this, "User profile not found. Please contact support.", 
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnLogin.setText("Login");
                    Toast.makeText(this, "Failed to load user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToHome(String userType) {
        Intent intent;
        if ("student".equalsIgnoreCase(userType)) {
            intent = new Intent(this, StudentHomeActivity.class);
            android.util.Log.d("LoginActivity", "Navigating to StudentHomeActivity");
        } else if ("landlord".equalsIgnoreCase(userType)) {
            intent = new Intent(this, LandlordHomeActivity.class);
            android.util.Log.d("LoginActivity", "Navigating to LandlordHomeActivity");
        } else {
            Toast.makeText(this, "Invalid user type. Please contact support.", 
                    Toast.LENGTH_LONG).show();
            return;
        }
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private String getErrorMessage(String firebaseError) {
        if (firebaseError == null) {
            return "Login failed. Please try again.";
        }
        
        if (firebaseError.contains("user-not-found") || firebaseError.contains("INVALID_LOGIN_CREDENTIALS")) {
            return "No account found with this email. Please register first.";
        } else if (firebaseError.contains("wrong-password") || firebaseError.contains("INVALID_LOGIN_CREDENTIALS")) {
            return "Incorrect email or password. Please try again.";
        } else if (firebaseError.contains("invalid-email")) {
            return "Please enter a valid email address.";
        } else if (firebaseError.contains("user-disabled")) {
            return "This account has been disabled. Please contact support.";
        } else if (firebaseError.contains("too-many-requests")) {
            return "Too many failed attempts. Please try again later.";
        } else if (firebaseError.contains("network")) {
            return "Network error. Please check your connection and try again.";
        } else {
            return "Login failed. Please check your credentials and try again.";
        }
    }
}
