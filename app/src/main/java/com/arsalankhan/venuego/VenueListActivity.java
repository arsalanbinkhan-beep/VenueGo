// Enhanced VenueListActivity.java with complete implementation
package com.arsalankhan.venuego;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arsalankhan.venuego.databinding.ActivityVenueListBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VenueListActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivityVenueListBinding binding;
    private AuthService authService;
    private VenueService venueService;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap googleMap;
    private List<Venue> venues = new ArrayList<>();
    private VenueAdapter venueAdapter;
    private Event currentEvent;
    private Map<Marker, Venue> markerVenueMap = new HashMap<>();
    private DatabaseHelper databaseHelper;
    private boolean isMapReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVenueListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        venueService = new VenueService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseHelper = new DatabaseHelper(this);

        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        // Get event from intent
        currentEvent = (Event) getIntent().getSerializableExtra("event");

        setupUI();
        setupMap();
        loadVenues();
    }

    private void setupUI() {
        binding.toolbarTitle.setText("Venues");
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Setup filter button
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());

        // Setup view toggle
        binding.btnListView.setOnClickListener(v -> switchView("list"));
        binding.btnMapView.setOnClickListener(v -> switchView("map"));

        // Setup sort options
        binding.btnSort.setOnClickListener(v -> showSortDialog());

        // Default to list view
        switchView("list");

        // Setup RecyclerView
        venueAdapter = new VenueAdapter(venues, this, true);
        binding.recyclerViewVenues.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewVenues.setAdapter(venueAdapter);

        // Setup empty state
        binding.btnExplore.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateEventActivity.class));
            finish();
        });
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            // Try alternative fragment ID
            mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map_container);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.isMapReady = true;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);

        // Set map click listener
        googleMap.setOnMarkerClickListener(marker -> {
            Venue venue = markerVenueMap.get(marker);
            if (venue != null) {
                showVenueInfoWindow(venue, marker);
                return true;
            }
            return false;
        });

        // Add venue markers if venues are already loaded
        if (!venues.isEmpty()) {
            addVenueMarkers();
            zoomToVenues();
        }
    }

    private void loadVenues() {
        // Check if venues were passed in intent
        ArrayList<Venue> passedVenues = (ArrayList<Venue>) getIntent().getSerializableExtra("venues");
        if (passedVenues != null && !passedVenues.isEmpty()) {
            venues.clear();
            venues.addAll(passedVenues);
            updateUI();
            return;
        }

        // Check if event was passed (AI recommendations)
        if (currentEvent != null) {
            loadAIRecommendedVenues();
            return;
        }

        // Check if filters were passed
        Map<String, Object> filters = (Map<String, Object>) getIntent().getSerializableExtra("filters");
        if (filters != null) {
            loadVenuesWithFilters(filters);
            return;
        }

        // Default: load venues near user location
        loadVenuesNearUser();
    }

    private void loadAIRecommendedVenues() {
        binding.progressBar.setVisibility(View.VISIBLE);

        AIRecommendationService aiService = new AIRecommendationService(this);
        aiService.recommendVenues(currentEvent, new AIRecommendationService.RecommendationCallback() {
            @Override
            public void onSuccess(List<Venue> recommendedVenues) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    venues.clear();
                    venues.addAll(recommendedVenues);
                    updateUI();

                    // Show AI recommendations badge
                    binding.tvAIRecommendations.setVisibility(View.VISIBLE);
                    binding.tvAIRecommendations.setText("AI Recommended (" + venues.size() + " venues)");
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(VenueListActivity.this,
                            "AI recommendations failed: " + error, Toast.LENGTH_SHORT).show();
                    loadVenuesNearUser(); // Fallback
                });
            }
        });
    }

    private void loadVenuesNearUser() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            // Try local database first
                            List<Venue> localVenues = databaseHelper.getVenuesNearby(
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    20.0
                            );

                            if (!localVenues.isEmpty()) {
                                venues.clear();
                                venues.addAll(localVenues);
                                updateUI();
                            } else {
                                // Fallback to Firestore
                                loadVenuesFromFirestore(location);
                            }
                        } else {
                            Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                            loadDefaultVenues();
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            loadDefaultVenues();
        }
    }

    private void loadVenuesFromFirestore(Location location) {
        venueService.searchVenuesNearby(location.getLatitude(), location.getLongitude(),
                20.0, new VenueService.VenueListCallback() {
                    @Override
                    public void onSuccess(List<Venue> venueList) {
                        venues.clear();
                        venues.addAll(venueList);
                        updateUI();

                        // Cache locally
                        cacheVenuesLocally(venueList);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(VenueListActivity.this,
                                "Error loading venues: " + error, Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });
    }

    private void loadVenuesWithFilters(Map<String, Object> filters) {
        binding.progressBar.setVisibility(View.VISIBLE);

        venueService.searchVenues(filters, new VenueService.VenueListCallback() {
            @Override
            public void onSuccess(List<Venue> venueList) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    venues.clear();
                    venues.addAll(venueList);
                    updateUI();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(VenueListActivity.this,
                            "Error loading venues: " + error, Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }

    private void loadDefaultVenues() {
        // Load venues from Mumbai as default
        Map<String, Object> filters = new HashMap<>();
        filters.put("city", "Mumbai");
        filters.put("limit", 20);

        loadVenuesWithFilters(filters);
    }

    private void updateUI() {
        if (venues.isEmpty()) {
            showEmptyState();
        } else {
            showVenuesList();
            venueAdapter.notifyDataSetChanged();

            // Update venue count
            binding.tvVenueCount.setText(venues.size() + " venues found");

            // Update map if available
            if (isMapReady) {
                addVenueMarkers();
                zoomToVenues();
            }
        }
    }

    private void addVenueMarkers() {
        if (googleMap == null) return;

        googleMap.clear();
        markerVenueMap.clear();

        for (Venue venue : venues) {
            LatLng position = new LatLng(venue.getLatitude(), venue.getLongitude());

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(venue.getName())
                    .snippet("₹" + String.format("%.0f", venue.getPriceRange()))
                    .icon(getVenueIcon(venue.getCategory())));

            markerVenueMap.put(marker, venue);
        }
    }

    private com.google.android.gms.maps.model.BitmapDescriptor getVenueIcon(String category) {
        int iconRes;
        switch (category) {
            case "banquet_hall":
                iconRes = R.drawable.ic_banquet;
                break;
            case "hotel":
                iconRes = R.drawable.ic_hotel;
                break;
            case "restaurant":
                iconRes = R.drawable.ic_restaurant;
                break;
            case "open_ground":
                iconRes = R.drawable.ic_park;
                break;
            case "stadium":
                iconRes = R.drawable.ic_stadium;
                break;
            case "auditorium":
                iconRes = R.drawable.ic_auditorium;
                break;
            case "community_center":
                iconRes = R.drawable.ic_community;
                break;
            default:
                iconRes = R.drawable.ic_venue;
        }

        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(iconRes);
    }

    private void zoomToVenues() {
        if (venues.isEmpty() || googleMap == null) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Venue venue : venues) {
            builder.include(new LatLng(venue.getLatitude(), venue.getLongitude()));
        }

        try {
            LatLngBounds bounds = builder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (IllegalStateException e) {
            // Fallback: zoom to first venue
            if (!venues.isEmpty()) {
                Venue firstVenue = venues.get(0);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(firstVenue.getLatitude(), firstVenue.getLongitude()), 12));
            }
        }
    }

    private void showVenueInfoWindow(Venue venue, Marker marker) {
        marker.showInfoWindow();
        // Scroll to venue in list view
        int position = venues.indexOf(venue);
        if (position >= 0 && binding.recyclerViewVenues.getVisibility() == View.VISIBLE) {
            binding.recyclerViewVenues.smoothScrollToPosition(position);
        }
    }

    private void switchView(String viewType) {
        if (viewType.equals("list")) {
            binding.recyclerViewVenues.setVisibility(View.VISIBLE);
            binding.mapContainer.setVisibility(View.GONE);
            binding.btnListView.setSelected(true);
            binding.btnMapView.setSelected(false);
        } else {
            binding.recyclerViewVenues.setVisibility(View.GONE);
            binding.mapContainer.setVisibility(View.VISIBLE);
            binding.btnListView.setSelected(false);
            binding.btnMapView.setSelected(true);

            if (isMapReady && !venues.isEmpty()) {
                addVenueMarkers();
                zoomToVenues();
            }
        }
    }

    private void showFilterDialog() {
        Intent intent = new Intent(this, SearchFilterActivity.class);
        startActivity(intent);
    }

    private void showSortDialog() {
        String[] sortOptions = {"Rating (High to Low)", "Price (Low to High)",
                "Price (High to Low)", "Capacity", "Distance"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Sort By");
        builder.setItems(sortOptions, (dialog, which) -> {
            sortVenues(which);
        });
        builder.show();
    }

    private void sortVenues(int sortOption) {
        switch (sortOption) {
            case 0: // Rating high to low
                venues.sort((v1, v2) -> Double.compare(v2.getRating(), v1.getRating()));
                break;
            case 1: // Price low to high
                venues.sort((v1, v2) -> Double.compare(v1.getPriceRange(), v2.getPriceRange()));
                break;
            case 2: // Price high to low
                venues.sort((v1, v2) -> Double.compare(v2.getPriceRange(), v1.getPriceRange()));
                break;
            case 3: // Capacity
                venues.sort((v1, v2) -> Integer.compare(v2.getCapacity(), v1.getCapacity()));
                break;
        }

        venueAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Sorted", Toast.LENGTH_SHORT).show();
    }

    private void showEmptyState() {
        binding.recyclerViewVenues.setVisibility(View.GONE);
        binding.mapContainer.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.VISIBLE);

        if (currentEvent != null) {
            binding.tvEmptyState.setText("No venues found for your event criteria");
        } else {
            binding.tvEmptyState.setText("No venues found");
        }
    }

    private void showVenuesList() {
        binding.recyclerViewVenues.setVisibility(View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void cacheVenuesLocally(List<Venue> venueList) {
        new Thread(() -> {
            for (Venue venue : venueList) {
                databaseHelper.insertVenue(venue);
            }
        }).start();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}