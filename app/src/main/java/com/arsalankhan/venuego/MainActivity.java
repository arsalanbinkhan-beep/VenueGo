package com.arsalankhan.venuego;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arsalankhan.venuego.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private AuthService authService;
    private VenueService venueService;
    private FusedLocationProviderClient fusedLocationClient;
    private VenueAdapter venueAdapter;
    private List<Venue> nearbyVenues = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        venueService = new VenueService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check authentication and email verification
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (!authService.isEmailVerified()) {
            showEmailVerificationDialog();
        }

        setupUI();
        setupVenuesRecyclerView();
        getUserLocationAndLoadVenues();
        loadTrendingVenues();
    }

    private void setupUI() {
        // Setup greeting based on user data
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null && currentUser.getDisplayName() != null) {
            binding.greetingUser.setText("Hello, " + currentUser.getDisplayName() + "!");
        } else {
            binding.greetingUser.setText("Hello, User!");
        }

        // Setup click listeners
        binding.iconProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        binding.iconBell.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        // Start New Plan button
        binding.btnStartNewPlan.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateEventActivity.class));
        });

        // View All Nearby button
        binding.btnViewAllNearby.setOnClickListener(v -> {
            Intent intent = new Intent(this, VenueListActivity.class);
            intent.putExtra("type", "nearby");
            startActivity(intent);
        });

        // View All Trending button
        binding.btnViewAllTrending.setOnClickListener(v -> {
            Intent intent = new Intent(this, VenueListActivity.class);
            intent.putExtra("type", "trending");
            startActivity(intent);
        });
    }

    private void setupVenuesRecyclerView() {
        venueAdapter = new VenueAdapter(nearbyVenues, this);
        binding.recyclerViewNearby.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewNearby.setAdapter(venueAdapter);

        // Setup trending venues recycler view
        VenueAdapter trendingAdapter = new VenueAdapter(new ArrayList<>(), this);
        binding.recyclerViewTrending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewTrending.setAdapter(trendingAdapter);
    }

    private void getUserLocationAndLoadVenues() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Get nearby venues based on location
                            Map<String, Object> filters = new HashMap<>();
                            filters.put("latitude", location.getLatitude());
                            filters.put("longitude", location.getLongitude());
                            filters.put("radius", 10.0); // 10km radius

                            venueService.searchVenuesNearby(location.getLatitude(), location.getLongitude(), 10.0,
                                    new VenueService.VenueListCallback() {
                                        @Override
                                        public void onSuccess(List<Venue> venues) {
                                            nearbyVenues.clear();
                                            nearbyVenues.addAll(venues);
                                            venueAdapter.notifyDataSetChanged();

                                            if (venues.isEmpty()) {
                                                binding.tvNoNearby.setVisibility(View.VISIBLE);
                                            } else {
                                                binding.tvNoNearby.setVisibility(View.GONE);
                                            }
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            Log.e("MainActivity", "Error loading nearby venues: " + error);
                                            binding.tvNoNearby.setVisibility(View.VISIBLE);
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTrendingVenues() {
        venueService.getTrendingVenues(new VenueService.VenueListCallback() {
            @Override
            public void onSuccess(List<Venue> venues) {
                VenueAdapter trendingAdapter = (VenueAdapter) binding.recyclerViewTrending.getAdapter();
                if (trendingAdapter != null) {
                    trendingAdapter.updateData(venues);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("MainActivity", "Error loading trending venues: " + error);
            }
        });
    }

    private void showEmailVerificationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Email Not Verified")
                .setMessage("Please verify your email address to access all features.")
                .setPositiveButton("Resend Verification", (dialog, which) -> {
                    authService.sendEmailVerification(new AuthService.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, "Verification email sent!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(MainActivity.this, "Failed to send email: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Later", null)
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
        }
    }
}