package com.arsalankhan.venuego;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIRecommendationService {
    private FirebaseFirestore firestore;
    private WeatherService weatherService;

    public AIRecommendationService() {
        firestore = FirebaseFirestore.getInstance();
        weatherService = new WeatherService();
    }

    public interface RecommendationCallback {
        void onSuccess(List<Venue> recommendedVenues);
        void onFailure(String error);
    }

    public void recommendVenues(Event event, RecommendationCallback callback) {
        // Step 1: Get all venues
        firestore.collection("venues")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Venue> allVenues = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Venue venue = doc.toObject(Venue.class);
                        if (venue != null) {
                            venue.setId(doc.getId());
                            allVenues.add(venue);
                        }
                    }

                    // Step 2: Score and rank venues
                    List<VenueScore> scoredVenues = scoreVenues(allVenues, event);

                    // Step 3: Get top 10 recommendations
                    List<Venue> recommendations = getTopRecommendations(scoredVenues, 10);

                    callback.onSuccess(recommendations);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private List<VenueScore> scoreVenues(List<Venue> venues, Event event) {
        List<VenueScore> scoredVenues = new ArrayList<>();

        for (Venue venue : venues) {
            double score = calculateVenueScore(venue, event);
            scoredVenues.add(new VenueScore(venue, score));
        }

        return scoredVenues;
    }

    private double calculateVenueScore(Venue venue, Event event) {
        double totalScore = 0.0;
        double maxScore = 100.0;

        // 1. Capacity match (30% weight)
        double capacityScore = calculateCapacityScore(venue.getCapacity(), event.getGuestCount());
        totalScore += capacityScore * 0.3;

        // 2. Budget match (25% weight)
        double budgetScore = calculateBudgetScore(venue.getPriceRange(), event.getBudgetRange());
        totalScore += budgetScore * 0.25;

        // 3. Venue type match (15% weight)
        double typeScore = calculateTypeScore(venue.getType(), event.getVenueType());
        totalScore += typeScore * 0.15;

        // 4. Rating and popularity (15% weight)
        double ratingScore = calculateRatingScore(venue.getRating(), venue.getReviewCount());
        totalScore += ratingScore * 0.15;

        // 5. Amenities match (10% weight)
        double amenitiesScore = calculateAmenitiesScore(venue.getAmenities(), event);
        totalScore += amenitiesScore * 0.10;

        // 6. Event type suitability (5% weight)
        double eventTypeScore = calculateEventTypeScore(venue.getCategory(), event.getEventType());
        totalScore += eventTypeScore * 0.05;

        return totalScore;
    }

    private double calculateCapacityScore(int venueCapacity, int guestCount) {
        if (venueCapacity >= guestCount) {
            // Perfect match or larger venue
            double ratio = (double) guestCount / venueCapacity;
            if (ratio >= 0.8) return 100; // 80-100% capacity is ideal
            if (ratio >= 0.5) return 80;  // 50-80% capacity is good
            return 60; // Venue too large but acceptable
        } else {
            // Venue too small
            return Math.max(0, 100 - ((guestCount - venueCapacity) * 2));
        }
    }

    private double calculateBudgetScore(double venuePrice, String budgetRange) {
        // Parse budget range
        double minBudget = 0;
        double maxBudget = Double.MAX_VALUE;

        if (budgetRange.contains("Under")) {
            maxBudget = Double.parseDouble(budgetRange.replaceAll("[^0-9]", ""));
        } else if (budgetRange.contains("Above")) {
            minBudget = Double.parseDouble(budgetRange.replaceAll("[^0-9]", ""));
        } else {
            String[] parts = budgetRange.split(" - ");
            minBudget = Double.parseDouble(parts[0].replaceAll("[^0-9]", ""));
            maxBudget = Double.parseDouble(parts[1].replaceAll("[^0-9]", ""));
        }

        if (venuePrice >= minBudget && venuePrice <= maxBudget) {
            return 100; // Perfect match
        } else if (venuePrice < minBudget) {
            return 70; // Under budget is acceptable
        } else {
            // Over budget - calculate penalty
            double overage = (venuePrice - maxBudget) / maxBudget;
            return Math.max(0, 100 - (overage * 100));
        }
    }

    private double calculateTypeScore(String venueType, String preferredType) {
        return venueType.equalsIgnoreCase(preferredType) ? 100 : 50;
    }

    private double calculateRatingScore(double rating, int reviewCount) {
        double baseScore = rating * 20; // Convert 0-5 rating to 0-100

        // Boost for venues with more reviews (confidence factor)
        double reviewBoost = Math.min(20, Math.log(reviewCount + 1) * 5);

        return Math.min(100, baseScore + reviewBoost);
    }

    private double calculateAmenitiesScore(List<String> venueAmenities, Event event) {
        if (venueAmenities == null || venueAmenities.isEmpty()) return 50;

        // Define required amenities based on event type
        List<String> requiredAmenities = getRequiredAmenities(event.getEventType());

        int matched = 0;
        for (String required : requiredAmenities) {
            if (venueAmenities.contains(required)) {
                matched++;
            }
        }

        return (double) matched / requiredAmenities.size() * 100;
    }

    private List<String> getRequiredAmenities(String eventType) {
        List<String> amenities = new ArrayList<>();

        switch (eventType.toLowerCase()) {
            case "wedding":
                amenities.add("catering");
                amenities.add("stage");
                amenities.add("parking");
                amenities.add("ac");
                break;
            case "corporate":
            case "conference":
                amenities.add("wifi");
                amenities.add("sound_system");
                amenities.add("ac");
                amenities.add("parking");
                break;
            case "party":
                amenities.add("sound_system");
                amenities.add("lighting");
                amenities.add("bar");
                break;
            default:
                amenities.add("parking");
                amenities.add("restrooms");
        }

        return amenities;
    }

    private double calculateEventTypeScore(String venueCategory, String eventType) {
        Map<String, List<String>> suitabilityMap = new HashMap<>();

        // Define which venue categories are suitable for which event types
        suitabilityMap.put("wedding", Arrays.asList("banquet_hall", "community_center", "hotel"));
        suitabilityMap.put("corporate", Arrays.asList("conference_center", "auditorium", "hotel"));
        suitabilityMap.put("party", Arrays.asList("open_ground", "banquet_hall", "restaurant"));
        suitabilityMap.put("sports", Arrays.asList("stadium", "open_ground"));

        List<String> suitableCategories = suitabilityMap.getOrDefault(eventType.toLowerCase(),
                Arrays.asList("event_venue", "banquet_hall"));

        return suitableCategories.contains(venueCategory) ? 100 : 60;
    }

    private List<Venue> getTopRecommendations(List<VenueScore> scoredVenues, int limit) {
        // Sort by score descending
        scoredVenues.sort((a, b) -> Double.compare(b.score, a.score));

        List<Venue> recommendations = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scoredVenues.size()); i++) {
            recommendations.add(scoredVenues.get(i).venue);
        }

        return recommendations;
    }

    // Helper class for scoring
    private static class VenueScore {
        Venue venue;
        double score;

        VenueScore(Venue venue, double score) {
            this.venue = venue;
            this.score = score;
        }
    }
}