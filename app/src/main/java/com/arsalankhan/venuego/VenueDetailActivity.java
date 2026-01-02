package com.arsalankhan.venuego;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arsalankhan.venuego.databinding.ActivityVenueDetailBinding;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class VenueDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivityVenueDetailBinding binding;
    private AuthService authService;
    private VenueService venueService;
    private Venue venue;
    private GoogleMap googleMap;
    private DecimalFormat priceFormat = new DecimalFormat("₹#,##,###");
    private AmenitiesAdapter amenitiesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVenueDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        venueService = new VenueService();

        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        String venueId = getIntent().getStringExtra("venueId");
        if (venueId == null) {
            finish();
            return;
        }

        setupUI();
        loadVenueDetails(venueId);
        setupMap();
    }

    private void setupUI() {
        binding.toolbarTitle.setText("Venue Details");
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Setup click listeners
        binding.btnBookNow.setOnClickListener(v -> bookVenue());
        binding.btnCall.setOnClickListener(v -> callVenue());
        binding.btnDirection.setOnClickListener(v -> showDirections());
        binding.btnShare.setOnClickListener(v -> shareVenue());
        binding.btnAddToFavorites.setOnClickListener(v -> addToFavorites());

        // Setup amenities recycler view
        amenitiesAdapter = new AmenitiesAdapter(new ArrayList<>());
        binding.recyclerViewAmenities.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewAmenities.setAdapter(amenitiesAdapter);
    }

    private void loadVenueDetails(String venueId) {
        binding.progressBar.setVisibility(View.VISIBLE);

        venueService.getVenueById(venueId, new VenueService.VenueCallback() {
            @Override
            public void onSuccess(Venue venueData) {
                binding.progressBar.setVisibility(View.GONE);
                venue = venueData;
                displayVenueDetails();
            }

            @Override
            public void onFailure(String error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(VenueDetailActivity.this,
                        "Error loading venue: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayVenueDetails() {
        if (venue == null) return;

        binding.tvVenueName.setText(venue.getName());
        binding.tvVenueAddress.setText(venue.getAddress());
        binding.ratingBar.setRating((float) venue.getRating());
        binding.tvRating.setText(String.format("%.1f", venue.getRating()));
        binding.tvReviewCount.setText("(" + venue.getReviewCount() + " reviews)");
        binding.tvCapacity.setText(venue.getCapacity() + " guests");
        binding.tvPrice.setText(priceFormat.format(venue.getPriceRange()));
        binding.tvCategory.setText(getCategoryDisplayName(venue.getCategory()));
        binding.tvType.setText(venue.getType().equals("indoor") ? "Indoor" : "Outdoor");
        binding.tvDescription.setText(venue.getDescription());

        // Load images
        if (venue.getImages() != null && !venue.getImages().isEmpty()) {
            Glide.with(this)
                    .load(venue.getImages().get(0))
                    .placeholder(R.drawable.placeholder_venue_image)
                    .into(binding.ivVenueImage);
        }

        // Display contact info
        if (venue.getContactPhone() != null && !venue.getContactPhone().isEmpty()) {
            binding.tvPhone.setText(venue.getContactPhone());
            binding.layoutPhone.setVisibility(View.VISIBLE);
        }

        if (venue.getContactEmail() != null && !venue.getContactEmail().isEmpty()) {
            binding.tvEmail.setText(venue.getContactEmail());
            binding.layoutEmail.setVisibility(View.VISIBLE);
        }

        // Display amenities
        if (venue.getAmenities() != null && !venue.getAmenities().isEmpty()) {
            amenitiesAdapter.updateData(venue.getAmenities());
            binding.tvAmenitiesLabel.setVisibility(View.VISIBLE);
            binding.recyclerViewAmenities.setVisibility(View.VISIBLE);
        }

        // Update map if ready
        if (googleMap != null) {
            updateMap();
        }
    }

    private String getCategoryDisplayName(String category) {
        switch (category) {
            case "banquet_hall": return "Banquet Hall";
            case "community_center": return "Community Center";
            case "event_venue": return "Event Venue";
            case "open_ground": return "Open Ground";
            case "stadium": return "Stadium";
            case "auditorium": return "Auditorium";
            default: return "Venue";
        }
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (venue != null) {
            updateMap();
        }
    }

    private void updateMap() {
        if (googleMap == null || venue == null) return;

        LatLng venueLocation = new LatLng(venue.getLatitude(), venue.getLongitude());
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(venueLocation)
                .title(venue.getName()));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(venueLocation, 15));
    }

    private void bookVenue() {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("venue", (Serializable) venue);
        startActivity(intent);
    }

    private void callVenue() {
        if (venue.getContactPhone() != null && !venue.getContactPhone().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + venue.getContactPhone()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDirections() {
        if (venue != null) {
            String uri = "http://maps.google.com/maps?daddr=" +
                    venue.getLatitude() + "," + venue.getLongitude();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(intent);
        }
    }

    private void shareVenue() {
        if (venue != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, venue.getName());
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Check out this venue: " + venue.getName() + "\n" +
                            "Address: " + venue.getAddress() + "\n" +
                            "Price: " + priceFormat.format(venue.getPriceRange()) + "\n" +
                            "Download VenueGo app for more details!");
            startActivity(Intent.createChooser(shareIntent, "Share Venue"));
        }
    }

    private void addToFavorites() {
        // Implement add to favorites
        Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}