package com.arsalankhan.venuego;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerificationHelper {
    private static final String TAG = "VerificationHelper";

    public interface VerificationCallback {
        void onVerified();
        void onNotVerified();
        void onError(String error);
    }

    public static void checkEmailVerification(VerificationCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError("No user logged in");
            return;
        }

        // Reload user to get fresh verification status
        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                if (refreshedUser != null) {
                    if (refreshedUser.isEmailVerified()) {
                        Log.d(TAG, "Email is verified: " + refreshedUser.getEmail());
                        callback.onVerified();
                    } else {
                        Log.d(TAG, "Email NOT verified: " + refreshedUser.getEmail());
                        callback.onNotVerified();
                    }
                } else {
                    callback.onError("User not found after reload");
                }
            } else {
                callback.onError("Failed to reload user: " +
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }

    public static void requestVerificationEmail(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Verification email sent to: " + user.getEmail());
                            // You could show a toast here
                        } else {
                            Log.e(TAG, "Failed to send verification email", task.getException());
                        }
                    });
        }
    }
}