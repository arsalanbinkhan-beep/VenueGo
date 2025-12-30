package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arsalankhan.venuego.databinding.ActivityNotificationsBinding;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private ActivityNotificationsBinding binding;
    private List<Notification> notifications;
    private NotificationsAdapter notificationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        loadNotifications();
    }

    private void setupUI() {
        // Setup toolbar
        binding.toolbarTitle.setText("Notifications");
        binding.btnBack.setOnClickListener(v -> onBackPressed());
        binding.btnClearAll.setOnClickListener(v -> clearAllNotifications());

        // Setup empty state
        binding.tvEmptyState.setText("No notifications");

        // Setup RecyclerView
        notifications = new ArrayList<>();
        notificationsAdapter = new NotificationsAdapter(notifications, this);
        binding.recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewNotifications.setAdapter(notificationsAdapter);
    }

    private void loadNotifications() {
        // For now, show sample notifications
        // In production, fetch from Firebase
        notifications.clear();

        // Add sample notifications
        notifications.add(new Notification(
                "New Venue Alert",
                "The Grand Ballroom in Pune is now available for bookings",
                "1 hour ago",
                "venue"
        ));

        notifications.add(new Notification(
                "Booking Confirmed",
                "Your booking for Sunshine Garden on Dec 25 is confirmed",
                "2 days ago",
                "booking"
        ));

        notifications.add(new Notification(
                "Special Offer",
                "Get 20% off on all banquet halls this weekend",
                "3 days ago",
                "promotion"
        ));

        if (notifications.isEmpty()) {
            showEmptyState();
        } else {
            showNotificationsList();
            notificationsAdapter.notifyDataSetChanged();
        }
    }

    private void showEmptyState() {
        binding.recyclerViewNotifications.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void showNotificationsList() {
        binding.recyclerViewNotifications.setVisibility(View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void clearAllNotifications() {
        if (!notifications.isEmpty()) {
            notifications.clear();
            notificationsAdapter.notifyDataSetChanged();
            showEmptyState();
        }
    }
}

// Notification Model Class
class Notification {
    private String title;
    private String message;
    private String time;
    private String type; // venue, booking, promotion, alert

    public Notification(String title, String message, String time, String type) {
        this.title = title;
        this.message = message;
        this.time = time;
        this.type = type;
    }

    // Getters
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTime() { return time; }
    public String getType() { return type; }
}