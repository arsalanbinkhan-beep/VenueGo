package com.arsalankhan.venuego;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SimpleAuthService {
    private final FirebaseAuth firebaseAuth;


    public SimpleAuthService() {
        firebaseAuth = FirebaseAuth.getInstance();
        Log.d("SimpleAuth", "Firebase Auth initialized: " + (firebaseAuth != null));
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String errorMessage);
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

    // SIMPLIFIED SIGNUP - No Firestore dependency
    public void signUp(String name, String email, String password, AuthCallback callback) {
        Log.d("SimpleAuth", "Starting signup for: " + email);

        if (firebaseAuth == null) {
            callback.onFailure("Firebase not initialized");
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            Log.d("SimpleAuth", "✓ Firebase user created: " + firebaseUser.getUid());

                            // Update profile with name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d("SimpleAuth", "✓ Profile updated with name: " + name);

                                            // Send verification email
                                            firebaseUser.sendEmailVerification()
                                                    .addOnCompleteListener(verifyTask -> {
                                                        if (verifyTask.isSuccessful()) {
                                                            Log.d("SimpleAuth", "✓ Verification email sent to: " + email);

                                                            // Create local User object
                                                            User user = new User(firebaseUser.getUid(), name, email);
                                                            user.setPhone("");

                                                            // SUCCESS - user created and verification email sent
                                                            callback.onSuccess(user);

                                                        } else {
                                                            Log.e("SimpleAuth", "✗ Verification email failed", verifyTask.getException());
                                                            // User created but verification failed
                                                            User user = new User(firebaseUser.getUid(), name, email);
                                                            user.setPhone("");
                                                            callback.onSuccess(user); // Still success
                                                        }
                                                    });
                                        } else {
                                            Log.e("SimpleAuth", "✗ Profile update failed", profileTask.getException());
                                            callback.onFailure("Failed to update profile: " +
                                                    (profileTask.getException() != null ?
                                                            profileTask.getException().getMessage() : "Unknown error"));
                                        }
                                    });
                        } else {
                            callback.onFailure("User creation failed - no user returned");
                        }
                    } else {
                        Log.e("SimpleAuth", "✗ Firebase signup failed", task.getException());
                        String errorMsg = "Sign up failed: ";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error.contains("email address is already in use")) {
                                errorMsg = "This email is already registered.";
                            } else if (error.contains("password is invalid") || error.contains("WEAK_PASSWORD")) {
                                errorMsg = "Password must be at least 6 characters.";
                            } else if (error.contains("network error") || error.contains("timeout")) {
                                errorMsg = "Network error. Check your internet connection.";
                            } else {
                                errorMsg = error;
                            }
                        }
                        callback.onFailure(errorMsg);
                    }
                });
    }

    // LOGIN
    public void login(String email, String password, AuthCallback callback) {
        Log.d("SimpleAuth", "Login attempt: " + email);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("SimpleAuth", "✓ Login successful");
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            User user = new User(
                                    firebaseUser.getUid(),
                                    firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User",
                                    firebaseUser.getEmail()
                            );
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure("Login failed - no user returned");
                        }
                    } else {
                        String errorMsg = "Login failed: ";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error.contains("password is invalid")) {
                                errorMsg = "Incorrect password.";
                            } else if (error.contains("no user record")) {
                                errorMsg = "No account found with this email.";
                            } else {
                                errorMsg = error;
                            }
                        }
                        callback.onFailure(errorMsg);
                    }
                });
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

    // Send email verification (for existing users)
    public void sendEmailVerification(SimpleCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("SimpleAuth", "Verification email sent to: " + user.getEmail());
                            callback.onSuccess();
                        } else {
                            Log.e("SimpleAuth", "Failed to send verification email", task.getException());
                            callback.onFailure(task.getException() != null ?
                                    task.getException().getMessage() : "Failed to send verification email");
                        }
                    });
        } else {
            callback.onFailure("No user logged in");
        }
    }

    // Logout
    public void logout() {
        firebaseAuth.signOut();
        Log.d("SimpleAuth", "User logged out");
    }

    // Test Firebase connection
    public void testConnection() {
        if (firebaseAuth != null) {
            Log.d("SimpleAuth", "Firebase Auth is connected");
            if (firebaseAuth.getCurrentUser() != null) {
                Log.d("SimpleAuth", "Current user: " + firebaseAuth.getCurrentUser().getEmail());
            } else {
                Log.d("SimpleAuth", "No user logged in");
            }
        } else {
            Log.e("SimpleAuth", "Firebase Auth is NULL!");
        }
    }
}