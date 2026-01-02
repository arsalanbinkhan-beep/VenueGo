package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arsalankhan.venuego.databinding.ActivityFavoritesBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {
    private ActivityFavoritesBinding binding;
    private AuthService authService;
    private List<Venue> favoriteVenues;
    private FavoritesAdapter favoritesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();

        // Check authentication
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        setupUI();
        loadFavorites();
    }

    private void setupUI() {
        // Setup toolbar
        binding.toolbarTitle.setText("My Favorites");
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Setup empty state
        binding.tvEmptyState.setText("No favorite venues yet");
        binding.btnExplore.setOnClickListener(v -> {
            startActivity(new Intent(this, SearchFilterActivity.class));
            finish();
        });

        // Setup RecyclerView
        favoriteVenues = new ArrayList<>();
        favoritesAdapter = new FavoritesAdapter(favoriteVenues, this);
        binding.recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewFavorites.setAdapter(favoritesAdapter);
    }

    private void loadFavorites() {
        if (!authService.isUserLoggedIn()) {
            return;
        }

        String userId = authService.getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoriteVenues.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Venue venue = doc.toObject(Venue.class);
                        if (venue != null) {
                            venue.setId(doc.getId());
                            favoriteVenues.add(venue);
                        }
                    }

                    if (favoriteVenues.isEmpty()) {
                        showEmptyState();
                    } else {
                        showFavoritesList();
                        favoritesAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void showEmptyState() {
        binding.recyclerViewFavorites.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void showFavoritesList() {
        binding.recyclerViewFavorites.setVisibility(View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Add animation for smoother transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
        } else {
            loadFavorites();
        }
    }
}