package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.arsalankhan.venuego.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();

        // If user is already logged in, redirect to MainActivity
        if (authService.isUserLoggedIn()) {
            redirectToMain();
            return;
        }

        setupClickListeners();
    }

    private void redirectToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> loginUser());

        binding.tvGoToSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });

        binding.tvForgotPassword.setOnClickListener(v -> resetPassword());

        // Password visibility toggle with proper click handling
        binding.etLoginPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (binding.etLoginPassword.getRight() - binding.etLoginPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility();
                    v.performClick(); // Call performClick to handle accessibility
                    return true;
                }
            }
            return false;
        });
    }

    private void loginUser() {
        String email = binding.etLoginEmail.getText().toString().trim();
        String password = binding.etLoginPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnLogin.setEnabled(false);

        authService.login(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                binding.btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                redirectToMain();
            }

            @Override
            public void onFailure(String errorMessage) {
                binding.btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void togglePasswordVisibility() {
        if (binding.etLoginPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            binding.etLoginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            binding.etLoginPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, R.drawable.ic_eye_off, 0);
        } else {
            binding.etLoginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.etLoginPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, R.drawable.ic_eye, 0);
        }
        binding.etLoginPassword.setSelection(binding.etLoginPassword.getText().length());
    }

    private void resetPassword() {
        String email = binding.etLoginEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Send password reset email to " + email + "?")
                .setPositiveButton("Send", (dialog, which) -> sendResetEmail(email))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendResetEmail(String email) {
        authService.resetPassword(email, new AuthService.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(LoginActivity.this, "Failed to send reset email: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if user logged in from another device or session
        if (authService.isUserLoggedIn()) {
            redirectToMain();
        }
    }
}