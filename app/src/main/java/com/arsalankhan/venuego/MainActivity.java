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
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    private VenueAdapter trendingAdapter;
    private List<Venue> nearbyVenues = new ArrayList<>();
    private List<Venue> trendingVenues = new ArrayList<>();
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        venueService = new VenueService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseHelper = new DatabaseHelper(this);

        // Check authentication
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        setupUI();
        setupRecyclerViews();
        setupBottomNavigation();
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
            Intent intent = new Intent(this, CreateEventActivity.class);
            startActivity(intent);
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

    private void setupRecyclerViews() {
        // Nearby venues adapter
        venueAdapter = new VenueAdapter(nearbyVenues, this, false);
        binding.recyclerViewNearby.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewNearby.setAdapter(venueAdapter);

        // Trending venues adapter
        trendingAdapter = new VenueAdapter(trendingVenues, this, false);
        binding.recyclerViewTrending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewTrending.setAdapter(trendingAdapter);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigationBar;
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_search) {
                Intent intent = new Intent(this, SearchFilterActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (itemId == R.id.nav_favorites) {
                Intent intent = new Intent(this, FavoritesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (itemId == R.id.nav_booking) {
                Intent intent = new Intent(this, BookingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void getUserLocationAndLoadVenues() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            loadNearbyVenues(location);
                        } else {
                            // Try to get cached location from local DB
                            loadNearbyVenuesFromDB();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Location error: " + e.getMessage());
                        loadNearbyVenuesFromDB();
                    });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            loadDefaultVenues();
        }
    }

    private void loadNearbyVenues(Location location) {
        // Try local database first
        List<Venue> localVenues = databaseHelper.getVenuesNearby(
                location.getLatitude(),
                location.getLongitude(),
                20.0
        );

        if (!localVenues.isEmpty()) {
            nearbyVenues.clear();
            nearbyVenues.addAll(localVenues);
            venueAdapter.notifyDataSetChanged();
            binding.tvNoNearby.setVisibility(View.GONE);
        } else {
            // Fallback to Firestore
            venueService.searchVenuesNearby(location.getLatitude(), location.getLongitude(),
                    20.0, new VenueService.VenueListCallback() {
                        @Override
                        public void onSuccess(List<Venue> venues) {
                            nearbyVenues.clear();
                            nearbyVenues.addAll(venues);
                            venueAdapter.notifyDataSetChanged();

                            if (venues.isEmpty()) {
                                binding.tvNoNearby.setVisibility(View.VISIBLE);
                            } else {
                                binding.tvNoNearby.setVisibility(View.GONE);
                                // Cache locally
                                cacheVenuesLocally(venues);
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e("MainActivity", "Error loading nearby venues: " + error);
                            binding.tvNoNearby.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    private void loadNearbyVenuesFromDB() {
        List<Venue> venues = databaseHelper.getAllVenues();
        if (!venues.isEmpty()) {
            nearbyVenues.clear();
            nearbyVenues.addAll(venues.subList(0, Math.min(10, venues.size())));
            venueAdapter.notifyDataSetChanged();
            binding.tvNoNearby.setVisibility(View.GONE);
        } else {
            loadDefaultVenues();
        }
    }

    private void loadDefaultVenues() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("city", "Mumbai");
        filters.put("limit", 10);

        venueService.searchVenues(filters, new VenueService.VenueListCallback() {
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
                binding.tvNoNearby.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadTrendingVenues() {
        // Try local database first
        List<Venue> localTrending = databaseHelper.getTrendingVenues(10);

        if (!localTrending.isEmpty()) {
            trendingVenues.clear();
            trendingVenues.addAll(localTrending);
            trendingAdapter.notifyDataSetChanged();
        } else {
            // Fallback to Firestore
            venueService.getTrendingVenues(new VenueService.VenueListCallback() {
                @Override
                public void onSuccess(List<Venue> venues) {
                    trendingVenues.clear();
                    trendingVenues.addAll(venues);
                    trendingAdapter.notifyDataSetChanged();

                    // Cache locally
                    cacheVenuesLocally(venues);
                }

                @Override
                public void onFailure(String error) {
                    Log.e("MainActivity", "Error loading trending venues: " + error);
                }
            });
        }
    }

    private void cacheVenuesLocally(List<Venue> venues) {
        new Thread(() -> {
            for (Venue venue : venues) {
                databaseHelper.insertVenue(venue);
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
        } else {
            binding.bottomNavigationBar.setSelectedItemId(R.id.nav_home);
            refreshData();
        }
    }

    private void refreshData() {
        getUserLocationAndLoadVenues();
        loadTrendingVenues();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}