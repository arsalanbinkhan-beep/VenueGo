package com.arsalankhan.venuego;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
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
import com.google.android.gms.maps.SupportMapFragment;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVenueListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        venueService = new VenueService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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

        // Default to list view
        switchView("list");

        // Setup RecyclerView
        venueAdapter = new VenueAdapter(venues, this, true); // true for detailed view
        binding.recyclerViewVenues.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewVenues.setAdapter(venueAdapter);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

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
        }
    }

    private void loadVenues() {
        // Check if venues were passed in intent (from AI recommendations)
        List<Venue> recommendedVenues = (List<Venue>) getIntent().getSerializableExtra("venues");
        if (recommendedVenues != null && !recommendedVenues.isEmpty()) {
            venues.clear();
            venues.addAll(recommendedVenues);
            updateUI();
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

    private void loadVenuesNearUser() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            venueService.searchVenuesNearby(location.getLatitude(), location.getLongitude(),
                                    20.0, new VenueService.VenueListCallback() {
                                        @Override
                                        public void onSuccess(List<Venue> venueList) {
                                            venues.clear();
                                            venues.addAll(venueList);
                                            updateUI();
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            Toast.makeText(VenueListActivity.this,
                                                    "Error loading venues: " + error, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadVenuesWithFilters(Map<String, Object> filters) {
        venueService.searchVenues(filters, new VenueService.VenueListCallback() {
            @Override
            public void onSuccess(List<Venue> venueList) {
                venues.clear();
                venues.addAll(venueList);
                updateUI();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(VenueListActivity.this,
                        "Error loading venues: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (venues.isEmpty()) {
            showEmptyState();
        } else {
            showVenuesList();
            venueAdapter.notifyDataSetChanged();

            // Update map if available
            if (googleMap != null) {
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
                    .snippet(venue.getAddress())
                    .icon(getVenueIcon(venue.getCategory())));

            markerVenueMap.put(marker, venue);
        }
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
            Venue firstVenue = venues.get(0);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(firstVenue.getLatitude(), firstVenue.getLongitude()), 12));
        }
    }

    private com.google.android.gms.maps.model.BitmapDescriptor getVenueIcon(String category) {
        switch (category) {
            case "banquet_hall":
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_banquet);
            case "hotel":
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_hotel);
            case "restaurant":
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_restaurant);
            case "open_ground":
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_park);
            case "stadium":
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_stadium);
            default:
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_venue);
        }
    }

    private void showVenueInfoWindow(Venue venue, Marker marker) {
        marker.showInfoWindow();
        // Highlight the venue in list view
        int position = venues.indexOf(venue);
        if (position >= 0) {
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

            if (googleMap != null && !venues.isEmpty()) {
                addVenueMarkers();
                zoomToVenues();
            }
        }
    }

    private void showFilterDialog() {
        // Implement filter dialog
        Toast.makeText(this, "Filter feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showEmptyState() {
        binding.recyclerViewVenues.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setText("No venues found matching your criteria");
        binding.btnExplore.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateEventActivity.class));
            finish();
        });
    }

    private void showVenuesList() {
        binding.recyclerViewVenues.setVisibility(View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}