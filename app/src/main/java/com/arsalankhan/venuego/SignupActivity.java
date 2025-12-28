package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arsalankhan.venuego.databinding.ActivitySignupBinding;

public class SignupActivity extends AppCompatActivity {
    private ActivitySignupBinding binding;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();

        // If user is already logged in, redirect to MainActivity
        if (authService.isUserLoggedIn()) {
            redirectToMain();
            return;
        }

        setupClickListeners();
    }

    private void redirectToMain() {
        startActivity(new Intent(SignupActivity.this, MainActivity.class));
        finish();
    }

    private void setupClickListeners() {
        binding.btnSignup.setOnClickListener(v -> signUpUser());

        binding.tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Password visibility toggle with proper click handling
        binding.etSignupPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (binding.etSignupPassword.getRight() - binding.etSignupPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility();
                    v.performClick(); // Call performClick to handle accessibility
                    return true;
                }
            }
            return false;
        });
    }

    private void signUpUser() {
        String name = binding.etSignupName.getText().toString().trim();
        String email = binding.etSignupEmail.getText().toString().trim();
        String password = binding.etSignupPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSignup.setEnabled(false);

        authService.signUp(name, email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                binding.btnSignup.setEnabled(true);
                Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                redirectToMain();
            }

            @Override
            public void onFailure(String errorMessage) {
                binding.btnSignup.setEnabled(true);
                Toast.makeText(SignupActivity.this, "Signup failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void togglePasswordVisibility() {
        if (binding.etSignupPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            binding.etSignupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            binding.etSignupPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, R.drawable.ic_eye_off, 0);
        } else {
            binding.etSignupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.etSignupPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, R.drawable.ic_eye, 0);
        }
        binding.etSignupPassword.setSelection(binding.etSignupPassword.getText().length());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if user logged in from another device or session
        if (authService.isUserLoggedIn()) {
            redirectToMain();
        }
    }
}