package com.arsalankhan.venuego;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.arsalankhan.venuego.databinding.ActivityBookingBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {
    private ActivityBookingBinding binding;
    private AuthService authService;
    private DatabaseHelper databaseHelper;
    private Venue venue;
    private Calendar eventDateTime = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private double totalPrice = 0;
    private double basePrice = 0;

    // Event types
    private String[] eventTypes = {
            "Wedding", "Birthday", "Corporate", "Conference",
            "Seminar", "Party", "Reception", "Get Together",
            "Sports Event", "Concert", "Exhibition"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        databaseHelper = new DatabaseHelper(this);

        venue = (Venue) getIntent().getSerializableExtra("venue");

        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (venue == null) {
            Toast.makeText(this, "Venue information missing", Toast.LENGTH_SHORT).show();
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
        binding.tvVenueAddress.setText(venue.getFullAddress());
        binding.tvVenueCapacity.setText("Capacity: " + venue.getCapacity() + " guests");
        basePrice = venue.getPriceRange();
        binding.tvBasePrice.setText("Base Price: " + formatPrice(basePrice));
        binding.tvBasePriceDisplay.setText(formatPrice(basePrice));

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

    private void calculatePrice() {
        int guestCount = Integer.parseInt(binding.tvGuestCount.getText().toString());

        // Start with base price
        totalPrice = basePrice;

        // Guest count adjustment
        double guestAdjustment = 0;
        if (guestCount > 100) {
            double multiplier = guestCount / 100.0;
            guestAdjustment = basePrice * (multiplier - 1);
        }
        totalPrice += guestAdjustment;
        binding.tvGuestAdjustment.setText("+ " + formatPrice(guestAdjustment));

        // Weekend/holiday surcharge
        int dayOfWeek = eventDateTime.get(Calendar.DAY_OF_WEEK);
        double weekendSurcharge = 0;
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            weekendSurcharge = basePrice * 0.2; // 20% weekend surcharge
            totalPrice += weekendSurcharge;
        }
        binding.tvWeekendSurcharge.setText("+ " + formatPrice(weekendSurcharge));

        // Peak season surcharge (Dec-Jan)
        int month = eventDateTime.get(Calendar.MONTH);
        if (month == Calendar.DECEMBER || month == Calendar.JANUARY) {
            totalPrice += basePrice * 0.3; // 30% peak season surcharge
        }

        // Display calculated price
        binding.tvTotalPrice.setText(formatPrice(totalPrice));
    }

    private String formatPrice(double price) {
        return "₹" + String.format("%,.0f", price);
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
                        "Event: " + binding.spinnerEventType.getSelectedItem().toString() + "\n" +
                        "Date: " + binding.etEventDate.getText().toString() + "\n" +
                        "Time: " + binding.etEventTime.getText().toString() + "\n" +
                        "Guests: " + guestCount + "\n" +
                        "Total: " + formatPrice(totalPrice) + "\n\n" +
                        "Note: ₹" + String.format("%,.0f", totalPrice * 0.3) +
                        " advance payment required to confirm.")
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
        booking.setVenueAddress(venue.getAddress());
        booking.setEventDate(eventDateTime.getTime());
        booking.setEventType(binding.spinnerEventType.getSelectedItem().toString());
        booking.setGuestCount(Integer.parseInt(binding.tvGuestCount.getText().toString()));
        booking.setTotalPrice(totalPrice);
        booking.setTotalAmount(totalPrice);
        booking.setBookingDate(new Date());
        booking.setBookingStatus("PENDING");
        booking.setPaymentStatus("UNPAID");
        booking.setSpecialRequirements(binding.etSpecialRequirements.getText().toString());

        // Save to Firestore
        FirebaseFirestore.getInstance().collection("bookings")
                .add(booking)
                .addOnSuccessListener(documentReference -> {
                    booking.setId(documentReference.getId());

                    // Also save locally
                    databaseHelper.addBooking(
                            booking.getVenueId(),
                            booking.getUserId(),
                            booking.getEventName() != null ? booking.getEventName() : venue.getName() + " Event",
                            dbDateFormat.format(booking.getEventDate()),
                            booking.getEventTime(),
                            booking.getGuestCount(),
                            booking.getTotalPrice()
                    );

                    binding.progressBar.setVisibility(View.GONE);

                    // Show success dialog
                    new AlertDialog.Builder(this)
                            .setTitle("Booking Request Submitted!")
                            .setMessage("Your booking request has been sent to the venue.\n\n" +
                                    "Booking ID: " + documentReference.getId() + "\n\n" +
                                    "You will receive a confirmation within 24 hours.")
                            .setPositiveButton("View My Bookings", (dialog, which) -> {
                                Intent intent = new Intent(this, BookingsActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            })
                            .setNegativeButton("Continue Browsing", (dialog, which) -> {
                                finish();
                            })
                            .setCancelable(false)
                            .show();

                    // Update venue availability
                    updateVenueAvailability();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateVenueAvailability() {
        String bookedDate = dbDateFormat.format(eventDateTime.getTime());

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("availability." + bookedDate, false);

        FirebaseFirestore.getInstance().collection("venues")
                .document(venue.getId())
                .update(updateData)
                .addOnFailureListener(e ->
                        Log.e("BookingActivity", "Failed to update availability: " + e.getMessage()));
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking?")
                .setMessage("Are you sure you want to cancel the booking process?")
                .setPositiveButton("Yes", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("No", null)
                .show();
    }
}