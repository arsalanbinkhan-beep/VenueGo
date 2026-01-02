package com.arsalankhan.venuego;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OTPVerificationService {
    private FirebaseAuth firebaseAuth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private static final String TAG = "OTPVerificationService";

    public OTPVerificationService() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public interface OTPCallback {
        void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token);
        void onVerificationCompleted(PhoneAuthCredential credential);
        void onVerificationFailed(String error);
        void onCodeAutoRetrievalTimeout(String verificationId);
    }

    public interface VerificationCallback {
        void onSuccess();
        void onFailure(String error);
    }

    // Send OTP to phone number
    public void sendOTP(String phoneNumber, Activity activity, OTPCallback callback) {
        Log.d(TAG, "Sending OTP to: " + phoneNumber);

        // Format phone number with country code if not present
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber; // Default to India (+91)
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        Log.d(TAG, "✓ OTP verification completed automatically");
                        callback.onVerificationCompleted(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e(TAG, "✗ OTP verification failed", e);
                        callback.onVerificationFailed(e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        Log.d(TAG, "✓ OTP code sent successfully");
                        OTPVerificationService.this.verificationId = verificationId;
                        resendToken = token;
                        callback.onCodeSent(verificationId, token);
                    }

                    public void onCodeAutoRetrievalTimeout(@NonNull String verificationId) {
                        Log.w(TAG, "OTP auto-retrieval timeout");
                        callback.onCodeAutoRetrievalTimeout(verificationId);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // Verify OTP code
    public void verifyOTP(String code, VerificationCallback callback) {
        if (verificationId == null) {
            callback.onFailure("No verification in progress. Please request OTP again.");
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneCredential(credential, callback);
    }

    // Resend OTP
    public void resendOTP(String phoneNumber, Activity activity, PhoneAuthProvider.ForceResendingToken token, OTPCallback callback) {
        Log.d(TAG, "Resending OTP to: " + phoneNumber);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setForceResendingToken(token)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        callback.onVerificationCompleted(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        callback.onVerificationFailed(e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        OTPVerificationService.this.verificationId = verificationId;
                        resendToken = token;
                        callback.onCodeSent(verificationId, token);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential, VerificationCallback callback) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✓ Phone authentication successful");
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "✗ Phone authentication failed", task.getException());
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Verification failed");
                    }
                });
    }

    // Get current verification ID
    public String getVerificationId() {
        return verificationId;
    }

    // Get resend token
    public PhoneAuthProvider.ForceResendingToken getResendToken() {
        return resendToken;
    }
}