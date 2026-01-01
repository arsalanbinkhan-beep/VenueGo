package com.arsalankhan.venuego;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.arsalankhan.venuego.databinding.ActivityBookingBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {
    private ActivityBookingBinding binding;
    private AuthService authService;
    private Venue venue;
    private Calendar eventDateTime = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private double totalPrice = 0;

    // Event types
    private String[] eventTypes = {
            "Wedding", "Birthday", "Corporate", "Conference",
            "Seminar", "Party", "Reception", "Get Together"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        venue = (Venue) getIntent().getSerializableExtra("venue");

        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (venue == null) {
            finish();
            return;
        }

        setupUI();
        calculatePrice();
    }

    private void setupUI() {
        binding.toolbarTitle.setText("Book Venue");
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Display venue info
        binding.tvVenueName.setText(venue.getName());
        binding.tvVenueAddress.setText(venue.getAddress());
        binding.tvVenueCapacity.setText("Capacity: " + venue.getCapacity() + " guests");
        binding.tvBasePrice.setText("Base Price: ₹" + String.format("%.0f", venue.getPriceRange()));

        // Setup event type spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, eventTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerEventType.setAdapter(adapter);

        // Setup date and time pickers
        binding.etEventDate.setOnClickListener(v -> showDatePicker());
        binding.etEventTime.setOnClickListener(v -> showTimePicker());

        // Setup guest count stepper
        binding.btnDecrease.setOnClickListener(v -> adjustGuestCount(-10));
        binding.btnIncrease.setOnClickListener(v -> adjustGuestCount(10));

        // Default values
        binding.tvGuestCount.setText("50");
        eventDateTime.add(Calendar.DAY_OF_YEAR, 7); // Default to 1 week from now
        updateDateTimeDisplay();

        // Setup amenities toggle
        setupAmenitiesToggle();

        // Setup booking button
        binding.btnConfirmBooking.setOnClickListener(v -> confirmBooking());
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    eventDateTime.set(year, month, dayOfMonth);
                    updateDateTimeDisplay();
                    calculatePrice();
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
                    updateDateTimeDisplay();
                },
                eventDateTime.get(Calendar.HOUR_OF_DAY),
                eventDateTime.get(Calendar.MINUTE),
                false);
        timePicker.show();
    }

    private void updateDateTimeDisplay() {
        binding.etEventDate.setText(dateFormat.format(eventDateTime.getTime()));
        binding.etEventTime.setText(timeFormat.format(eventDateTime.getTime()));
    }

    private void adjustGuestCount(int change) {
        int current = Integer.parseInt(binding.tvGuestCount.getText().toString());
        int newCount = Math.max(10, Math.min(venue.getCapacity(), current + change));
        binding.tvGuestCount.setText(String.valueOf(newCount));
        calculatePrice();
    }

    private void setupAmenitiesToggle() {
        // Setup toggle buttons for additional amenities
        // This would be populated based on venue's available amenities
    }

    private void calculatePrice() {
        int guestCount = Integer.parseInt(binding.tvGuestCount.getText().toString());

        // Base price calculation
        totalPrice = venue.getPriceRange();

        // Adjust for guest count (if different from venue's base capacity)
        if (guestCount > 100) {
            double guestMultiplier = guestCount / 100.0;
            totalPrice *= guestMultiplier;
        }

        // Weekend/holiday surcharge
        int dayOfWeek = eventDateTime.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            totalPrice *= 1.2; // 20% weekend surcharge
        }

        // Peak season surcharge (Dec-Jan)
        int month = eventDateTime.get(Calendar.MONTH);
        if (month == Calendar.DECEMBER || month == Calendar.JANUARY) {
            totalPrice *= 1.3; // 30% peak season surcharge
        }

        // Display calculated price
        binding.tvTotalPrice.setText("₹" + String.format("%.0f", totalPrice));
    }

    private void confirmBooking() {
        // Validate inputs
        if (binding.etEventDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select event date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (binding.etEventTime.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select event time", Toast.LENGTH_SHORT).show();
            return;
        }

        int guestCount = Integer.parseInt(binding.tvGuestCount.getText().toString());
        if (guestCount > venue.getCapacity()) {
            Toast.makeText(this, "Guest count exceeds venue capacity", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage("Confirm booking for " + venue.getName() + "?\n\n" +
                        "Date: " + binding.etEventDate.getText().toString() + "\n" +
                        "Time: " + binding.etEventTime.getText().toString() + "\n" +
                        "Guests: " + guestCount + "\n" +
                        "Total: ₹" + String.format("%.0f", totalPrice))
                .setPositiveButton("Confirm", (dialog, which) -> createBooking())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createBooking() {
        binding.progressBar.setVisibility(View.VISIBLE);

        Booking booking = new Booking();
        booking.setUserId(authService.getCurrentUser().getUid());
        booking.setVenueId(venue.getId());
        booking.setVenueName(venue.getName());
        booking.setEventDate(eventDateTime.getTime());
        booking.setEventType(binding.spinnerEventType.getSelectedItem().toString());
        booking.setGuestCount(Integer.parseInt(binding.tvGuestCount.getText().toString()));
        booking.setTotalPrice(totalPrice);
        booking.setStatus("PENDING");
        booking.setBookingDate(new Date());
        booking.setSpecialRequirements(binding.etSpecialRequirements.getText().toString());

        // Save booking to Firestore
        FirebaseFirestore.getInstance().collection("bookings")
                .add(booking)
                .addOnSuccessListener(documentReference -> {
                    binding.progressBar.setVisibility(View.GONE);

                    // Update venue availability
                    updateVenueAvailability();

                    // Send booking confirmation
                    sendBookingConfirmation(documentReference.getId());

                    Toast.makeText(this, "Booking request submitted!", Toast.LENGTH_SHORT).show();

                    // Navigate to booking details or bookings list
                    Intent intent = new Intent(this, BookingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateVenueAvailability() {
        // Mark the date as booked in venue's availability
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String bookedDate = dateFormat.format(eventDateTime.getTime());

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("availability." + bookedDate, false);

        FirebaseFirestore.getInstance().collection("venues")
                .document(venue.getId())
                .update(updateData);
    }

    private void sendBookingConfirmation(String bookingId) {
        // Send email/notification confirmation
        // This would integrate with a notification service or email service

        // For now, just log
        android.util.Log.d("Booking", "Booking created with ID: " + bookingId);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}