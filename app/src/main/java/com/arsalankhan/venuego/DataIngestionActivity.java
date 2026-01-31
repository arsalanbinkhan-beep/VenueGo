package com.arsalankhan.venuego;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DataIngestionActivity extends AppCompatActivity {
    private Button btnFetchOSMData;
    private Button btnViewStats;
    private Button btnClearData;
    private TextView tvTotalVenues;
    private TextView tvCityStats;

    private OSMDataService osmDataService;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_ingestion);

        osmDataService = new OSMDataService(this);

        initializeViews();
        setupClickListeners();
        loadStatistics();
    }

    private void initializeViews() {
        btnFetchOSMData = findViewById(R.id.btnFetchOSMData);
        btnViewStats = findViewById(R.id.btnViewStats);
        btnClearData = findViewById(R.id.btnClearData);
        tvTotalVenues = findViewById(R.id.tvTotalVenues);
        tvCityStats = findViewById(R.id.tvCityStats);
    }

    private void setupClickListeners() {
        btnFetchOSMData.setOnClickListener(v -> fetchOSMData());
        btnViewStats.setOnClickListener(v -> viewStatistics());
        btnClearData.setOnClickListener(v -> clearAllData());
    }

    private void fetchOSMData() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching venue data from OpenStreetMap...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        osmDataService.fetchAndStoreMaharashtraVenues(new OSMDataService.SimpleOSMDataCallback() {
            @Override
            public void onSuccess(int venuesAdded) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(DataIngestionActivity.this,
                            "Successfully added " + venuesAdded + " venues",
                            Toast.LENGTH_LONG).show();
                    loadStatistics();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(DataIngestionActivity.this,
                            "Error fetching data: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadStatistics() {
        FirebaseFirestore.getInstance().collection("venues")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalVenues = queryDocumentSnapshots.size();
                    tvTotalVenues.setText("Total Venues: " + totalVenues);

                    // Count by city
                    Map<String, Integer> cityCount = new HashMap<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String city = doc.getString("city");
                        if (city != null) {
                            cityCount.put(city, cityCount.getOrDefault(city, 0) + 1);
                        }
                    }

                    // Update city statistics
                    updateCityStats(cityCount);
                })
                .addOnFailureListener(e -> {
                    tvTotalVenues.setText("Error loading statistics");
                });
    }

    private void updateCityStats(Map<String, Integer> cityCount) {
        StringBuilder stats = new StringBuilder("City Distribution:\n");
        for (Map.Entry<String, Integer> entry : cityCount.entrySet()) {
            stats.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        tvCityStats.setText(stats.toString());
    }

    private void viewStatistics() {
        // You can create a separate StatisticsActivity or show in dialog
        Toast.makeText(this, "Statistics feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void clearAllData() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("This will remove all venues from the database. Continue?")
                .setPositiveButton("Yes", (dialog, which) -> clearVenuesData())
                .setNegativeButton("No", null)
                .show();
    }

    private void clearVenuesData() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Clearing venue data...");
        progressDialog.show();

        FirebaseFirestore.getInstance().collection("venues")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        progressDialog.dismiss();
                        Toast.makeText(DataIngestionActivity.this,
                                "No data to clear", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Use batch operation to delete all documents
                    com.google.firebase.firestore.WriteBatch batch = FirebaseFirestore.getInstance().batch();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                progressDialog.dismiss();
                                Toast.makeText(DataIngestionActivity.this,
                                        "All venue data cleared", Toast.LENGTH_SHORT).show();
                                loadStatistics();
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(DataIngestionActivity.this,
                                        "Error clearing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(DataIngestionActivity.this,
                            "Error accessing database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}