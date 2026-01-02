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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class VerificationActivity extends AppCompatActivity {
    private TextView tvCountdown, tvResend, tvCallMe, tvSubtitle;
    private TextInputEditText etOTP1, etOTP2, etOTP3, etOTP4, etOTP5, etOTP6;
    private TextInputLayout[] otpLayouts = new TextInputLayout[6];
    private Button btnVerify;
    private ImageView btnBack;

    private FirebaseAuth firebaseAuth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private CountDownTimer countDownTimer;
    private boolean canResend = false;
    private String phoneNumber;
    private boolean isPhoneVerification = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        firebaseAuth = FirebaseAuth.getInstance();
        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        isPhoneVerification = getIntent().getBooleanExtra("isPhoneVerification", false);

        initializeViews();
        setupOTPInputs();
        startCountdown();
        updatePhoneNumberDisplay();

        // If no verificationId but we have phoneNumber, send OTP automatically
        if (verificationId == null && phoneNumber != null && !phoneNumber.isEmpty()) {
            sendOTP();
        }
    }

    private void initializeViews() {
        tvCountdown = findViewById(R.id.tv_countdown);
        tvResend = findViewById(R.id.tv_resend);
        tvCallMe = findViewById(R.id.tv_call_me);
        tvSubtitle = findViewById(R.id.tv_subtitle);

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
            String maskedPhone;
            if (phoneNumber.length() > 4) {
                maskedPhone = "******" + phoneNumber.substring(phoneNumber.length() - 4);
            } else {
                maskedPhone = phoneNumber;
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
        }
    }

    private void checkOTPComplete() {
        String otp = getOTPString();
        boolean isComplete = otp.length() == 6;
        btnVerify.setEnabled(isComplete);
        btnVerify.setAlpha(isComplete ? 1.0f : 0.5f);
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

        btnVerify.setEnabled(false);
        btnVerify.setText("Verifying...");

        // REAL FIREBASE OTP VERIFICATION
        if (verificationId != null) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
            signInWithPhoneCredential(credential);
        } else {
            Toast.makeText(this, "Verification session expired. Please request new OTP.", Toast.LENGTH_SHORT).show();
            resetVerifyButton();
        }
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(VerificationActivity.this, "✓ Phone verified successfully!", Toast.LENGTH_SHORT).show();

                        if (isPhoneVerification) {
                            // For phone verification (after signup), go to Login
                            redirectToLogin();
                        } else {
                            // For direct phone login, go to Main
                            proceedToMain();
                        }
                    } else {
                        Toast.makeText(VerificationActivity.this,
                                "Verification failed: " + (task.getException() != null ?
                                        task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                        highlightInvalidOTP();
                        resetVerifyButton();
                    }
                });
    }

    private void sendOTP() {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add country code if not present
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber; // Default India code
        }

        Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto-verification (SMS auto-read)
                        Toast.makeText(VerificationActivity.this, "Auto-verified!", Toast.LENGTH_SHORT).show();
                        signInWithPhoneCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(VerificationActivity.this,
                                "Failed to send OTP: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        VerificationActivity.this.verificationId = verificationId;
                        resendToken = token;
                        Toast.makeText(VerificationActivity.this, "OTP sent successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resetVerifyButton() {
        btnVerify.setEnabled(true);
        btnVerify.setText("Verify & Continue");
    }

    private void highlightInvalidOTP() {
        for (TextInputLayout layout : otpLayouts) {
            if (layout != null) {
                layout.setError(" ");
            }
        }

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

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                sendOTP();
                startCountdown();
            }
        }
    }

    private void callMeInstead() {
        Toast.makeText(this, "Call verification coming soon", Toast.LENGTH_SHORT).show();
        // Implement voice call verification
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void proceedToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
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