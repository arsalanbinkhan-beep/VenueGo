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

    public DataSyncService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = this;
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

    // ================= SCHEDULE DAILY SYNC =================

    public void scheduleDailySync() {

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, DataSyncReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 2);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

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
        }

        Log.d("DataSyncService", "Daily sync scheduled");
    }

    // ================= SYNC PROCESS =================

    public void performIncrementalSync() {

        firestore.collection("data_sync")
                .document("last_sync")
                .get()
                .addOnSuccessListener(snapshot -> {

                    Date lastSync = null;

                    if (snapshot.exists()) {
                        lastSync = snapshot.getDate("timestamp");
                    }

                    if (lastSync == null) {
                        performFullSync();
                    } else {
                        performIncrementalOSMSync(lastSync);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DataSyncService", "Fetch failed: " + e.getMessage());
                    performFullSync();
                });
    }

    // ================= FULL SYNC =================

    private void performFullSync() {

        osmDataService.fetchAndStoreMaharashtraVenues(
                new OSMDataService.SimpleOSMDataCallback() {
                    @Override
                    public void onSuccess(int venuesAdded) {
                        String message = "Successfully added " + venuesAdded + " venues";
                        Log.d("DataSyncService", message);
                        updateSyncStatus(venuesAdded, "completed");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("DataSyncService", "Sync failed: " + error);
                        updateSyncStatus(0, "failed");
                    }
                }
        );
    }

    // ================= INCREMENTAL (TEMP FULL) =================

    private void performIncrementalOSMSync(Date lastSync) {
        Log.d("DataSyncService", "Incremental fallback → full sync");
        performFullSync();
    }

    // ================= UPDATE FIRESTORE =================

    private void updateSyncStatus(int venuesAdded, String status) {

        Map<String, Object> syncData = new HashMap<>();

        syncData.put("timestamp", new Date());
        syncData.put("venues_added", venuesAdded);
        syncData.put("sync_status", status);

        firestore.collection("data_sync")
                .document("last_sync")
                .set(syncData)
                .addOnSuccessListener(aVoid ->
                        Log.d("DataSyncService", "Sync updated: " + status)
                )
                .addOnFailureListener(e ->
                        Log.e("DataSyncService", "Update error: " + e.getMessage())
                );
    }

    // ================= CANCEL SYNC =================

    public void cancelDailySync() {

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, DataSyncReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        Log.d("DataSyncService", "Daily sync cancelled");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("DataSyncService", "Service destroyed");
    }
}