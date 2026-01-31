package com.arsalankhan.venuego;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DataSyncReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DataSyncReceiver", "Data sync alarm triggered");

        // Start data sync service
        DataSyncService dataSyncService = new DataSyncService();
        dataSyncService.performIncrementalSync();

        // You can also start a foreground service here for longer operations
        startSyncService(context);
    }

    private void startSyncService(Context context) {
        // If you need to run longer operations, start a foreground service
        Intent serviceIntent = new Intent(context, DataSyncService.class);
        context.startService(serviceIntent);
    }
}