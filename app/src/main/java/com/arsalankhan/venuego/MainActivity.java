package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.arsalankhan.venuego.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();

        setupUI();
        setupClickListeners();
        checkUserAuthentication();
    }

    private void setupUI() {
        // Setup toolbar icons
        binding.iconProfile.setOnClickListener(v -> {
            if (authService.isUserLoggedIn()) {
                // Go to profile
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        });

        binding.iconBell.setOnClickListener(v -> {
            // Handle notifications
        });
    }

    private void setupClickListeners() {
        binding.btnStartNewPlan.setOnClickListener(v -> {
            startActivity(new Intent(this, SearchFilterActivity.class));
        });

        // Setup trending venues
        setupTrendingVenues();
    }

    private void checkUserAuthentication() {
        if (!authService.isUserLoggedIn()) {
            binding.greetingUser.setText("Hello, Guest!");
        } else {
            // Fetch user data and update greeting
            String userName = authService.getCurrentUser().getDisplayName();
            if (userName != null && !userName.isEmpty()) {
                binding.greetingUser.setText("Hello, " + userName + "!");
            }
        }
    }

    private void setupTrendingVenues() {
        // This will be populated from Firestore
        VenueService venueService = new VenueService();
        venueService.getTrendingVenues(new VenueService.VenueListCallback() {
            @Override
            public void onSuccess(List<Venue> venues) {
                // Populate trending venues
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(MainActivity.this, "Error loading venues", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setupDataIngestion() {
        // You can add a menu item or hidden button to access data ingestion
        // For testing, you can add this:
        binding.iconProfile.setOnLongClickListener(v -> {
            startActivity(new Intent(this, DataIngestionActivity.class));
            return true;
        });
    }
}