package com.arsalankhan.venuego;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
    private DatabaseHelper databaseHelper;
    private Map<Marker, Venue> markerVenueMap = new HashMap<>();
    private boolean isMapReady = false;
    private String currentView = "list"; // "list" or "map"
    private Location userLocation;

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

        setupUI();
        setupMap();
        loadVenues();
        getUserLocation();
    }

    private void setupUI() {
        binding.toolbarTitle.setText("Venues");
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Setup filter button
        binding.btnFilter.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchFilterActivity.class);
            startActivity(intent);
        });

        // Setup view toggle
        binding.btnListView.setOnClickListener(v -> switchView("list"));
        binding.btnMapView.setOnClickListener(v -> switchView("map"));

        // Setup sort button
        binding.btnSort.setOnClickListener(v -> showSortDialog());

        // Default to list view
        switchView("list");

        // Setup RecyclerView
        venueAdapter = new VenueAdapter(venues, this, true);
        binding.recyclerViewVenues.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewVenues.setAdapter(venueAdapter);

        // Setup empty state button
        binding.btnExplore.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateEventActivity.class));
            finish();
        });
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
        this.isMapReady = true;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);

        googleMap.setOnMarkerClickListener(marker -> {
            Venue venue = markerVenueMap.get(marker);
            if (venue != null) {
                showVenueDetails(venue);
                return true;
            }
            return false;
        });

        if (!venues.isEmpty()) {
            addVenueMarkers();
            zoomToVenues();
        }
    }

    private void getUserLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        userLocation = location;
                        if (googleMap != null && location != null) {
                            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.addMarker(new MarkerOptions()
                                    .position(userLatLng)
                                    .title("You are here")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        }
                    });
        } catch (SecurityException e) {
            Log.e("VenueListActivity", "Location permission error", e);
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

        // Check type from intent
        String type = getIntent().getStringExtra("type");
        if (type != null) {
            if (type.equals("nearby")) {
                loadNearbyVenues();
            } else if (type.equals("trending")) {
                loadTrendingVenues();
            }
            return;
        }

        // Default: load from local DB
        loadFromLocalDB();
    }

    private void loadNearbyVenues() {
        binding.progressBar.setVisibility(View.VISIBLE);

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            venueService.searchVenuesNearby(location.getLatitude(), location.getLongitude(),
                                    20.0, new VenueService.VenueListCallback() {
                                        @Override
                                        public void onSuccess(List<Venue> venueList) {
                                            binding.progressBar.setVisibility(View.GONE);
                                            venues.clear();
                                            venues.addAll(venueList);
                                            updateUI();
                                            cacheVenuesLocally(venueList);
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            binding.progressBar.setVisibility(View.GONE);
                                            Toast.makeText(VenueListActivity.this,
                                                    "Error: " + error, Toast.LENGTH_SHORT).show();
                                            loadFromLocalDB();
                                        }
                                    });
                        } else {
                            binding.progressBar.setVisibility(View.GONE);
                            loadFromLocalDB();
                        }
                    });
        } catch (SecurityException e) {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            loadFromLocalDB();
        }
    }

    private void loadTrendingVenues() {
        binding.progressBar.setVisibility(View.VISIBLE);

        venueService.getTrendingVenues(new VenueService.VenueListCallback() {
            @Override
            public void onSuccess(List<Venue> venueList) {
                binding.progressBar.setVisibility(View.GONE);
                venues.clear();
                venues.addAll(venueList);
                updateUI();
            }

            @Override
            public void onFailure(String error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(VenueListActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFromLocalDB() {
        List<Venue> localVenues = databaseHelper.getAllVenues();
        if (!localVenues.isEmpty()) {
            venues.clear();
            venues.addAll(localVenues);
            updateUI();
        } else {
            showEmptyState();
        }
    }

    private void updateUI() {
        if (venues.isEmpty()) {
            showEmptyState();
        } else {
            showVenuesList();
            binding.tvVenueCount.setText(venues.size() + " venues found");
            venueAdapter.notifyDataSetChanged();

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
                    .snippet("₹" + String.format("%,.0f", venue.getPriceRange()))
                    .icon(getVenueIcon(venue.getCategory())));

            markerVenueMap.put(marker, venue);
        }
    }

    private com.google.android.gms.maps.model.BitmapDescriptor getVenueIcon(String category) {
        if (category == null) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
        }

        switch (category.toLowerCase()) {
            case "banquet_hall":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE);
            case "hotel":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
            case "restaurant":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
            case "open_ground":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
            case "stadium":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
            case "auditorium":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
            case "community_center":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
            default:
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
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
            if (!venues.isEmpty()) {
                Venue firstVenue = venues.get(0);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(firstVenue.getLatitude(), firstVenue.getLongitude()), 12));
            }
        }
    }

    private void showVenueDetails(Venue venue) {
        Intent intent = new Intent(this, VenueDetailActivity.class);
        intent.putExtra("venueId", venue.getId());
        startActivity(intent);
    }

    private void switchView(String viewType) {
        currentView = viewType;

        if (viewType.equals("list")) {
            binding.recyclerViewVenues.setVisibility(View.VISIBLE);
            binding.mapContainer.setVisibility(View.GONE);
            binding.btnListView.setBackgroundTintList(getColorStateList(R.color.vibrant_purple));
            binding.btnMapView.setBackgroundTintList(getColorStateList(R.color.light_gray_text));
        } else {
            binding.recyclerViewVenues.setVisibility(View.GONE);
            binding.mapContainer.setVisibility(View.VISIBLE);
            binding.btnListView.setBackgroundTintList(getColorStateList(R.color.light_gray_text));
            binding.btnMapView.setBackgroundTintList(getColorStateList(R.color.vibrant_purple));

            if (isMapReady && !venues.isEmpty()) {
                addVenueMarkers();
                zoomToVenues();
            }
        }
    }

    private void showSortDialog() {
        String[] sortOptions = {"Rating (High to Low)", "Price (Low to High)",
                "Price (High to Low)", "Capacity (High to Low)", "Distance (Nearby)"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            case 3: // Capacity high to low
                venues.sort((v1, v2) -> Integer.compare(v2.getCapacity(), v1.getCapacity()));
                break;
            case 4: // Distance
                if (userLocation != null) {
                    venues.sort((v1, v2) -> {
                        double dist1 = calculateDistance(userLocation.getLatitude(), userLocation.getLongitude(),
                                v1.getLatitude(), v1.getLongitude());
                        double dist2 = calculateDistance(userLocation.getLatitude(), userLocation.getLongitude(),
                                v2.getLatitude(), v2.getLongitude());
                        return Double.compare(dist1, dist2);
                    });
                }
                break;
        }

        venueAdapter.notifyDataSetChanged();

        if (currentView.equals("map") && isMapReady) {
            addVenueMarkers();
        }

        Toast.makeText(this, "Sorted", Toast.LENGTH_SHORT).show();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000.0; // Convert to kilometers
    }

    private void showEmptyState() {
        binding.recyclerViewVenues.setVisibility(View.GONE);
        binding.mapContainer.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setText("No venues found");
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}