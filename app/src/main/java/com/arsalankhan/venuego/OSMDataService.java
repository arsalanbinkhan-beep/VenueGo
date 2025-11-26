package com.arsalankhan.venuego;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OSMDataService {
    private static final String OSM_OVERPass_URL = "https://overpass-api.de/api/interpreter";
    private FirebaseFirestore firestore;
    private OkHttpClient client;

    public OSMDataService() {
        firestore = FirebaseFirestore.getInstance();
        client = new OkHttpClient();
    }

    public interface OSMDataCallback {
        void onSuccess(int venuesAdded);
        void onFailure(String error);
    }

    public void fetchAndStoreMaharashtraVenues(OSMDataCallback callback) {
        String overpassQuery = buildMaharashtraQuery();

        RequestBody body = RequestBody.create(overpassQuery, MediaType.parse("text/plain"));

        Request request = new Request.Builder()
                .url(OSM_OVERPass_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String jsonResponse = response.body().string();
                        List<Venue> venues = parseOSMResponse(jsonResponse);
                        storeVenuesInFirestore(venues, callback);
                    } catch (Exception e) {
                        callback.onFailure(e.getMessage());
                    }
                } else {
                    callback.onFailure("HTTP Error: " + response.code());
                }
            }
        });
    }

    private String buildMaharashtraQuery() {
        // Simplified query that focuses on major cities and common venue types
        return "[out:json][timeout:180];\n" +
                "(\n" +
                // Mumbai area
                "  node[\"amenity\"=\"community_centre\"](around:30000,19.0760,72.8777);\n" +
                "  node[\"amenity\"=\"events_venue\"](around:30000,19.0760,72.8777);\n" +
                "  node[\"amenity\"=\"hall\"](around:30000,19.0760,72.8777);\n" +
                "  node[\"leisure\"=\"park\"][\"name\"](around:30000,19.0760,72.8777);\n" +
                "  node[\"building\"=\"stadium\"](around:30000,19.0760,72.8777);\n" +
                "  node[\"building\"=\"auditorium\"](around:30000,19.0760,72.8777);\n" +
                "  \n" +
                // Pune area
                "  node[\"amenity\"=\"community_centre\"](around:30000,18.5204,73.8567);\n" +
                "  node[\"amenity\"=\"events_venue\"](around:30000,18.5204,73.8567);\n" +
                "  node[\"amenity\"=\"hall\"](around:30000,18.5204,73.8567);\n" +
                "  node[\"leisure\"=\"park\"][\"name\"](around:30000,18.5204,73.8567);\n" +
                "  node[\"building\"=\"stadium\"](around:30000,18.5204,73.8567);\n" +
                "  node[\"building\"=\"auditorium\"](around:30000,18.5204,73.8567);\n" +
                "  \n" +
                // Nagpur area
                "  node[\"amenity\"=\"community_centre\"](around:30000,21.1458,79.0882);\n" +
                "  node[\"amenity\"=\"events_venue\"](around:30000,21.1458,79.0882);\n" +
                "  node[\"amenity\"=\"hall\"](around:30000,21.1458,79.0882);\n" +
                "  node[\"leisure\"=\"park\"][\"name\"](around:30000,21.1458,79.0882);\n" +
                "  node[\"building\"=\"stadium\"](around:30000,21.1458,79.0882);\n" +
                "  node[\"building\"=\"auditorium\"](around:30000,21.1458,79.0882);\n" +
                "  \n" +
                // Nashik area
                "  node[\"amenity\"=\"community_centre\"](around:30000,20.0059,73.7910);\n" +
                "  node[\"amenity\"=\"events_venue\"](around:30000,20.0059,73.7910);\n" +
                "  node[\"amenity\"=\"hall\"](around:30000,20.0059,73.7910);\n" +
                "  node[\"leisure\"=\"park\"][\"name\"](around:30000,20.0059,73.7910);\n" +
                "  node[\"building\"=\"stadium\"](around:30000,20.0059,73.7910);\n" +
                "  node[\"building\"=\"auditorium\"](around:30000,20.0059,73.7910);\n" +
                ");\n" +
                "out body;\n" +
                ">;\n" +
                "out skel qt;";
    }

    private List<Venue> parseOSMResponse(String jsonResponse) throws JSONException {
        List<Venue> venues = new ArrayList<>();
        JSONObject response = new JSONObject(jsonResponse);
        JSONArray elements = response.getJSONArray("elements");

        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            Venue venue = parseOSMElement(element);
            if (venue != null && !venue.getName().equals("Unknown Venue")) {
                venues.add(venue);
            }
        }

        Log.d("OSMDataService", "Successfully parsed " + venues.size() + " venues from OSM");
        return venues;
    }

    private Venue parseOSMElement(JSONObject element) {
        try {
            JSONObject tags = element.optJSONObject("tags");
            if (tags == null) return null;

            String name = tags.optString("name", "Unknown Venue");
            if (name.equals("Unknown Venue")) {
                return null; // Skip venues without names
            }

            double lat = element.getDouble("lat");
            double lon = element.getDouble("lon");

            Venue venue = new Venue();
            venue.setName(name);
            venue.setLatitude(lat);
            venue.setLongitude(lon);
            venue.setOsmId(String.valueOf(element.optLong("id", 0)));

            // Parse address
            String address = buildAddressFromTags(tags);
            venue.setAddress(address);

            // Determine category and type
            String category = determineCategory(tags);
            String type = determineType(tags);
            venue.setCategory(category);
            venue.setType(type);

            // Parse capacity if available
            int capacity = parseCapacity(tags);
            venue.setCapacity(capacity);

            // Parse amenities
            List<String> amenities = parseAmenities(tags);
            venue.setAmenities(amenities);

            // Determine city
            String city = determineCity(tags, lat, lon);
            venue.setCity(city);

            // Set default values
            venue.setRating(3.5 + (Math.random() * 1.5)); // 3.5-5.0
            venue.setReviewCount((int)(Math.random() * 150));
            venue.setPriceRange(estimatePriceRange(category, capacity, city));

            // Contact information
            venue.setContactPhone(tags.optString("phone", ""));
            venue.setContactEmail(tags.optString("email", ""));

            venue.setDescription(buildDescription(name, category, tags));

            // Store OSM tags for reference
            Map<String, String> osmTags = new HashMap<>();
            Iterator<String> keys = tags.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                osmTags.put(key, tags.optString(key, ""));
            }
            venue.setOsmTags(osmTags);

            return venue;

        } catch (Exception e) {
            Log.e("OSMDataService", "Error parsing OSM element: " + e.getMessage());
            return null;
        }
    }

    private String buildAddressFromTags(JSONObject tags) {
        StringBuilder address = new StringBuilder();

        if (tags.has("addr:street")) {
            address.append(tags.optString("addr:street"));
        }
        if (tags.has("addr:housenumber")) {
            address.append(" ").append(tags.optString("addr:housenumber"));
        }
        if (tags.has("addr:city")) {
            if (address.length() > 0) address.append(", ");
            address.append(tags.optString("addr:city"));
        } else if (tags.has("addr:suburb")) {
            if (address.length() > 0) address.append(", ");
            address.append(tags.optString("addr:suburb"));
        }

        if (address.length() == 0) {
            address.append("Maharashtra, India");
        }

        return address.toString();
    }

    private String determineCategory(JSONObject tags) {
        if (tags.has("amenity")) {
            String amenity = tags.optString("amenity");
            switch (amenity) {
                case "community_centre":
                    return "community_center";
                case "events_venue":
                    return "event_venue";
                case "hall":
                    return "banquet_hall";
                case "restaurant":
                    return "restaurant";
            }
        }

        if (tags.has("leisure")) {
            String leisure = tags.optString("leisure");
            if ("park".equals(leisure)) {
                return "open_ground";
            }
        }

        if (tags.has("building")) {
            String building = tags.optString("building");
            if ("stadium".equals(building)) return "stadium";
            if ("auditorium".equals(building)) return "auditorium";
        }

        if (tags.has("tourism") && "hotel".equals(tags.optString("tourism"))) {
            if (tags.optBoolean("conference", false)) {
                return "conference_center";
            }
        }

        return "event_venue"; // default category
    }

    private String determineType(JSONObject tags) {
        if (tags.has("indoor")) {
            return tags.optString("indoor").equals("yes") ? "indoor" : "outdoor";
        }

        String category = determineCategory(tags);
        if (category.equals("open_ground") || category.equals("stadium")) {
            return "outdoor";
        }

        return "indoor"; // default
    }

    private int parseCapacity(JSONObject tags) {
        if (tags.has("capacity")) {
            try {
                return Integer.parseInt(tags.optString("capacity"));
            } catch (NumberFormatException e) {
                // Continue to estimation
            }
        }

        // Estimate capacity based on venue type
        String category = determineCategory(tags);
        switch (category) {
            case "stadium":
                return 5000;
            case "auditorium":
                return 1000;
            case "banquet_hall":
                return 300;
            case "community_center":
                return 200;
            case "open_ground":
                return 1000;
            default:
                return 150;
        }
    }

    private List<String> parseAmenities(JSONObject tags) {
        List<String> amenities = new ArrayList<>();

        // Parse OSM tags for amenities
        if (tags.optBoolean("parking", false)) amenities.add("parking");
        if (tags.optBoolean("air_conditioning", false) || tags.optBoolean("airconditioning", false)) amenities.add("ac");
        if (tags.optBoolean("catering", false)) amenities.add("catering");
        if (tags.optBoolean("wifi", false)) amenities.add("wifi");
        if (tags.optBoolean("stage", false)) amenities.add("stage");
        if (tags.optBoolean("lighting", false)) amenities.add("lighting");
        if (tags.optBoolean("sound_system", false)) amenities.add("sound_system");
        if (tags.optBoolean("kitchen", false)) amenities.add("kitchen");
        if (tags.optBoolean("bar", false)) amenities.add("bar");
        if (tags.optBoolean("restrooms", false)) amenities.add("restrooms");

        // Infer from other tags
        if (tags.has("accessibility") && tags.optString("accessibility").equals("yes")) {
            amenities.add("wheelchair_accessible");
        }

        return amenities;
    }

    private String determineCity(JSONObject tags, double lat, double lon) {
        if (tags.has("addr:city")) {
            return tags.optString("addr:city");
        }

        return geocodeToCity(lat, lon);
    }

    private String geocodeToCity(double lat, double lon) {
        // Mumbai coordinates
        if (lat >= 18.9 && lat <= 19.3 && lon >= 72.7 && lon <= 73.0) return "Mumbai";
        // Pune coordinates
        if (lat >= 18.4 && lat <= 18.7 && lon >= 73.7 && lon <= 74.0) return "Pune";
        // Nagpur coordinates
        if (lat >= 21.0 && lat <= 21.3 && lon >= 79.0 && lon <= 79.2) return "Nagpur";
        // Nashik coordinates
        if (lat >= 19.9 && lat <= 20.2 && lon >= 73.6 && lon <= 73.9) return "Nashik";
        // Aurangabad coordinates
        if (lat >= 19.8 && lat <= 20.0 && lon >= 75.2 && lon <= 75.4) return "Aurangabad";
        // Thane coordinates
        if (lat >= 19.1 && lat <= 19.3 && lon >= 72.9 && lon <= 73.1) return "Thane";
        // Navi Mumbai coordinates
        if (lat >= 19.0 && lat <= 19.2 && lon >= 73.0 && lon <= 73.2) return "Navi Mumbai";

        return "Maharashtra";
    }

    private double estimatePriceRange(String category, int capacity, String city) {
        double basePrice;

        switch (category) {
            case "stadium":
                basePrice = 500000;
                break;
            case "auditorium":
                basePrice = 200000;
                break;
            case "banquet_hall":
                basePrice = 150000;
                break;
            case "conference_center":
                basePrice = 100000;
                break;
            case "community_center":
                basePrice = 50000;
                break;
            case "open_ground":
                basePrice = 75000;
                break;
            default:
                basePrice = 60000;
        }

        // Adjust for capacity
        basePrice *= (capacity / 100.0);

        // Adjust for city (Mumbai is more expensive)
        if ("Mumbai".equals(city)) {
            basePrice *= 1.5;
        } else if ("Pune".equals(city)) {
            basePrice *= 1.2;
        }

        return Math.max(basePrice, 10000); // Minimum 10,000
    }

    private String buildDescription(String name, String category, JSONObject tags) {
        StringBuilder description = new StringBuilder();
        description.append(name).append(" is a ");

        switch (category) {
            case "banquet_hall":
                description.append("spacious banquet hall");
                break;
            case "community_center":
                description.append("community center");
                break;
            case "event_venue":
                description.append("event venue");
                break;
            case "open_ground":
                description.append("open ground");
                break;
            case "stadium":
                description.append("stadium");
                break;
            case "auditorium":
                description.append("auditorium");
                break;
            default:
                description.append("venue");
        }

        description.append(" located in ");

        if (tags.has("addr:city")) {
            description.append(tags.optString("addr:city"));
        } else {
            description.append("Maharashtra");
        }

        description.append(". Perfect for various events and gatherings.");

        return description.toString();
    }

    private void storeVenuesInFirestore(List<Venue> venues, OSMDataCallback callback) {
        if (venues.isEmpty()) {
            callback.onSuccess(0);
            return;
        }

        WriteBatch batch = firestore.batch();
        int venuesAdded = 0;

        for (Venue venue : venues) {
            // Use OSM ID as document ID to avoid duplicates
            String docId = "osm_" + venue.getOsmId();
            batch.set(firestore.collection("venues").document(docId), venue);
            venuesAdded++;
        }

        int finalVenuesAdded = venuesAdded;
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("OSMDataService", "Successfully added " + finalVenuesAdded + " venues to Firestore");
                    callback.onSuccess(finalVenuesAdded);
                })
                .addOnFailureListener(e -> {
                    Log.e("OSMDataService", "Error adding venues to Firestore: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                });
    }

    // Method to test the service
    public void testOSMService(OSMDataCallback callback) {
        String testQuery = "[out:json][timeout:30];\n" +
                "node[\"amenity\"=\"community_centre\"](around:5000,19.0760,72.8777);\n" +
                "out body;";

        RequestBody body = RequestBody.create(testQuery, MediaType.parse("text/plain"));

        Request request = new Request.Builder()
                .url(OSM_OVERPass_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Test failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess(1); // Test passed
                } else {
                    callback.onFailure("Test failed with HTTP: " + response.code());
                }
            }
        });
    }
}