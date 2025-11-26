package com.arsalankhan.venuego;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.*;

public class VenueService {
    private FirebaseFirestore firestore;

    public VenueService() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface VenueListCallback {
        void onSuccess(List<Venue> venues);
        void onFailure(String error);
    }

    public interface VenueCallback {
        void onSuccess(Venue venue);
        void onFailure(String error);
    }

    public void getTrendingVenues(VenueListCallback callback) {
        firestore.collection("venues")
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Venue> venues = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Venue venue = document.toObject(Venue.class);
                        if (venue != null) {
                            venue.setId(document.getId());
                            venues.add(venue);
                        }
                    }
                    callback.onSuccess(venues);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void searchVenues(Map<String, Object> filters, VenueListCallback callback) {
        CollectionReference venuesRef = firestore.collection("venues");
        Query query = venuesRef;

        // Apply filters
        if (filters.containsKey("city")) {
            query = query.whereEqualTo("city", filters.get("city"));
        }
        if (filters.containsKey("type")) {
            query = query.whereEqualTo("type", filters.get("type"));
        }
        if (filters.containsKey("category")) {
            query = query.whereEqualTo("category", filters.get("category"));
        }
        if (filters.containsKey("minCapacity")) {
            query = query.whereGreaterThanOrEqualTo("capacity", filters.get("minCapacity"));
        }
        if (filters.containsKey("maxPrice")) {
            query = query.whereLessThanOrEqualTo("priceRange", filters.get("maxPrice"));
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Venue> venues = new ArrayList<>();
            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                Venue venue = document.toObject(Venue.class);
                if (venue != null) {
                    venue.setId(document.getId());
                    venues.add(venue);
                }
            }
            callback.onSuccess(venues);
        }).addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getVenueById(String venueId, VenueCallback callback) {
        firestore.collection("venues")
                .document(venueId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Venue venue = documentSnapshot.toObject(Venue.class);
                        if (venue != null) {
                            venue.setId(documentSnapshot.getId());
                            callback.onSuccess(venue);
                        }
                    } else {
                        callback.onFailure("Venue not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}