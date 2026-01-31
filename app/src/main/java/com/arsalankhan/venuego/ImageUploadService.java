package com.arsalankhan.venuego;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImageUploadService {
    private FirebaseStorage storage;
    private StorageReference venueImagesRef;
    private Context context;

    public ImageUploadService(Context context) {
        this.context = context;
        storage = FirebaseStorage.getInstance();
        venueImagesRef = storage.getReference().child("venue_images");
    }

    public interface UploadCallback {
        void onSuccess(List<String> downloadUrls);
        void onProgress(int progress);
        void onFailure(String error);
    }

    public void uploadVenueImages(List<Uri> imageUris, String venueId, UploadCallback callback) {
        if (imageUris == null || imageUris.isEmpty()) {
            callback.onFailure("No images to upload");
            return;
        }

        List<String> downloadUrls = new ArrayList<>();
        final int[] uploadedCount = {0};
        final int totalCount = imageUris.size();

        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);
            uploadSingleImage(imageUri, venueId, i, new SingleImageCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    downloadUrls.add(downloadUrl);
                    uploadedCount[0]++;

                    int progress = (uploadedCount[0] * 100) / totalCount;
                    callback.onProgress(progress);

                    if (uploadedCount[0] == totalCount) {
                        callback.onSuccess(downloadUrls);
                    }
                }

                @Override
                public void onFailure(String error) {
                    Log.e("ImageUpload", "Failed to upload image: " + error);
                    uploadedCount[0]++;

                    if (uploadedCount[0] == totalCount && !downloadUrls.isEmpty()) {
                        callback.onSuccess(downloadUrls);
                    } else if (uploadedCount[0] == totalCount) {
                        callback.onFailure("All images failed to upload");
                    }
                }
            });
        }
    }

    private void uploadSingleImage(Uri imageUri, String venueId, int index, SingleImageCallback callback) {
        String fileName = "venue_" + venueId + "_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = venueImagesRef.child(venueId).child(fileName);

        UploadTask uploadTask = imageRef.putFile(imageUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                callback.onSuccess(uri.toString());
            }).addOnFailureListener(e -> {
                callback.onFailure(e.getMessage());
            });
        }).addOnFailureListener(e -> {
            callback.onFailure(e.getMessage());
        }).addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            Log.d("ImageUpload", "Upload progress: " + progress + "%");
        });
    }

    public void deleteImage(String imageUrl, DeleteCallback callback) {
        try {
            StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);
            imageRef.delete().addOnSuccessListener(aVoid -> {
                callback.onSuccess();
            }).addOnFailureListener(e -> {
                callback.onFailure(e.getMessage());
            });
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    public interface SingleImageCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onFailure(String error);
    }

    // Generate placeholder images for venues without images
    public List<String> generatePlaceholderImages(String venueName, String city) {
        List<String> placeholders = new ArrayList<>();

        // Using Unsplash placeholder service
        String query = venueName.replace(" ", "+") + "+" + city.replace(" ", "+");

        placeholders.add("https://source.unsplash.com/featured/800x600/?" + query);
        placeholders.add("https://source.unsplash.com/featured/800x600/?venue,event");
        placeholders.add("https://source.unsplash.com/featured/800x600/?hall," + city);

        return placeholders;
    }
}