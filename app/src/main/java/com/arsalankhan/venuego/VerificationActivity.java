package com.arsalankhan.venuego;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class VerificationActivity extends AppCompatActivity {
    private TextView tvCountdown, tvResend, tvCallMe, tvSubtitle;
    private TextInputEditText etOTP1, etOTP2, etOTP3, etOTP4, etOTP5, etOTP6;
    private TextInputLayout[] otpLayouts = new TextInputLayout[6];
    private Button btnVerify;
    private ImageView btnBack;
    private AuthService authService;
    private String verificationId;
    private CountDownTimer countDownTimer;
    private boolean canResend = false;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        authService = new AuthService();
        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");

        initializeViews();
        setupOTPInputs();
        startCountdown();
        updatePhoneNumberDisplay();
    }

    private void initializeViews() {
        tvCountdown = findViewById(R.id.tv_countdown);
        tvResend = findViewById(R.id.tv_resend);
        tvCallMe = findViewById(R.id.tv_call_me);
        tvSubtitle = findViewById(R.id.tv_subtitle); // Changed from subtitle to tv_subtitle

        etOTP1 = findViewById(R.id.et_otp_1);
        etOTP2 = findViewById(R.id.et_otp_2);
        etOTP3 = findViewById(R.id.et_otp_3);
        etOTP4 = findViewById(R.id.et_otp_4);
        etOTP5 = findViewById(R.id.et_otp_5);
        etOTP6 = findViewById(R.id.et_otp_6);

        otpLayouts[0] = findViewById(R.id.input_otp_1);
        otpLayouts[1] = findViewById(R.id.input_otp_2);
        otpLayouts[2] = findViewById(R.id.input_otp_3);
        otpLayouts[3] = findViewById(R.id.input_otp_4);
        otpLayouts[4] = findViewById(R.id.input_otp_5);
        otpLayouts[5] = findViewById(R.id.input_otp_6);

        btnVerify = findViewById(R.id.btn_verify);
        btnBack = findViewById(R.id.btn_back);

        btnVerify.setOnClickListener(v -> verifyOTP());
        tvResend.setOnClickListener(v -> resendOTP());
        tvCallMe.setOnClickListener(v -> callMeInstead());
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void updatePhoneNumberDisplay() {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            // Mask the phone number for privacy (show only last 4 digits)
            String maskedPhone;
            if (phoneNumber.length() > 4) {
                maskedPhone = "******" + phoneNumber.substring(phoneNumber.length() - 4);
            } else {
                maskedPhone = phoneNumber; // Show full if too short
            }
            tvSubtitle.setText("Enter the 6-digit code sent to\n" + maskedPhone);
        }
    }

    private void setupOTPInputs() {
        TextInputEditText[] otpFields = {etOTP1, etOTP2, etOTP3, etOTP4, etOTP5, etOTP6};

        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Clear error when user types
                    if (otpLayouts[index] != null) {
                        otpLayouts[index].setError(null);
                    }

                    if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                    } else if (s.length() == 0 && index > 0) {
                        otpFields[index - 1].requestFocus();
                    }
                    checkOTPComplete();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Handle backspace
            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == 67 && event.getAction() == 0) { // Backspace pressed
                    if (otpFields[index].getText().toString().isEmpty() && index > 0) {
                        otpFields[index - 1].requestFocus();
                        otpFields[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void checkOTPComplete() {
        String otp = getOTPString();
        boolean isComplete = otp.length() == 6;
        btnVerify.setEnabled(isComplete);

        // Update button appearance
        if (isComplete) {
            btnVerify.setAlpha(1f);
        } else {
            btnVerify.setAlpha(0.5f);
        }
    }

    private String getOTPString() {
        return (etOTP1.getText() != null ? etOTP1.getText().toString() : "") +
                (etOTP2.getText() != null ? etOTP2.getText().toString() : "") +
                (etOTP3.getText() != null ? etOTP3.getText().toString() : "") +
                (etOTP4.getText() != null ? etOTP4.getText().toString() : "") +
                (etOTP5.getText() != null ? etOTP5.getText().toString() : "") +
                (etOTP6.getText() != null ? etOTP6.getText().toString() : "");
    }

    private void verifyOTP() {
        String otp = getOTPString();
        if (otp.length() != 6) {
            Toast.makeText(this, "Please enter 6-digit OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        btnVerify.setEnabled(false);
        btnVerify.setText("Verifying...");

        // Simulate verification (replace with actual Firebase/API call)
        simulateVerification(otp);
    }

    private void simulateVerification(String otp) {
        // This is a simulation - replace with actual verification logic
        new android.os.Handler().postDelayed(() -> {
            // For testing: accept any 6-digit OTP starting with 1-9
            if (otp.matches("[1-9][0-9]{5}")) {
                Toast.makeText(VerificationActivity.this, "Verification successful!", Toast.LENGTH_SHORT).show();
                proceedToMain();
            } else {
                Toast.makeText(VerificationActivity.this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
                highlightInvalidOTP();
                resetVerifyButton();
            }
        }, 1500);
    }

    private void resetVerifyButton() {
        btnVerify.setEnabled(true);
        btnVerify.setText("Verify & Continue");
        btnVerify.setAlpha(1f);
    }

    private void highlightInvalidOTP() {
        for (TextInputLayout layout : otpLayouts) {
            if (layout != null) {
                layout.setError(" ");
            }
        }

        // Clear error after 2 seconds
        new android.os.Handler().postDelayed(this::clearOTPErrors, 2000);
    }

    private void clearOTPErrors() {
        for (TextInputLayout layout : otpLayouts) {
            if (layout != null) {
                layout.setError(null);
            }
        }
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText(String.format("Resend in %d seconds", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                canResend = true;
                tvResend.setEnabled(true);
                tvResend.setAlpha(1f);
                tvCountdown.setVisibility(View.GONE);
            }
        }.start();
    }

    private void resendOTP() {
        if (canResend) {
            canResend = false;
            tvResend.setEnabled(false);
            tvResend.setAlpha(0.5f);
            tvCountdown.setVisibility(View.VISIBLE);
            tvCountdown.setText("Resend in 60 seconds");

            Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show();

            // Simulate resend delay
            new android.os.Handler().postDelayed(() -> {
                Toast.makeText(this, "OTP resent successfully!", Toast.LENGTH_SHORT).show();
                startCountdown();
            }, 2000);
        }
    }

    private void callMeInstead() {
        Toast.makeText(this, "Call verification feature coming soon", Toast.LENGTH_SHORT).show();
        // You can implement voice call verification here
    }

    private void proceedToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Show confirmation dialog
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cancel Verification?")
                .setMessage("Are you sure you want to go back? You'll need to restart the verification process.")
                .setPositiveButton("Yes", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}