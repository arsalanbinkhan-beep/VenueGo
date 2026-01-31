package com.arsalankhan.venuego;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchFilterActivity extends AppCompatActivity {

    private EditText etSearchQuery;
    private ChipGroup chipGroupEventType, chipGroupVenueCategory;
    private Switch switchGuests, switchOutdoor;
    private TextView tvLocation, tvGuestCount, tvMaxBudget;
    private SeekBar seekBarGuests, seekBarBudget;
    private Button btnShowVenues, btnUseCurrentLocation;
    private Spinner spinnerCity;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseHelper databaseHelper;
    private AIRecommendationService aiService;

    private String selectedCity = "Mumbai";
    private int guestCount = 100;
    private double maxBudget = 50000;
    private boolean useLocation = true;

    // Lists for dropdowns
    private List<String> cities = Arrays.asList(
            "Mumbai", "Pune", "Nagpur", "Nashik",
            "Aurangabad", "Thane", "Navi Mumbai",
            "Kolhapur", "Solapur", "Amravati"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search); // Updated layout name

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseHelper = new DatabaseHelper(this);
        aiService = new AIRecommendationService(this);

        initializeViews();
        setupEventListeners();
        loadUserLocation();
    }

    private void initializeViews() {
        try {
            // Initialize all views with correct IDs
            spinnerCity = findViewById(R.id.spinner_city);
            tvLocation = findViewById(R.id.tv_location); // This was missing
            etSearchQuery = findViewById(R.id.et_search_venue);
            chipGroupEventType = findViewById(R.id.chip_group_event_type);
            chipGroupVenueCategory = findViewById(R.id.chip_group_venue_category);
            switchGuests = findViewById(R.id.switch_guests);
            switchOutdoor = findViewById(R.id.switch_outdoor);
            tvGuestCount = findViewById(R.id.tv_guests_count);
            tvMaxBudget = findViewById(R.id.tv_max_budget);
            seekBarGuests = findViewById(R.id.seekBar_guests);
            seekBarBudget = findViewById(R.id.seekBar_budget);
            btnShowVenues = findViewById(R.id.btn_show_venues);
            btnUseCurrentLocation = findViewById(R.id.btn_use_current_location);

            // Setup city spinner
            ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, cities);
            cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCity.setAdapter(cityAdapter);

            // Set default city
            int defaultPosition = cities.indexOf("Mumbai");
            if (defaultPosition >= 0) {
                spinnerCity.setSelection(defaultPosition);
            }

            // Setup seek bars
            seekBarGuests.setMax(1000); // 10 to 1000 guests
            seekBarGuests.setProgress(100);

            seekBarBudget.setMax(200000); // 10K to 2L
            seekBarBudget.setProgress(50000);

            updateGuestCountText();
            updateBudgetText();

            // Initialize location text
            tvLocation.setText("Mumbai, Maharashtra");

        } catch (Exception e) {
            Log.e("SearchFilterActivity", "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Layout initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupEventListeners() {
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 3 && s.toString().contains(" ")) {
                    processNaturalLanguageQuery(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        seekBarGuests.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                guestCount = Math.max(10, progress);
                updateGuestCountText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarBudget.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxBudget = Math.max(10000, progress);
                updateBudgetText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCity = cities.get(position);
                tvLocation.setText(selectedCity + ", Maharashtra");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnUseCurrentLocation.setOnClickListener(v -> {
            useLocation = true;
            loadUserLocation();
        });

        btnShowVenues.setOnClickListener(v -> searchVenues());

        // Setup switch listeners
        switchGuests.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Enable/disable guest seekbar based on switch state
            seekBarGuests.setEnabled(isChecked);
            tvGuestCount.setEnabled(isChecked);
        });

        switchOutdoor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Handle outdoor filter logic
        });
    }

    // Rest of your methods remain the same...
    private void processNaturalLanguageQuery(String query) {
        if (aiService != null) {
            aiService.processNaturalLanguageQuery(query,
                    new AIRecommendationService.NLPResultCallback() {
                        @Override
                        public void onFiltersExtracted(Map<String, Object> filters) {
                            runOnUiThread(() -> applyNLPFilters(filters));
                        }
                    });
        }
    }

    private void applyNLPFilters(Map<String, Object> filters) {
        if (filters.containsKey("minCapacity")) {
            guestCount = (int) filters.get("minCapacity");
            seekBarGuests.setProgress(guestCount);
            updateGuestCountText();
        }

        if (filters.containsKey("city")) {
            selectedCity = filters.get("city").toString();
            int position = cities.indexOf(selectedCity);
            if (position >= 0) {
                spinnerCity.setSelection(position);
            }
            tvLocation.setText(selectedCity + ", Maharashtra");
        }

        if (filters.containsKey("maxPrice")) {
            maxBudget = (double) filters.get("maxPrice");
            seekBarBudget.setProgress((int) maxBudget);
            updateBudgetText();
        }

        if (filters.containsKey("type")) {
            String type = filters.get("type").toString();
            switchOutdoor.setChecked(type.equals("outdoor"));
        }

        if (filters.containsKey("eventType")) {
            String eventType = filters.get("eventType").toString();
            highlightEventTypeChip(eventType);
        }
    }

    private void highlightEventTypeChip(String eventType) {
        for (int i = 0; i < chipGroupEventType.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupEventType.getChildAt(i);
            if (chip.getText().toString().toLowerCase().contains(eventType.toLowerCase())) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private void updateGuestCountText() {
        tvGuestCount.setText("Guests: " + guestCount);
    }

    private void updateBudgetText() {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(0);
        tvMaxBudget.setText("Max Budget: " + format.format(maxBudget));
    }

    private void loadUserLocation() {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Request permission
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null && useLocation) {
                            String city = getCityFromLocation(location);
                            if (city != null) {
                                selectedCity = city;
                                int position = cities.indexOf(city);
                                if (position >= 0) {
                                    spinnerCity.setSelection(position);
                                }
                                tvLocation.setText(city + " (Current Location)");
                            }
                        } else {
                            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCityFromLocation(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        // Maharashtra city coordinates approximation
        if (lat >= 18.9 && lat <= 19.3 && lng >= 72.7 && lng <= 73.0) return "Mumbai";
        if (lat >= 18.4 && lat <= 18.7 && lng >= 73.7 && lng <= 74.0) return "Pune";
        if (lat >= 21.0 && lat <= 21.3 && lng >= 79.0 && lng <= 79.2) return "Nagpur";
        if (lat >= 19.9 && lat <= 20.2 && lng >= 73.6 && lng <= 73.9) return "Nashik";
        if (lat >= 19.8 && lat <= 20.0 && lng >= 75.2 && lng <= 75.4) return "Aurangabad";
        if (lat >= 19.1 && lat <= 19.3 && lng >= 72.9 && lng <= 73.1) return "Thane";

        return "Mumbai"; // Default
    }

    private void searchVenues() {
        Map<String, Object> filters = new HashMap<>();

        // Get selected event types
        List<String> selectedEventTypes = getSelectedChipTexts(chipGroupEventType);
        if (!selectedEventTypes.isEmpty()) {
            filters.put("eventTypes", selectedEventTypes);
        }

        // Get selected venue categories
        List<String> selectedCategories = getSelectedChipTexts(chipGroupVenueCategory);
        if (!selectedCategories.isEmpty()) {
            filters.put("categories", selectedCategories);
        }

        // Add other filters
        filters.put("city", selectedCity);
        filters.put("minCapacity", guestCount);
        filters.put("maxPrice", maxBudget);

        if (switchOutdoor.isChecked()) {
            filters.put("type", "outdoor");
        } else {
            filters.put("type", "indoor");
        }

        // Get NLP query if any
        String query = etSearchQuery.getText().toString().trim();
        if (!query.isEmpty()) {
            filters.put("query", query);
        }

        // Show loading
        Toast.makeText(this, "Searching venues...", Toast.LENGTH_SHORT).show();

        // Search in local database first
        List<Venue> venues = databaseHelper.searchVenuesWithFilters(
                selectedCity,
                selectedCategories.isEmpty() ? null : selectedCategories.get(0),
                switchOutdoor.isChecked() ? "outdoor" : "indoor",
                guestCount,
                maxBudget
        );

        if (venues != null && !venues.isEmpty()) {
            showVenueResults(venues);
        } else {
            searchFirestoreVenues(filters);
        }
    }

    private List<String> getSelectedChipTexts(ChipGroup chipGroup) {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selected.add(chip.getText().toString());
            }
        }
        return selected;
    }

    private void searchFirestoreVenues(Map<String, Object> filters) {
        VenueService venueService = new VenueService();
        venueService.searchVenues(filters, new VenueService.VenueListCallback() {
            @Override
            public void onSuccess(List<Venue> venues) {
                runOnUiThread(() -> {
                    if (venues == null || venues.isEmpty()) {
                        showEmptyState();
                    } else {
                        showVenueResults(venues);
                        cacheVenuesLocally(venues);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SearchFilterActivity.this,
                            "Search failed: " + error, Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }

    private void showVenueResults(List<Venue> venues) {
        Intent intent = new Intent(this, VenueListActivity.class);
        intent.putExtra("venues", new ArrayList<>(venues));
        intent.putExtra("searchType", "filtered");
        startActivity(intent);
    }

    private void showEmptyState() {
        Toast.makeText(this, "No venues found matching your criteria", Toast.LENGTH_LONG).show();
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
        if (useLocation) {
            loadUserLocation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}