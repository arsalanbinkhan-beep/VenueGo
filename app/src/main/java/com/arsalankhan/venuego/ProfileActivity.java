package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.arsalankhan.venuego.databinding.ActivityProfileBinding;
import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();

        // Check authentication
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        setupUI();
        loadUserData();
    }

    private void setupUI() {
        // Setup toolbar
        binding.toolbarTitle.setText("My Profile");
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Setup click listeners
        binding.cardEditProfile.setOnClickListener(v -> showToast("Edit Profile feature coming soon"));
        binding.cardMyPreferences.setOnClickListener(v -> showToast("Preferences feature coming soon"));
        binding.cardSettings.setOnClickListener(v -> showToast("Settings feature coming soon"));
        binding.cardHelpSupport.setOnClickListener(v -> showToast("Help & Support feature coming soon"));
        binding.cardAboutApp.setOnClickListener(v -> showToast("About App feature coming soon"));
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserData() {
        authService.getCurrentUserData(new AuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    binding.tvUserName.setText(user.getName());
                    binding.tvUserEmail.setText(user.getEmail());

                    if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                        binding.tvUserPhone.setText(user.getPhone());
                    } else {
                        binding.tvUserPhone.setText("Not set");
                    }

                    // Load profile image with Glide if available
                    // For now, use placeholder
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // Use Firebase user data as fallback
                if (authService.getCurrentUser() != null) {
                    binding.tvUserName.setText(authService.getCurrentUser().getDisplayName() != null ?
                            authService.getCurrentUser().getDisplayName() : "User");
                    binding.tvUserEmail.setText(authService.getCurrentUser().getEmail() != null ?
                            authService.getCurrentUser().getEmail() : "Email not available");
                }
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    authService.logoutWithCallback(new AuthService.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                            redirectToLogin();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(ProfileActivity.this, "Logout failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
        } else {
            loadUserData();
        }
    }
}