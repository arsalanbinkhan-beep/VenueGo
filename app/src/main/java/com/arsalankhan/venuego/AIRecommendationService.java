package com.arsalankhan.venuego;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AIRecommendationService {
    private FirebaseFirestore firestore;
    private WeatherService weatherService;
    private DatabaseHelper databaseHelper;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public AIRecommendationService(Context context) {
        this.context = context;
        firestore = FirebaseFirestore.getInstance();
        weatherService = new WeatherService(context);
        databaseHelper = new DatabaseHelper(context);
    }

    public interface RecommendationCallback {
        void onSuccess(List<Venue> recommendedVenues);
        void onFailure(String error);
    }

    public void recommendVenues(Event event, RecommendationCallback callback) {
        Log.d("AIRecommendation", "Starting recommendation for event: " + event.getEventName());

        // Step 1: Get weather-aware venue type preference
        adjustVenueTypeBasedOnWeather(event, new WeatherCallback() {
            @Override
            public void onWeatherChecked() {
                // Step 2: Fetch venues with intelligent filtering
                fetchAndScoreVenues(event, callback);
            }

            @Override
            public void onError(String error) {
                // Continue without weather data
                fetchAndScoreVenues(event, callback);
            }
        });
    }

    private void adjustVenueTypeBasedOnWeather(Event event, WeatherCallback callback) {
        if (event.getEventDate() == null) {
            callback.onWeatherChecked();
            return;
        }

        weatherService.getWeatherForecast(19.0760, 72.8777, event.getEventDate(),
                new WeatherService.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherForecast forecast) {
                        event.setWeatherCondition(forecast.getCondition());
                        event.setTemperature(forecast.getTemperature());

                        // Adjust venue type based on weather
                        if (forecast.isRainy() || forecast.isExtremeHeat()) {
                            event.setVenueType("indoor");
                            Log.d("AIRecommendation", "Weather adjustment: Forcing indoor due to " +
                                    forecast.getCondition());
                        } else if (forecast.isGoodWeather()) {
                            // Keep user preference or suggest outdoor
                            if (event.getVenueType().equals("both") || event.getVenueType().isEmpty()) {
                                event.setVenueType("outdoor");
                            }
                        }
                        callback.onWeatherChecked();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.w("AIRecommendation", "Weather check failed: " + error);
                        callback.onError(error);
                    }
                });
    }

    private void fetchAndScoreVenues(Event event, RecommendationCallback callback) {
        // Try local database first for faster response
        List<DatabaseHelper.VenueScore> localScores = databaseHelper.getVenuesWithScore(
                19.0760, 72.8777, // Default to Mumbai center
                event.getGuestCount(),
                parseBudget(event.getBudgetRange()),
                event.getEventType()
        );

        if (!localScores.isEmpty()) {
            List<Venue> localVenues = new ArrayList<>();
            for (DatabaseHelper.VenueScore score : localScores) {
                localVenues.add(score.venue);
            }

            // Apply additional filters
            List<Venue> filtered = applyEventSpecificFilters(localVenues, event);

            // Cache recommendations
            if (event.getId() != null) {
                databaseHelper.saveAIRecommendation(event.getId(), filtered);
            }

            callback.onSuccess(filtered);
            return;
        }

        // Fallback to Firestore
        firestore.collection("venues")
                .whereGreaterThanOrEqualTo("capacity", event.getGuestCount())
                .whereLessThanOrEqualTo("priceRange", parseBudget(event.getBudgetRange()) * 1.5)
                .limit(100)
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

                    // Score and rank venues
                    List<VenueScore> scoredVenues = scoreVenues(allVenues, event);

                    // Get top recommendations
                    List<Venue> recommendations = getTopRecommendations(scoredVenues, 10);

                    // Apply NLP filters if any
                    recommendations = applyNLPFilters(recommendations, event);

                    // Cache in local database
                    cacheVenuesInLocalDB(allVenues);

                    callback.onSuccess(recommendations);
                })
                .addOnFailureListener(e -> {
                    Log.e("AIRecommendation", "Firestore fetch failed: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                });
    }

    private double parseBudget(String budgetRange) {
        if (budgetRange == null || budgetRange.isEmpty()) return 100000; // Default 1 lakh

        try {
            if (budgetRange.contains("Under")) {
                return Double.parseDouble(budgetRange.replaceAll("[^0-9]", ""));
            } else if (budgetRange.contains("Above")) {
                return Double.parseDouble(budgetRange.replaceAll("[^0-9]", "")) * 2;
            } else if (budgetRange.contains(" - ")) {
                String[] parts = budgetRange.split(" - ");
                return (Double.parseDouble(parts[0].replaceAll("[^0-9]", "")) +
                        Double.parseDouble(parts[1].replaceAll("[^0-9]", ""))) / 2;
            }
        } catch (Exception e) {
            Log.e("AIRecommendation", "Error parsing budget: " + e.getMessage());
        }

        return 100000;
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

        // 1. Capacity match (30% weight)
        double capacityScore = calculateCapacityScore(venue.getCapacity(), event.getGuestCount());
        totalScore += capacityScore * 0.3;

        // 2. Budget match (25% weight)
        double budgetScore = calculateBudgetScore(venue.getPriceRange(), event.getBudgetRange());
        totalScore += budgetScore * 0.25;

        // 3. Venue type match (20% weight)
        double typeScore = calculateTypeScore(venue.getType(), event.getVenueType(), event.getWeatherCondition());
        totalScore += typeScore * 0.20;

        // 4. Rating and popularity (15% weight)
        double ratingScore = calculateRatingScore(venue.getRating(), venue.getReviewCount());
        totalScore += ratingScore * 0.15;

        // 5. Event type suitability (10% weight)
        double eventTypeScore = calculateEventTypeScore(venue.getCategory(), event.getEventType());
        totalScore += eventTypeScore * 0.10;

        return totalScore;
    }

    private double calculateCapacityScore(int venueCapacity, int guestCount) {
        if (venueCapacity >= guestCount) {
            // Perfect match or larger venue
            double occupancyRatio = (double) guestCount / venueCapacity;
            if (occupancyRatio >= 0.7 && occupancyRatio <= 0.9) return 100; // Ideal 70-90% occupancy
            if (occupancyRatio >= 0.5 && occupancyRatio <= 0.95) return 80;  // Good 50-95% occupancy
            return 60; // Acceptable but not ideal
        } else {
            // Venue too small - calculate penalty
            double shortageRatio = (double) (guestCount - venueCapacity) / guestCount;
            return Math.max(0, 100 - (shortageRatio * 200)); // Heavy penalty for insufficient capacity
        }
    }

    private double calculateBudgetScore(double venuePrice, String budgetRange) {
        double budget = parseBudget(budgetRange);

        if (venuePrice <= budget) {
            // Within budget
            double budgetUtilization = venuePrice / budget;
            if (budgetUtilization >= 0.7 && budgetUtilization <= 0.9) return 100; // Ideal utilization
            if (budgetUtilization >= 0.5 && budgetUtilization <= 1.0) return 80;  // Good utilization
            return 70; // Underutilized but acceptable
        } else {
            // Over budget - calculate penalty
            double overageRatio = (venuePrice - budget) / budget;
            return Math.max(0, 100 - (overageRatio * 150)); // Penalize going over budget
        }
    }

    private double calculateTypeScore(String venueType, String preferredType, String weatherCondition) {
        boolean isWeatherSensitive = weatherCondition != null &&
                (weatherCondition.toLowerCase().contains("rain") ||
                        weatherCondition.toLowerCase().contains("storm"));

        if (isWeatherSensitive && venueType.equals("indoor")) {
            return 120; // Bonus for indoor venues during bad weather
        }

        if (preferredType.equals("both")) {
            return 100; // User has no preference
        }

        return venueType.equalsIgnoreCase(preferredType) ? 100 : 60;
    }

    private double calculateRatingScore(double rating, int reviewCount) {
        double baseScore = rating * 20; // Convert 0-5 rating to 0-100

        // Confidence factor based on review count
        double confidenceBoost = Math.min(30, Math.log10(reviewCount + 1) * 10);

        return Math.min(100, baseScore + confidenceBoost);
    }

    private double calculateEventTypeScore(String venueCategory, String eventType) {
        Map<String, List<String>> suitabilityMatrix = new HashMap<>();

        // Define suitability matrix
        suitabilityMatrix.put("wedding", Arrays.asList("banquet_hall", "hotel", "community_center", "open_ground"));
        suitabilityMatrix.put("corporate", Arrays.asList("conference_center", "auditorium", "hotel", "banquet_hall"));
        suitabilityMatrix.put("conference", Arrays.asList("conference_center", "auditorium", "hotel"));
        suitabilityMatrix.put("party", Arrays.asList("open_ground", "banquet_hall", "restaurant", "community_center"));
        suitabilityMatrix.put("birthday", Arrays.asList("restaurant", "banquet_hall", "open_ground"));
        suitabilityMatrix.put("sports", Arrays.asList("stadium", "open_ground", "sports_complex"));
        suitabilityMatrix.put("exhibition", Arrays.asList("conference_center", "exhibition_hall", "open_ground"));

        List<String> suitableCategories = suitabilityMatrix.getOrDefault(eventType.toLowerCase(),
                Arrays.asList("event_venue", "banquet_hall"));

        if (suitableCategories.contains(venueCategory)) {
            return 100;
        } else {
            // Check partial matches
            for (String suitable : suitableCategories) {
                if (venueCategory.contains(suitable) || suitable.contains(venueCategory)) {
                    return 80;
                }
            }
            return 50;
        }
    }

    private List<Venue> getTopRecommendations(List<VenueScore> scoredVenues, int limit) {
        scoredVenues.sort((a, b) -> Double.compare(b.score, a.score));

        List<Venue> recommendations = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scoredVenues.size()); i++) {
            recommendations.add(scoredVenues.get(i).venue);
        }

        return recommendations;
    }

    private List<Venue> applyEventSpecificFilters(List<Venue> venues, Event event) {
        List<Venue> filtered = new ArrayList<>();

        for (Venue venue : venues) {
            if (matchesEventRequirements(venue, event)) {
                filtered.add(venue);
            }
        }

        return filtered;
    }

    private boolean matchesEventRequirements(Venue venue, Event event) {
        // Check capacity
        if (venue.getCapacity() < event.getGuestCount()) return false;

        // Check budget
        double budget = parseBudget(event.getBudgetRange());
        if (venue.getPriceRange() > budget * 1.3) return false; // Allow 30% over budget

        // Check venue type
        if (!event.getVenueType().equals("both") &&
                !event.getVenueType().equals(venue.getType())) {
            return false;
        }

        // Check amenities for specific event types
        if (event.getEventType().equalsIgnoreCase("wedding") &&
                !hasWeddingAmenities(venue)) {
            return false;
        }

        if (event.getEventType().equalsIgnoreCase("corporate") &&
                !hasCorporateAmenities(venue)) {
            return false;
        }

        return true;
    }

    private boolean hasWeddingAmenities(Venue venue) {
        List<String> required = Arrays.asList("stage", "catering", "parking", "ac");
        if (venue.getAmenities() == null) return false;
        return venue.getAmenities().containsAll(required);
    }

    private boolean hasCorporateAmenities(Venue venue) {
        List<String> required = Arrays.asList("wifi", "sound_system", "ac", "projector");
        if (venue.getAmenities() == null) return false;
        return venue.getAmenities().containsAll(required);
    }

    private List<Venue> applyNLPFilters(List<Venue> venues, Event event) {
        // Implement basic NLP filtering based on event description
        String description = event.getEventDescription();
        if (description == null || description.isEmpty()) return venues;

        List<Venue> filtered = new ArrayList<>();
        description = description.toLowerCase();

        for (Venue venue : venues) {
            if (matchesNLPCriteria(venue, description)) {
                filtered.add(venue);
            }
        }

        return filtered.isEmpty() ? venues : filtered;
    }

    private boolean matchesNLPCriteria(Venue venue, String description) {
        // Basic keyword matching
        if (description.contains("luxury") && venue.getPriceRange() < 500000) return false;
        if (description.contains("intimate") && venue.getCapacity() > 200) return false;
        if (description.contains("grand") && venue.getCapacity() < 300) return false;
        if (description.contains("modern") && venue.getRating() < 4.0) return false;

        return true;
    }

    private void cacheVenuesInLocalDB(List<Venue> venues) {
        new Thread(() -> {
            for (Venue venue : venues) {
                databaseHelper.insertVenue(venue);
            }
            Log.d("AIRecommendation", "Cached " + venues.size() + " venues locally");
        }).start();
    }

    // NLP Search Implementation
    public void processNaturalLanguageQuery(String query, NLPResultCallback callback) {
        Log.d("AIRecommendation", "Processing NLP query: " + query);

        Map<String, Object> filters = new HashMap<>();
        query = query.toLowerCase();

        // Extract guest count
        if (query.contains("people") || query.contains("guests") || query.contains("persons")) {
            String[] words = query.split(" ");
            for (int i = 0; i < words.length; i++) {
                if (words[i].matches("\\d+") && i > 0 &&
                        (words[i-1].contains("people") || words[i-1].contains("guests") ||
                                words[i-1].contains("persons"))) {
                    try {
                        int guestCount = Integer.parseInt(words[i]);
                        filters.put("minCapacity", guestCount);
                        break;
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }
        }

        // Extract location
        String[] cities = {"mumbai", "pune", "nagpur", "nashik", "aurangabad", "thane", "navi mumbai"};
        for (String city : cities) {
            if (query.contains(city)) {
                filters.put("city", city.substring(0, 1).toUpperCase() + city.substring(1));
                break;
            }
        }

        // Extract budget
        if (query.contains("under") || query.contains("below") || query.contains("less than")) {
            String[] words = query.split(" ");
            for (int i = 0; i < words.length; i++) {
                if ((words[i].contains("under") || words[i].contains("below") ||
                        words[i].contains("less")) && i + 1 < words.length) {
                    String nextWord = words[i+1];
                    if (nextWord.contains("₹") || nextWord.contains("rs") || nextWord.contains("rupees")) {
                        try {
                            String amountStr = nextWord.replaceAll("[^0-9]", "");
                            double amount = Double.parseDouble(amountStr);
                            filters.put("maxPrice", amount);
                            break;
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
            }
        }

        // Extract venue type
        if (query.contains("indoor")) {
            filters.put("type", "indoor");
        } else if (query.contains("outdoor")) {
            filters.put("type", "outdoor");
        }

        // Extract event type
        if (query.contains("wedding")) {
            filters.put("eventType", "wedding");
        } else if (query.contains("corporate") || query.contains("conference")) {
            filters.put("eventType", "corporate");
        } else if (query.contains("party") || query.contains("birthday")) {
            filters.put("eventType", "party");
        }

        callback.onFiltersExtracted(filters);
    }

    // Geospatial Intelligence
    public List<String> recommendLocalities(List<Venue> venues, int guestCount, double budget) {
        Map<String, LocalityStats> localityMap = new HashMap<>();

        for (Venue venue : venues) {
            String city = venue.getCity();
            if (city == null || city.isEmpty()) continue;

            LocalityStats stats = localityMap.getOrDefault(city, new LocalityStats());
            stats.venueCount++;
            stats.totalCapacity += venue.getCapacity();
            stats.averagePrice += venue.getPriceRange();
            stats.averageRating += venue.getRating();

            localityMap.put(city, stats);
        }

        // Calculate scores for each locality
        List<LocalityScore> scoredLocalities = new ArrayList<>();
        for (Map.Entry<String, LocalityStats> entry : localityMap.entrySet()) {
            LocalityStats stats = entry.getValue();
            stats.averagePrice /= stats.venueCount;
            stats.averageRating /= stats.venueCount;

            double score = calculateLocalityScore(stats, guestCount, budget);
            scoredLocalities.add(new LocalityScore(entry.getKey(), score, stats));
        }

        // Sort by score
        scoredLocalities.sort((a, b) -> Double.compare(b.score, a.score));

        // Return top 3 localities
        List<String> topLocalities = new ArrayList<>();
        for (int i = 0; i < Math.min(3, scoredLocalities.size()); i++) {
            topLocalities.add(scoredLocalities.get(i).locality);
        }

        return topLocalities;
    }

    private double calculateLocalityScore(LocalityStats stats, int guestCount, double budget) {
        double score = 0;

        // Venue density (40%)
        score += Math.min(100, stats.venueCount * 10) * 0.4;

        // Capacity match (30%)
        double capacityScore = 100 - Math.abs(stats.totalCapacity/stats.venueCount - guestCount) * 0.1;
        score += Math.max(0, capacityScore) * 0.3;

        // Affordability (20%)
        double affordabilityScore = 100 - (stats.averagePrice / budget * 100);
        score += Math.max(0, affordabilityScore) * 0.2;

        // Quality (10%)
        score += stats.averageRating * 20 * 0.1;

        return score;
    }

    // Helper classes
    public class VenueScore {
        public Venue venue;
        public double score;

        public VenueScore(Venue venue, double score) {
            this.venue = venue;
            this.score = score;
        }
    }

    private class LocalityStats {
        int venueCount = 0;
        int totalCapacity = 0;
        double averagePrice = 0;
        double averageRating = 0;
    }

    private class LocalityScore {
        String locality;
        double score;
        LocalityStats stats;

        LocalityScore(String locality, double score, LocalityStats stats) {
            this.locality = locality;
            this.score = score;
            this.stats = stats;
        }
    }

    public interface WeatherCallback {
        void onWeatherChecked();
        void onError(String error);
    }

    public interface NLPResultCallback {
        void onFiltersExtracted(Map<String, Object> filters);
    }
}