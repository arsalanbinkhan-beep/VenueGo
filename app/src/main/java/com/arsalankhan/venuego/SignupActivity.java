package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {
    private EditText etSignupName, etSignupEmail, etSignupPassword;
    private Button btnSignup;
    private TextView tvGoToLogin;
    private ProgressBar progressBar;
    private SimpleAuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        authService = new SimpleAuthService();
        authService.testConnection();

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etSignupName = findViewById(R.id.et_signup_name);
        etSignupEmail = findViewById(R.id.et_signup_email);
        etSignupPassword = findViewById(R.id.et_signup_password);
        btnSignup = findViewById(R.id.btn_signup);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnSignup.setOnClickListener(v -> signUpUser());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Password visibility toggle
        etSignupPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etSignupPassword.getRight() - etSignupPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility();
                    v.performClick();
                    return true;
                }
            }
            return false;
        });
    }

    private void signUpUser() {
        String name = etSignupName.getText().toString().trim();
        String email = etSignupEmail.getText().toString().trim();
        String password = etSignupPassword.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showToast("Please fill all fields");
            return;
        }

        if (name.length() < 2) {
            showToast("Name must be at least 2 characters");
            return;
        }

        if (password.length() < 6) {
            showToast("Password must be at least 6 characters");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email address");
            return;
        }

        setLoadingState(true);

        authService.signUp(name, email, password, new SimpleAuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    setLoadingState(false);

                    // FIX: Force logout and redirect to Login
                    authService.logout();

                    // Show success message
                    new androidx.appcompat.app.AlertDialog.Builder(SignupActivity.this)
                            .setTitle("✓ Account Created!")
                            .setMessage("Your VenueGo account has been created successfully!\n\n" +
                                    "A verification email has been sent to:\n" +
                                    email + "\n\n" +
                                    "Please verify your email before logging in.\n\n" +
                                    "Do you want to verify your phone number now?")
                            .setPositiveButton("Add Phone Later", (dialog, which) -> {
                                redirectToLogin(email);
                            })
                            .setNegativeButton("Verify Phone Now", (dialog, which) -> {
                                // Navigate to phone verification
                                Intent intent = new Intent(SignupActivity.this, VerificationActivity.class);
                                intent.putExtra("isPhoneVerification", true);
                                startActivity(intent);
                            })
                            .setCancelable(false)
                            .show();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void redirectToLogin(String email) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("email", email);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean isLoading) {
        runOnUiThread(() -> {
            btnSignup.setEnabled(!isLoading);
            btnSignup.setText(isLoading ? "Creating Account..." : "Sign Up");

            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }

            etSignupName.setEnabled(!isLoading);
            etSignupEmail.setEnabled(!isLoading);
            etSignupPassword.setEnabled(!isLoading);
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void togglePasswordVisibility() {
        if (etSignupPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            etSignupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            etSignupPassword.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_lock, 0, R.drawable.ic_eye_off, 0);
        } else {
            etSignupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etSignupPassword.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_lock, 0, R.drawable.ic_eye, 0);
        }
        etSignupPassword.setSelection(etSignupPassword.getText().length());
    }
}