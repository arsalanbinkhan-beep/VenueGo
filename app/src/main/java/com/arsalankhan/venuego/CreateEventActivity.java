package com.arsalankhan.venuego;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arsalankhan.venuego.databinding.ActivityCreateEventBinding;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {
    private ActivityCreateEventBinding binding;
    private AuthService authService;
    private Calendar eventDateTime = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    // Event types
    private String[] eventTypes = {
            "Wedding", "Birthday", "Corporate", "Conference",
            "Seminar", "Party", "Reception", "Get Together",
            "Sports Event", "Concert", "Exhibition", "Other"
    };

    // Budget ranges
    private String[] budgetRanges = {
            "Under ₹50,000", "₹50,000 - ₹1,00,000",
            "₹1,00,000 - ₹5,00,000", "₹5,00,000 - ₹10,00,000",
            "Above ₹10,00,000"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();

        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        setupUI();
    }

    private void setupUI() {
        // Setup toolbar
        binding.toolbarTitle.setText("Create New Event");
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Setup spinners
        setupEventTypeSpinner();
        setupBudgetSpinner();

        // Setup date and time pickers
        binding.etEventDate.setOnClickListener(v -> showDatePicker());
        binding.etEventTime.setOnClickListener(v -> showTimePicker());

        // Setup indoor/outdoor toggle
        binding.btnIndoor.setOnClickListener(v -> toggleVenueType("indoor"));
        binding.btnOutdoor.setOnClickListener(v -> toggleVenueType("outdoor"));

        // Default to indoor
        toggleVenueType("indoor");

        // Setup create button
        binding.btnFindVenues.setOnClickListener(v -> createAndFindVenues());

        // Setup guest count stepper
        binding.btnDecrease.setOnClickListener(v -> adjustGuestCount(-10));
        binding.btnIncrease.setOnClickListener(v -> adjustGuestCount(10));
    }

    private void setupEventTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, eventTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerEventType.setAdapter(adapter);

        binding.spinnerEventType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Update AI recommendations based on event type
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupBudgetSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, budgetRanges);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBudget.setAdapter(adapter);
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    eventDateTime.set(year, month, dayOfMonth);
                    binding.etEventDate.setText(dateFormat.format(eventDateTime.getTime()));
                },
                eventDateTime.get(Calendar.YEAR),
                eventDateTime.get(Calendar.MONTH),
                eventDateTime.get(Calendar.DAY_OF_MONTH));

        // Set minimum date to tomorrow
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis() + 86400000);
        // Set maximum date to 1 year from now
        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis() + (365L * 86400000));
        datePicker.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    eventDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    eventDateTime.set(Calendar.MINUTE, minute);
                    binding.etEventTime.setText(timeFormat.format(eventDateTime.getTime()));
                },
                eventDateTime.get(Calendar.HOUR_OF_DAY),
                eventDateTime.get(Calendar.MINUTE),
                false);
        timePicker.show();
    }

    private void toggleVenueType(String type) {
        if (type.equals("indoor")) {
            binding.btnIndoor.setSelected(true);
            binding.btnOutdoor.setSelected(false);
            binding.btnIndoor.setBackgroundResource(R.drawable.button_selected);
            binding.btnOutdoor.setBackgroundResource(R.drawable.button_unselected);
        } else {
            binding.btnIndoor.setSelected(false);
            binding.btnOutdoor.setSelected(true);
            binding.btnIndoor.setBackgroundResource(R.drawable.button_unselected);
            binding.btnOutdoor.setBackgroundResource(R.drawable.button_selected);
        }
    }

    private void adjustGuestCount(int change) {
        int current = Integer.parseInt(binding.tvGuestCount.getText().toString());
        int newCount = Math.max(10, current + change); // Minimum 10 guests
        binding.tvGuestCount.setText(String.valueOf(newCount));
    }

    private void createAndFindVenues() {
        // Validate inputs
        if (binding.etEventName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter event name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (binding.etEventDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select event date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (binding.etEventTime.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select event time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create event object
        Event event = new Event();
        event.setUserId(authService.getCurrentUser().getUid());
        event.setEventName(binding.etEventName.getText().toString().trim());
        event.setEventDescription(binding.etEventDescription.getText().toString().trim());
        event.setEventType(binding.spinnerEventType.getSelectedItem().toString());
        event.setEventDate(eventDateTime.getTime());
        event.setGuestCount(Integer.parseInt(binding.tvGuestCount.getText().toString()));
        event.setVenueType(binding.btnIndoor.isSelected() ? "indoor" : "outdoor");
        event.setBudgetRange(binding.spinnerBudget.getSelectedItem().toString());
        event.setCreatedAt(new Date());

        // Get weather forecast for the event date
        checkWeatherAndFindVenues(event);
    }

    private void checkWeatherAndFindVenues(Event event) {
        // Get weather service
        WeatherService weatherService = new WeatherService(this);

        // Get user's location for weather
        // For now, use default location (Mumbai)
        weatherService.getWeatherForecast(19.0760, 72.8777, event.getEventDate(),
                new WeatherService.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherForecast forecast) {
                        // Store weather info with event
                        event.setWeatherCondition(forecast.getCondition());
                        event.setTemperature(forecast.getTemperature());

                        // Adjust venue type based on weather
                        if (forecast.getCondition().toLowerCase().contains("rain") ||
                                forecast.getTemperature() > 35) {
                            // Force indoor if rain or extreme heat
                            event.setVenueType("indoor");
                        }

                        // Find AI-recommended venues
                        findAIRecVenues(event);
                    }

                    @Override
                    public void onFailure(String error) {
                        // Proceed without weather data
                        findAIRecVenues(event);
                    }
                });
    }

    private void findAIRecVenues(Event event) {
        // Use AI recommendation service
        AIRecommendationService aiService = new AIRecommendationService(this);

        aiService.recommendVenues(event, new AIRecommendationService.RecommendationCallback() {
            @Override
            public void onSuccess(List<Venue> recommendedVenues) {
                // Navigate to venue list with recommendations
                Intent intent = new Intent(CreateEventActivity.this, VenueListActivity.class);
                intent.putExtra("event", event);

                // Create ArrayList to pass venues
                ArrayList<Venue> venueList = new ArrayList<>(recommendedVenues);
                intent.putExtra("venues", venueList);

                startActivity(intent);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CreateEventActivity.this,
                        "AI recommendation failed: " + error, Toast.LENGTH_SHORT).show();
                // Fallback to regular search
                fallbackSearch(event);
            }
        });
    }

    private void fallbackSearch(Event event) {
        // Regular search based on filters
        Map<String, Object> filters = new HashMap<>();
        filters.put("eventType", event.getEventType());
        filters.put("guestCount", event.getGuestCount());
        filters.put("venueType", event.getVenueType());

        // Navigate to venue list
        Intent intent = new Intent(this, VenueListActivity.class);
        intent.putExtra("event", event);

        // Convert HashMap to Serializable
        HashMap<String, Object> serializableFilters = new HashMap<>(filters);
        intent.putExtra("filters", serializableFilters);

        startActivity(intent);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}