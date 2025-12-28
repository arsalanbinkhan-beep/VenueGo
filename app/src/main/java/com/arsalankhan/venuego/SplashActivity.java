package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Remove the launch screen warning by using a theme
        // Apply fade in animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // 1.5 seconds delay for splash screen
        new Handler().postDelayed(this::checkAuthentication, 1500);
    }

    private void checkAuthentication() {
        AuthService authService = new AuthService();

        Intent intent;
        if (authService.isUserLoggedIn()) {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        finish(); // Close splash activity
    }
}