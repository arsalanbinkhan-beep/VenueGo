package com.arsalankhan.venuego;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.google.firebase.FirebaseApp;

public class VenueGo extends Application {
    public static final String CHANNEL_ID = "venuego_notifications";
    public static final String CHANNEL_SYNC_ID = "venuego_sync";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Create notification channels
        createNotificationChannels();

        // Initialize database and services
        initializeServices();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Main notifications channel
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "VenueGo Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for bookings, reminders, and updates");

            // Sync notifications channel
            NotificationChannel syncChannel = new NotificationChannel(
                    CHANNEL_SYNC_ID,
                    "VenueGo Sync",
                    NotificationManager.IMPORTANCE_LOW
            );
            syncChannel.setDescription("Background sync notifications");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            manager.createNotificationChannel(syncChannel);
        }
    }

    private void initializeServices() {
        // Initialize database
        DatabaseHelper databaseHelper = new DatabaseHelper(this);

        // Clear old cache
        databaseHelper.clearOldWeatherCache();
        databaseHelper.clearOldSearchHistory(30);

        // Start background sync service
        DataSyncService dataSyncService = new DataSyncService();
        dataSyncService.scheduleDailySync();
    }
}