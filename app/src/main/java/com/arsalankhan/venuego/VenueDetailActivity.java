package com.arsalankhan.venuego;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

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
    private DatabaseHelper databaseHelper;
    private Venue venue;
    private GoogleMap googleMap;
    private DecimalFormat priceFormat = new DecimalFormat("₹#,##,###");
    private AmenitiesAdapter amenitiesAdapter;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVenueDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        venueService = new VenueService();
        databaseHelper = new DatabaseHelper(this);

        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        String venueId = getIntent().getStringExtra("venueId");
        if (venueId == null) {
            Toast.makeText(this, "Venue ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        setupMap();
        loadVenueDetails(venueId);
        checkFavoriteStatus(venueId);
    }

    private void setupUI() {
        binding.toolbarTitle.setText("Venue Details");
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Setup click listeners
        binding.btnBookNow.setOnClickListener(v -> bookVenue());
        binding.btnCall.setOnClickListener(v -> callVenue());
        binding.btnDirection.setOnClickListener(v -> showDirections());
        binding.btnShare.setOnClickListener(v -> shareVenue());
        binding.btnAddToFavorites.setOnClickListener(v -> toggleFavorite());
        binding.btnEmail.setOnClickListener(v -> sendEmail());
        binding.btnCheckAvailability.setOnClickListener(v -> checkAvailability());

        // Setup amenities recycler view
        amenitiesAdapter = new AmenitiesAdapter(new ArrayList<>());
        binding.recyclerViewAmenities.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewAmenities.setAdapter(amenitiesAdapter);
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

    private void loadVenueDetails(String venueId) {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Try local database first
        venue = databaseHelper.getVenue(venueId);

        if (venue != null) {
            binding.progressBar.setVisibility(View.GONE);
            displayVenueDetails();

            // Increment view count
            databaseHelper.incrementVenueViews(venueId);
        } else {
            // Fallback to Firestore
            venueService.getVenueById(venueId, new VenueService.VenueCallback() {
                @Override
                public void onSuccess(Venue venueData) {
                    binding.progressBar.setVisibility(View.GONE);
                    venue = venueData;
                    displayVenueDetails();

                    // Cache locally
                    databaseHelper.insertVenue(venue);

                    // Increment view count
                    databaseHelper.incrementVenueViews(venueId);
                }

                @Override
                public void onFailure(String error) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(VenueDetailActivity.this,
                            "Error: " + error, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }

    private void checkFavoriteStatus(String venueId) {
        String userId = authService.getCurrentUser().getUid();
        isFavorite = databaseHelper.isFavorite(venueId, userId);
        updateFavoriteIcon();
    }

    private void displayVenueDetails() {
        if (venue == null) return;

        binding.tvVenueName.setText(venue.getName());
        binding.tvVenueAddress.setText(venue.getFullAddress());
        binding.ratingBar.setRating((float) venue.getRating());
        binding.tvRating.setText(String.format("%.1f", venue.getRating()));
        binding.tvReviewCount.setText("(" + venue.getReviewCount() + " reviews)");
        binding.tvCapacity.setText(venue.getCapacity() + " guests");
        binding.tvPrice.setText(priceFormat.format(venue.getPriceRange()));
        binding.tvCategory.setText(getCategoryDisplayName(venue.getCategory()));
        binding.tvType.setText(venue.getType() != null && venue.getType().equals("indoor") ? "Indoor" : "Outdoor");
        binding.tvDescription.setText(venue.getDescription() != null ? venue.getDescription() :
                "No description available for this venue.");

        // Load image
        if (venue.getImages() != null && !venue.getImages().isEmpty()) {
            Glide.with(this)
                    .load(venue.getImages().get(0))
                    .placeholder(R.drawable.placeholder_venue_image)
                    .error(R.drawable.placeholder_venue_image)
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

    private void updateMap() {
        if (googleMap == null || venue == null) return;

        LatLng venueLocation = new LatLng(venue.getLatitude(), venue.getLongitude());
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(venueLocation)
                .title(venue.getName()));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(venueLocation, 15));
    }

    private String getCategoryDisplayName(String category) {
        if (category == null) return "Venue";

        switch (category.toLowerCase()) {
            case "banquet_hall": return "Banquet Hall";
            case "community_center": return "Community Center";
            case "event_venue": return "Event Venue";
            case "open_ground": return "Open Ground";
            case "stadium": return "Stadium";
            case "auditorium": return "Auditorium";
            case "restaurant": return "Restaurant";
            case "hotel": return "Hotel";
            case "conference_center": return "Conference Center";
            case "sports_complex": return "Sports Complex";
            default: return "Venue";
        }
    }

    private void bookVenue() {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("venue", (Serializable) venue);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
                            "Address: " + venue.getFullAddress() + "\n" +
                            "Price: " + priceFormat.format(venue.getPriceRange()) + "\n" +
                            "Rating: " + String.format("%.1f", venue.getRating()) + " ⭐\n\n" +
                            "Download VenueGo app to book this venue!");
            startActivity(Intent.createChooser(shareIntent, "Share Venue"));
        }
    }

    private void sendEmail() {
        if (venue.getContactEmail() != null && !venue.getContactEmail().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + venue.getContactEmail()));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about " + venue.getName());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Email not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAvailability() {
        Toast.makeText(this, "Checking availability for " + venue.getName(), Toast.LENGTH_SHORT).show();
        // Navigate to availability calendar
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("venue", (Serializable) venue);
        startActivity(intent);
    }

    private void toggleFavorite() {
        String userId = authService.getCurrentUser().getUid();

        if (isFavorite) {
            databaseHelper.removeFavorite(venue.getId(), userId);
            isFavorite = false;
            Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
        } else {
            databaseHelper.addFavorite(venue.getId(), userId);
            isFavorite = true;
            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        }

        updateFavoriteIcon();
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            binding.btnAddToFavorites.setImageResource(R.drawable.ic_favorite_filled);
            binding.btnAddToFavorites.setColorFilter(getColor(R.color.red_error));
        } else {
            binding.btnAddToFavorites.setImageResource(R.drawable.ic_favorite_border);
            binding.btnAddToFavorites.setColorFilter(getColor(android.R.color.white));
        }
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