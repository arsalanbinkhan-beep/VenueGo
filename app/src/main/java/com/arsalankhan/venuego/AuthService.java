package com.arsalankhan.venuego;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AuthService {
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private String verificationId;

    public AuthService() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OTPCallback {
        void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token);
        void onVerificationFailed(String error);
        void onVerificationCompleted();
    }

    // Check if user is logged in
    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    // Get current user
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    // Check if email is verified
    public boolean isEmailVerified() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    // Reset password
    public void resetPassword(String email, SimpleCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    // Email verification
    public void sendEmailVerification(SimpleCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess();
                        } else {
                            callback.onFailure(task.getException() != null ?
                                    task.getException().getMessage() : "Failed to send verification email");
                        }
                    });
        } else {
            callback.onFailure("No user logged in");
        }
    }

    // Sign up with email verification
    public void signUp(String name, String email, String password, String phone, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Update profile with name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            // Create user in Firestore
                                            User user = new User(firebaseUser.getUid(), name, email);
                                            user.setPhone(phone);
                                            saveUserToFirestore(user, callback);

                                            // Send verification email
                                            sendEmailVerification(new SimpleCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d("AuthService", "Verification email sent");
                                                }

                                                @Override
                                                public void onFailure(String errorMessage) {
                                                    Log.e("AuthService", "Failed to send verification email: " + errorMessage);
                                                }
                                            });
                                        } else {
                                            callback.onFailure("Failed to update profile");
                                        }
                                    });
                        } else {
                            callback.onFailure("User creation failed");
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    // Sign up without phone (for backward compatibility)
    public void signUp(String name, String email, String password, AuthCallback callback) {
        signUp(name, email, password, "", callback);
    }

    // Login
    public void login(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            getUserFromFirestore(firebaseUser.getUid(), callback);
                        } else {
                            callback.onFailure("Login failed - no user returned");
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    // Get current user data
    public void getCurrentUserData(AuthCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            getUserFromFirestore(firebaseUser.getUid(), callback);
        } else {
            callback.onFailure("No user logged in");
        }
    }

    // Phone OTP Verification - FIXED VERSION
    public void sendOTP(String phoneNumber, OTPCallback callback) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60, // Timeout duration
                TimeUnit.SECONDS,
                this.getActivity(),
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithPhoneAuthCredential(credential, new SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                callback.onVerificationCompleted();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                callback.onVerificationFailed(errorMessage);
                            }
                        });
                    }

                    @Override
                    public void onVerificationFailed(@NonNull com.google.firebase.FirebaseException e) {
                        callback.onVerificationFailed(e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        AuthService.this.verificationId = verificationId;
                        resendToken = token;
                        callback.onCodeSent(verificationId, token);
                    }
                });
    }

    // Helper method to get activity
    private android.app.Activity getActivity() {
        // This is a placeholder - in real implementation, pass Activity context
        return null;
    }

    public void verifyOTP(String code, SimpleCallback callback) {
        if (verificationId == null) {
            callback.onFailure("No verification ID found");
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential, callback);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential, SimpleCallback callback) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Verification failed");
                    }
                });
    }

    // Add phone number to existing user
    public void addPhoneNumber(String phone, SimpleCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // Update in Firestore
            firestore.collection("users").document(user.getUid())
                    .update("phone", phone)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        } else {
            callback.onFailure("No user logged in");
        }
    }

    // Save user to Firestore
    private void saveUserToFirestore(User user, AuthCallback callback) {
        firestore.collection("users")
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Get user from Firestore
    private void getUserFromFirestore(String uid, AuthCallback callback) {
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure("Failed to parse user data");
                        }
                    } else {
                        // Create a basic user if not in Firestore yet
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            User user = new User(firebaseUser.getUid(),
                                    firebaseUser.getDisplayName() != null ?
                                            firebaseUser.getDisplayName() : "User",
                                    firebaseUser.getEmail() != null ?
                                            firebaseUser.getEmail() : "");
                            saveUserToFirestore(user, callback);
                        } else {
                            callback.onFailure("User data not found");
                        }
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Logout
    public void logout() {
        firebaseAuth.signOut();
    }

    // Logout with callback
    public void logoutWithCallback(SimpleCallback callback) {
        try {
            firebaseAuth.signOut();
            callback.onSuccess();
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }
}