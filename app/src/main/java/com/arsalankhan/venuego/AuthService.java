package com.arsalankhan.venuego;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

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

    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

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

    public void signUp(String name, String email, String password, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            User user = new User(firebaseUser.getUid(), name, email);
                            saveUserToFirestore(user, callback);
                        } else {
                            callback.onFailure("User creation failed");
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Unknown error");
                    }
                });
    }

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

    public void getCurrentUserData(AuthCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            getUserFromFirestore(firebaseUser.getUid(), callback);
        } else {
            callback.onFailure("No user logged in");
        }
    }

    private void saveUserToFirestore(User user, AuthCallback callback) {
        firestore.collection("users")
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

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

    public void logout() {
        firebaseAuth.signOut();
    }

    public void logoutWithCallback(SimpleCallback callback) {
        try {
            firebaseAuth.signOut();
            callback.onSuccess();
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }
}