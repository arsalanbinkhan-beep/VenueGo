package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arsalankhan.venuego.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

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
        setupClickListeners();
        loadUserData();
        setupDataIngestion();

        // Removed: setupTrendingVenues() - because trendingRecyclerView doesn't exist in XML
        // Removed: setupInterests() - because interest buttons don't exist in XML
    }

    private void setupUI() {
        // Setup greeting based on user data
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null && currentUser.getDisplayName() != null) {
            binding.greetingUser.setText("Hello, " + currentUser.getDisplayName() + "!");
        } else {
            binding.greetingUser.setText("Hello, User!");
        }

        // Removed: trendingRecyclerView doesn't exist in XML
        // if (binding.trendingRecyclerView != null) {
        //     binding.trendingRecyclerView.setVisibility(View.GONE);
        // }
    }

    private void setupClickListeners() {
        // Profile icon click
        binding.iconProfile.setOnClickListener(v -> {
            if (authService.isUserLoggedIn()) {
                // Go to profile - create ProfileActivity later
                Toast.makeText(this, "Profile feature coming soon", Toast.LENGTH_SHORT).show();
            } else {
                redirectToLogin();
            }
        });

        // Notification icon click
        binding.iconBell.setOnClickListener(v -> {
            // Handle notifications
            Toast.makeText(this, "Notifications feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // Start New Plan button
        binding.btnStartNewPlan.setOnClickListener(v -> {
            startActivity(new Intent(this, SearchFilterActivity.class));
        });
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

    // Removed setupTrendingVenues() method - Add it back only if you add trendingRecyclerView to XML
    /*
    private void setupTrendingVenues() {
        // Just show a simple message instead of using adapter
        binding.trendingVenuesTitle.setText("Trending Venues");

        venueService.getTrendingVenues(new VenueService.VenueListCallback() {
            @Override
            public void onSuccess(List<Venue> venues) {
                if (venues.isEmpty()) {
                    binding.trendingVenuesTitle.setText("No trending venues found");
                } else {
                    binding.trendingVenuesTitle.setText("Found " + venues.size() + " trending venues");
                    // You can process venues here without adapter
                    processVenuesWithoutAdapter(venues);
                }
            }

            @Override
            public void onFailure(String error) {
                binding.trendingVenuesTitle.setText("Error loading venues");
                Toast.makeText(MainActivity.this, "Error loading venues: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processVenuesWithoutAdapter(List<Venue> venues) {
        // If you want to display venues without RecyclerView/Adapter,
        // you can update TextViews or show in a different way

        // For example, show the first venue name
        if (!venues.isEmpty()) {
            Venue firstVenue = venues.get(0);
            String message = "Top venue: " + firstVenue.getName();
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
    */

    // Modified setupInterests() - Only sets text, no button click listeners
    private void setupInterests() {
        // Only set the title text - buttons don't exist in XML
        binding.interestsTitle.setText("Based on your interests...");

        // Removed button click listeners - buttons don't exist in XML
        /*
        if (binding.interestWedding != null) {
            binding.interestWedding.setOnClickListener(v -> navigateToCategory("wedding"));
        }
        if (binding.interestCorporate != null) {
            binding.interestCorporate.setOnClickListener(v -> navigateToCategory("corporate"));
        }
        if (binding.interestBirthday != null) {
            binding.interestBirthday.setOnClickListener(v -> navigateToCategory("birthday"));
        }
        if (binding.interestHangout != null) {
            binding.interestHangout.setOnClickListener(v -> navigateToCategory("hangout"));
        }
        */
    }

    // Keep this method for future use if you add interest buttons
    private void navigateToCategory(String category) {
        // Navigate to SearchFilterActivity with category
        Intent intent = new Intent(this, SearchFilterActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
        Toast.makeText(this, "Showing " + category + " venues", Toast.LENGTH_SHORT).show();
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
            // Removed: setupTrendingVenues() - because trendingRecyclerView doesn't exist
        }
    }

    @Override
    public void onBackPressed() {
        // Minimize app instead of going back
        moveTaskToBack(true);
    }
}