package com.arsalankhan.venuego;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DataSyncService extends Service {
    private FirebaseFirestore firestore;
    private OSMDataService osmDataService;
    private Context context;

    public DataSyncService(Context context) {
        // Required empty constructor
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        firestore = FirebaseFirestore.getInstance();
        osmDataService = new OSMDataService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DataSyncService", "Service started");
        performIncrementalSync();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void scheduleDailySync() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Create the intent for the broadcast receiver
        Intent intent = new Intent(context, DataSyncReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set alarm to trigger daily at 2 AM
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 2);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If the time has passed today, set for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
            Log.d("DataSyncService", "Daily sync scheduled for 2 AM");
        }
    }

    public void performIncrementalSync() {
        Log.d("DataSyncService", "Starting incremental sync");

        // Get last sync timestamp
        firestore.collection("data_sync").document("last_sync")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Date lastSync = null;
                    if (documentSnapshot.exists()) {
                        lastSync = documentSnapshot.getDate("timestamp");
                    }

                    if (lastSync == null) {
                        // First time sync, fetch all data
                        performFullSync();
                    } else {
                        // Incremental sync based on last sync time
                        performIncrementalOSMSync(lastSync);
                    }
                })
                .addOnFailureListener(e -> {
                    // If no sync record exists, do a full sync
                    Log.e("DataSyncService", "Error getting last sync time: " + e.getMessage());
                    performFullSync();
                });
    }

    private void performFullSync() {
        osmDataService.fetchAndStoreMaharashtraVenues(new OSMDataService.OSMDataCallback() {
            @Override
            public void onSuccess(int venuesAdded) {
                updateSyncStatus(venuesAdded, "completed");
            }

            @Override
            public void onFailure(String error) {
                updateSyncStatus(0, "failed");
            }
        });
    }

    private void performIncrementalOSMSync(Date lastSync) {
        // For now, we'll do a full sync
        // In production, you would implement OSM changeset API for incremental updates
        Log.d("DataSyncService", "Performing full sync instead of incremental");
        performFullSync();
    }

    private void updateSyncStatus(int venuesAdded, String status) {
        Map<String, Object> syncData = new HashMap<>();
        syncData.put("timestamp", new Date());
        syncData.put("venues_added", venuesAdded);
        syncData.put("sync_status", status);

        firestore.collection("data_sync").document("last_sync")
                .set(syncData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DataSyncService", "Sync status updated: " + status + ", venues added: " + venuesAdded);
                })
                .addOnFailureListener(e -> {
                    Log.e("DataSyncService", "Error updating sync status: " + e.getMessage());
                });
    }

    public void cancelDailySync() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DataSyncReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d("DataSyncService", "Daily sync cancelled");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("DataSyncService", "Service destroyed");
    }
}