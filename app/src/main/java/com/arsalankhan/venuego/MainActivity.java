package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.arsalankhan.venuego.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private AuthService authService;
    private VenueService venueService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        venueService = new VenueService();

        // Check authentication first
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        setupUI();
        setupBottomNavigation();
        loadUserData();
        setupDataIngestion();
    }

    private void setupUI() {
        // Setup greeting based on user data
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null && currentUser.getDisplayName() != null) {
            binding.greetingUser.setText("Hello, " + currentUser.getDisplayName() + "!");
        } else {
            binding.greetingUser.setText("Hello, User!");
        }

        // Setup click listeners for profile and bell icons
        binding.iconProfile.setOnClickListener(v -> {
            if (authService.isUserLoggedIn()) {
                // Navigate to profile
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            } else {
                redirectToLogin();
            }
        });

        binding.iconBell.setOnClickListener(v -> {
            // Navigate to notifications
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });

        // Start New Plan button
        binding.btnStartNewPlan.setOnClickListener(v -> {
            startActivity(new Intent(this, SearchFilterActivity.class));
        });
    }

    private void setupBottomNavigation() {
        binding.bottomNavigationBar.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // Already on home, just refresh
                    loadUserData();
                    return true;
                } else if (itemId == R.id.nav_search) {
                    navigateToSearch();
                    return true;
                } else if (itemId == R.id.nav_favorites) {
                    navigateToFavorites();
                    return true;
                } else if (itemId == R.id.nav_booking) {
                    navigateToBookings();
                    return true;
                }
                return false;
            }
        });
    }

    private void navigateToSearch() {
        Intent intent = new Intent(this, SearchFilterActivity.class);
        startActivity(intent);
    }

    private void navigateToFavorites() {
        Intent intent = new Intent(this, FavoritesActivity.class);
        startActivity(intent);
    }

    private void navigateToBookings() {
        Intent intent = new Intent(this, BookingsActivity.class);
        startActivity(intent);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadUserData() {
        if (authService.isUserLoggedIn()) {
            authService.getCurrentUserData(new AuthService.AuthCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user != null && user.getName() != null) {
                        binding.greetingUser.setText("Hello, " + user.getName() + "!");
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    // Use Firebase user display name as fallback
                    FirebaseUser firebaseUser = authService.getCurrentUser();
                    if (firebaseUser != null && firebaseUser.getDisplayName() != null) {
                        binding.greetingUser.setText("Hello, " + firebaseUser.getDisplayName() + "!");
                    }
                }
            });
        }
    }

    private void setupDataIngestion() {
        // Hidden feature: long press on profile icon for admin features
        binding.iconProfile.setOnLongClickListener(v -> {
            if (authService.isUserLoggedIn()) {
                startActivity(new Intent(this, DataIngestionActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check authentication when returning to app
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
        } else {
            // Refresh user data
            loadUserData();
        }
    }

    @Override
    public void onBackPressed() {
        // If not on home screen, navigate to home
        super.onBackPressed();
        if (binding.bottomNavigationBar.getSelectedItemId() != R.id.nav_home) {
            binding.bottomNavigationBar.setSelectedItemId(R.id.nav_home);
        } else {
            // Minimize app if already on home
            moveTaskToBack(true);
        }
    }
}