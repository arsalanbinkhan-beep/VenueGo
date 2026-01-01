package com.arsalankhan.venuego;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

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

    public void searchVenuesNearby(double latitude, double longitude, double radiusInKm, VenueListCallback callback) {
        // For simplicity, we'll get all venues and filter by distance
        // In production, use GeoFire or similar for geospatial queries
        firestore.collection("venues")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Venue> nearbyVenues = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Venue venue = document.toObject(Venue.class);
                        if (venue != null) {
                            venue.setId(document.getId());
                            // Calculate distance
                            double distance = calculateDistance(latitude, longitude,
                                    venue.getLatitude(), venue.getLongitude());
                            if (distance <= radiusInKm) {
                                nearbyVenues.add(venue);
                            }
                        }
                    }
                    callback.onSuccess(nearbyVenues);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void searchVenues(java.util.Map<String, Object> filters, VenueListCallback callback) {
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

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula to calculate distance between two points
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in km
    }
}