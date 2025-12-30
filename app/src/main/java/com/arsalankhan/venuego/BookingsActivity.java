package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arsalankhan.venuego.databinding.ActivityBookingsBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookingsActivity extends AppCompatActivity {
    private ActivityBookingsBinding binding;
    private AuthService authService;
    private List<Booking> bookings;
    private BookingsAdapter bookingsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();

        // Check authentication
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        setupUI();
        loadBookings();
    }

    private void setupUI() {
        // Setup toolbar
        binding.toolbarTitle.setText("My Bookings");
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Setup tabs
        setupTabs();

        // Setup empty state
        binding.tvEmptyState.setText("No bookings yet");
        binding.btnExplore.setOnClickListener(v -> {
            startActivity(new Intent(this, SearchFilterActivity.class));
            finish();
        });

        // Setup RecyclerView
        bookings = new ArrayList<>();
        bookingsAdapter = new BookingsAdapter(bookings, this);
        binding.recyclerViewBookings.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewBookings.setAdapter(bookingsAdapter);
    }

    private void setupTabs() {
        binding.tvTabUpcoming.setOnClickListener(v -> switchTab("upcoming"));
        binding.tvTabPast.setOnClickListener(v -> switchTab("past"));
        binding.tvTabCancelled.setOnClickListener(v -> switchTab("cancelled"));

        // Default to upcoming
        switchTab("upcoming");
    }

    private void switchTab(String tab) {
        // Reset all tabs
        binding.tvTabUpcoming.setBackgroundResource(android.R.color.transparent);
        binding.tvTabUpcoming.setTextColor(getResources().getColor(android.R.color.white));

        binding.tvTabPast.setBackgroundResource(android.R.color.transparent);
        binding.tvTabPast.setTextColor(getResources().getColor(android.R.color.white));

        binding.tvTabCancelled.setBackgroundResource(android.R.color.transparent);
        binding.tvTabCancelled.setTextColor(getResources().getColor(android.R.color.white));

        // Highlight selected tab
        if (tab.equals("upcoming")) {
            binding.tvTabUpcoming.setBackgroundResource(R.drawable.tab_selected_background);
            binding.tvTabUpcoming.setTextColor(getResources().getColor(R.color.vibrant_purple));
        } else if (tab.equals("past")) {
            binding.tvTabPast.setBackgroundResource(R.drawable.tab_selected_background);
            binding.tvTabPast.setTextColor(getResources().getColor(R.color.vibrant_purple));
        } else if (tab.equals("cancelled")) {
            binding.tvTabCancelled.setBackgroundResource(R.drawable.tab_selected_background);
            binding.tvTabCancelled.setTextColor(getResources().getColor(R.color.vibrant_purple));
        }

        // Load bookings for selected tab
        loadBookingsByStatus(tab);
    }

    private void loadBookings() {
        loadBookingsByStatus("upcoming");
    }

    private void loadBookingsByStatus(String statusFilter) {
        if (!authService.isUserLoggedIn()) {
            return;
        }

        String userId = authService.getCurrentUser().getUid();
        Query query;

        if (statusFilter.equals("upcoming")) {
            query = FirebaseFirestore.getInstance().collection("bookings")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "CONFIRMED")
                    .orderBy("eventDate", Query.Direction.ASCENDING);
        } else if (statusFilter.equals("past")) {
            query = FirebaseFirestore.getInstance().collection("bookings")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "COMPLETED")
                    .orderBy("eventDate", Query.Direction.DESCENDING);
        } else {
            query = FirebaseFirestore.getInstance().collection("bookings")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "CANCELLED")
                    .orderBy("eventDate", Query.Direction.DESCENDING);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookings.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Booking booking = doc.toObject(Booking.class);
                        if (booking != null) {
                            booking.setId(doc.getId());
                            bookings.add(booking);
                        }
                    }

                    if (bookings.isEmpty()) {
                        showEmptyState();
                    } else {
                        showBookingsList();
                        bookingsAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void showEmptyState() {
        binding.recyclerViewBookings.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void showBookingsList() {
        binding.recyclerViewBookings.setVisibility(View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
        } else {
            loadBookings();
        }
    }
}