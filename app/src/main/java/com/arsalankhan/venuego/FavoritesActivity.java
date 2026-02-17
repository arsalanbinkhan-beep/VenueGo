package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arsalankhan.venuego.databinding.ActivityFavoritesBinding;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {
    private ActivityFavoritesBinding binding;
    private AuthService authService;
    private DatabaseHelper databaseHelper;
    private List<Venue> favoriteVenues;
    private FavoritesAdapter favoritesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        databaseHelper = new DatabaseHelper(this);

        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        setupUI();
        loadFavorites();
    }

    private void setupUI() {
        binding.toolbarTitle.setText("My Favorites");
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        binding.tvEmptyState.setText("No favorite venues yet");
        binding.btnExplore.setOnClickListener(v -> {
            startActivity(new Intent(this, SearchFilterActivity.class));
            finish();
        });

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
        List<Venue> venues = databaseHelper.getUserFavorites(userId);

        favoriteVenues.clear();
        favoriteVenues.addAll(venues);

        if (favoriteVenues.isEmpty()) {
            showEmptyState();
        } else {
            showFavoritesList();
            favoritesAdapter.notifyDataSetChanged();
        }
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
    protected void onResume() {
        super.onResume();
        if (!authService.isUserLoggedIn()) {
            redirectToLogin();
        } else {
            loadFavorites();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}