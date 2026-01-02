package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin;
    private TextView tvGoToSignup, tvForgotPassword, tvPhoneLogin;
    private SimpleAuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = new SimpleAuthService();

        // If user is already logged in, redirect to MainActivity
        if (authService.isUserLoggedIn()) {
            redirectToMain();
            return;
        }

        initializeViews();
        setupClickListeners();

        // Auto-fill received email from signup
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            String email = intent.getStringExtra("email");
            etLoginEmail.setText(email);
        }
    }

    private void initializeViews() {
        etLoginEmail = findViewById(R.id.et_login_email);
        etLoginPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToSignup = findViewById(R.id.tv_go_to_signup);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);

        // Add this TextView to your layout for phone login
        // Add this after the "Forgot Password?" textview
        // <TextView android:id="@+id/tv_phone_login" ... />
        tvPhoneLogin = findViewById(R.id.tv_phone_login);
    }

    private void redirectToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());

        tvGoToSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = etLoginEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show();
                return;
            }

            authService.resetPassword(email, new SimpleAuthService.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(LoginActivity.this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(LoginActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Phone login option
        if (tvPhoneLogin != null) {
            tvPhoneLogin.setOnClickListener(v -> {
                showPhoneLoginDialog();
            });
        }

        // Password visibility toggle
        etLoginPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etLoginPassword.getRight() - etLoginPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility();
                    v.performClick();
                    return true;
                }
            }
            return false;
        });
    }

    private void loginUser() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        authService.login(email, password, new SimpleAuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Log In");

                    // Check if email is verified
                    if (!authService.isEmailVerified()) {
                        showEmailNotVerifiedDialog();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        redirectToMain();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Log In");
                    Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showEmailNotVerifiedDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Email Not Verified")
                .setMessage("Your email is not verified. Please check your inbox for the verification email.\n\n" +
                        "Do you want to resend the verification email?")
                .setPositiveButton("Resend Email", (dialog, which) -> {
                    authService.sendEmailVerification(new SimpleAuthService.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(LoginActivity.this, "Verification email sent!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(LoginActivity.this, "Failed to send: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Login Anyway", (dialog, which) -> {
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    redirectToMain();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showPhoneLoginDialog() {
        // Create a custom dialog view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_phone_input, null);
        EditText etPhoneNumber = dialogView.findViewById(R.id.et_phone_number);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Login with Phone")
                .setView(dialogView)
                .setPositiveButton("Send OTP", (dialog, which) -> {
                    String phoneNumber = etPhoneNumber.getText().toString().trim();
                    if (phoneNumber.isEmpty() || phoneNumber.length() < 10) {
                        Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Navigate to VerificationActivity for phone login
                    Intent intent = new Intent(this, VerificationActivity.class);
                    intent.putExtra("phoneNumber", phoneNumber);
                    intent.putExtra("isPhoneVerification", false);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void togglePasswordVisibility() {
        if (etLoginPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            etLoginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            etLoginPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, R.drawable.ic_eye_off, 0);
        } else {
            etLoginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etLoginPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, R.drawable.ic_eye, 0);
        }
        etLoginPassword.setSelection(etLoginPassword.getText().length());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authService.isUserLoggedIn()) {
            redirectToMain();
        }
    }
}