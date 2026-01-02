package com.arsalankhan.venuego;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AuthService {
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private String verificationId;
    private static final int SIGNUP_TIMEOUT_SECONDS = 30;

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
                            Log.d("AuthService", "Verification email sent to: " + user.getEmail());
                            callback.onSuccess();
                        } else {
                            Log.e("AuthService", "Failed to send verification email", task.getException());
                            callback.onFailure(task.getException() != null ?
                                    task.getException().getMessage() : "Failed to send verification email");
                        }
                    });
        } else {
            callback.onFailure("No user logged in");
        }
    }

    // Sign up with email verification - FIXED VERSION
    public void signUp(String name, String email, String password, String phone, AuthCallback callback) {
        Log.d("AuthService", "Starting sign up for: " + email);

        // Create a timeout handler on main looper
        final Handler timeoutHandler = new Handler(Looper.getMainLooper());
        final Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.e("AuthService", "Signup timeout for: " + email);
                callback.onFailure("Signup timed out. Please check your internet connection and try again.");
            }
        };

        // Schedule timeout
        timeoutHandler.postDelayed(timeoutRunnable, SIGNUP_TIMEOUT_SECONDS * 1000);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // Cancel timeout
                    timeoutHandler.removeCallbacks(timeoutRunnable);

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            Log.d("AuthService", "Firebase user created: " + firebaseUser.getUid());

                            // Update profile with name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d("AuthService", "Profile updated with name: " + name);

                                            // Send verification email
                                            firebaseUser.sendEmailVerification()
                                                    .addOnCompleteListener(verifyTask -> {
                                                        if (verifyTask.isSuccessful()) {
                                                            Log.d("AuthService", "✓ Verification email sent to: " + email);

                                                            // Create user object
                                                            User user = new User(firebaseUser.getUid(), name, email);
                                                            user.setPhone(phone);

                                                            // Save to Firestore in background
                                                            new Thread(() -> {
                                                                try {
                                                                    saveUserToFirestore(user, new AuthCallback() {
                                                                        @Override
                                                                        public void onSuccess(User storedUser) {
                                                                            Log.d("AuthService", "User saved to Firestore");
                                                                            callback.onSuccess(storedUser);
                                                                        }

                                                                        @Override
                                                                        public void onFailure(String errorMessage) {
                                                                            Log.w("AuthService", "Firestore save failed: " + errorMessage);
                                                                            // Still return success since Firebase user is created
                                                                            callback.onSuccess(user);
                                                                        }
                                                                    });
                                                                } catch (Exception e) {
                                                                    Log.e("AuthService", "Firestore thread error", e);
                                                                    callback.onSuccess(user);
                                                                }
                                                            }).start();

                                                        } else {
                                                            Log.e("AuthService", "✗ Verification email failed", verifyTask.getException());
                                                            // User created but verification failed
                                                            User user = new User(firebaseUser.getUid(), name, email);
                                                            user.setPhone(phone);
                                                            callback.onSuccess(user);
                                                        }
                                                    });
                                        } else {
                                            Log.e("AuthService", "Profile update failed", profileTask.getException());
                                            callback.onFailure("Failed to update profile: " +
                                                    (profileTask.getException() != null ?
                                                            profileTask.getException().getMessage() : "Unknown error"));
                                        }
                                    });
                        } else {
                            callback.onFailure("User creation failed - no user returned");
                        }
                    } else {
                        Log.e("AuthService", "Firebase signup failed", task.getException());
                        String errorMsg = "Sign up failed: ";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error.contains("email address is already in use")) {
                                errorMsg = "This email is already registered.";
                            } else if (error.contains("password is invalid")) {
                                errorMsg = "Password must be at least 6 characters.";
                            } else if (error.contains("network error") || error.contains("timeout")) {
                                errorMsg = "Network error. Check your internet connection.";
                            } else {
                                errorMsg = error;
                            }
                        }
                        callback.onFailure(errorMsg);
                    }
                })
                .addOnFailureListener(e -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    Log.e("AuthService", "Signup completely failed", e);
                    callback.onFailure("Signup failed: " + e.getMessage());
                });
    }

    // Sign up without phone (for backward compatibility)
    public void signUp(String name, String email, String password, AuthCallback callback) {
        signUp(name, email, password, "", callback);
    }

    // Login
    public void login(String email, String password, AuthCallback callback) {
        Log.d("AuthService", "Attempting login for: " + email);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("AuthService", "Login successful");
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            getUserFromFirestore(firebaseUser.getUid(), callback);
                        } else {
                            callback.onFailure("Login failed - no user returned");
                        }
                    } else {
                        String errorMsg = "Login failed: ";
                        if (task.getException() != null) {
                            errorMsg += task.getException().getMessage();
                            if (task.getException().getMessage().contains("password is invalid")) {
                                errorMsg = "Incorrect password. Please try again.";
                            } else if (task.getException().getMessage().contains("no user record")) {
                                errorMsg = "No account found with this email. Please sign up first.";
                            }
                        }
                        callback.onFailure(errorMsg);
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

    // Phone OTP Verification
    public void sendOTP(String phoneNumber, android.app.Activity activity, OTPCallback callback) {
        if (activity == null) {
            callback.onVerificationFailed("Activity context is required");
            return;
        }

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60, // Timeout duration
                TimeUnit.SECONDS,
                activity, // Pass the activity context
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
                    private void saveUserToFirestore(User user, AuthCallback callback) {
                        try {
                            // Add additional user data
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("uid", user.getUid());
                            userData.put("name", user.getName());
                            userData.put("email", user.getEmail());
                            userData.put("phone", user.getPhone() != null ? user.getPhone() : "");
                            userData.put("createdAt", new Date());
                            userData.put("emailVerified", false);
                            userData.put("preferences", user.getPreferences() != null ? user.getPreferences() : new ArrayList<String>());

                            firestore.collection("users")
                                    .document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("AuthService", "User saved to Firestore: " + user.getUid());
                                        callback.onSuccess(user);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("AuthService", "Error saving user to Firestore", e);

                                        // DON'T FAIL - Just log the error and continue
                                        Log.w("AuthService", "Firestore failed but user is created in Firebase Auth. Continuing...");
                                        callback.onSuccess(user); // Still return success
                                    });
                        } catch (Exception e) {
                            Log.e("AuthService", "Exception in saveUserToFirestore", e);
                            callback.onSuccess(user); // Don't fail because of Firestore
                        }
                    }
                    @Override
                    public void onVerificationFailed(@NonNull com.google.firebase.FirebaseException e) {
                        Log.e("AuthService", "OTP verification failed", e);
                        callback.onVerificationFailed(e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        Log.d("AuthService", "OTP code sent successfully");
                        AuthService.this.verificationId = verificationId;
                        resendToken = token;
                        callback.onCodeSent(verificationId, token);
                    }
                });
    }

    public void verifyOTP(String code, SimpleCallback callback) {
        if (verificationId == null) {
            callback.onFailure("No verification ID found. Please request OTP again.");
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
        // Add additional user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());
        userData.put("phone", user.getPhone() != null ? user.getPhone() : "");
        userData.put("createdAt", new Date());
        userData.put("emailVerified", false);
        userData.put("preferences", user.getPreferences() != null ? user.getPreferences() : new ArrayList<String>());

        firestore.collection("users")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("AuthService", "User saved to Firestore: " + user.getUid());
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.e("AuthService", "Error saving user to Firestore", e);
                    callback.onFailure("Failed to save user data: " + e.getMessage());
                });
    }

    // Get user from Firestore
    private void getUserFromFirestore(String uid, AuthCallback callback) {
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            User user = new User();
                            user.setUid(documentSnapshot.getString("uid"));
                            user.setName(documentSnapshot.getString("name"));
                            user.setEmail(documentSnapshot.getString("email"));
                            user.setPhone(documentSnapshot.getString("phone"));

                            callback.onSuccess(user);
                        } catch (Exception e) {
                            Log.e("AuthService", "Error parsing user data", e);
                            createBasicUserFromFirebase(uid, callback);
                        }
                    } else {
                        // Create a basic user if not in Firestore yet
                        createBasicUserFromFirebase(uid, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AuthService", "Error getting user from Firestore", e);
                    createBasicUserFromFirebase(uid, callback);
                });
    }

    private void createBasicUserFromFirebase(String uid, AuthCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            User user = new User(firebaseUser.getUid(),
                    firebaseUser.getDisplayName() != null ?
                            firebaseUser.getDisplayName() : "User",
                    firebaseUser.getEmail() != null ?
                            firebaseUser.getEmail() : "");
            // Save to Firestore for future
            saveUserToFirestore(user, new AuthCallback() {
                @Override
                public void onSuccess(User savedUser) {
                    callback.onSuccess(savedUser);
                }

                @Override
                public void onFailure(String errorMessage) {
                    // Return user anyway even if Firestore fails
                    callback.onSuccess(user);
                }
            });
        } else {
            callback.onFailure("User data not found");
        }
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

    // Add this method for debugging
    public void testDirectSignup(String email, String password, AuthCallback callback) {
        Log.d("AuthService", "Testing direct Firebase signup for: " + email);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            Log.d("AuthService", "Direct test SUCCESS: " + user.getUid());
                            callback.onSuccess(new User(user.getUid(), "Test User", email));
                        } else {
                            callback.onFailure("Direct test: User null");
                        }
                    } else {
                        Log.e("AuthService", "Direct test FAILED", task.getException());
                        callback.onFailure("Direct test failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                    }
                });
    }
}