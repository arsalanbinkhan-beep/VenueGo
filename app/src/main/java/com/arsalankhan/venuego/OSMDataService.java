package com.arsalankhan.venuego;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OSMDataService {
    private static final String OSM_OVERPass_URL = "https://overpass-api.de/api/interpreter";
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse";
    private FirebaseFirestore firestore;
    private OkHttpClient client;
    private Context context;
    private DatabaseHelper databaseHelper;

    // Maharashtra cities and their coordinates
    private static final Map<String, double[]> MAHARASHTRA_CITIES = new HashMap<String, double[]>() {{
        put("Mumbai", new double[]{19.0760, 72.8777});
        put("Pune", new double[]{18.5204, 73.8567});
        put("Nagpur", new double[]{21.1458, 79.0882});
        put("Nashik", new double[]{20.0059, 73.7910});
        put("Aurangabad", new double[]{19.8762, 75.3433});
        put("Thane", new double[]{19.2183, 72.9781});
        put("Navi Mumbai", new double[]{19.0330, 73.0297});
        put("Kolhapur", new double[]{16.7050, 74.2433});
        put("Solapur", new double[]{17.6599, 75.9064});
        put("Amravati", new double[]{20.9374, 77.7796});
        put("Sangli", new double[]{16.8524, 74.5815});
        put("Jalgaon", new double[]{21.0077, 75.5626});
        put("Akola", new double[]{20.7060, 77.0020});
        put("Latur", new double[]{18.4088, 76.5604});
        put("Ahmednagar", new double[]{19.0952, 74.7496});
        put("Chandrapur", new double[]{19.9615, 79.2961});
        put("Parbhani", new double[]{19.2686, 76.7708});
        put("Jalna", new double[]{19.8410, 75.8860});
        put("Bhusawal", new double[]{21.0486, 75.7851});
        put("Panvel", new double[]{18.9881, 73.1102});
    }};

    public OSMDataService() {
        firestore = FirebaseFirestore.getInstance();
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public OSMDataService(Context context) {
        this();
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
    }

    // NEW: Simple callback interface for backward compatibility
    public interface SimpleOSMDataCallback {
        void onSuccess(int venuesAdded);
        void onFailure(String error);
    }

    // NEW: Method with old interface for backward compatibility
    public void fetchAndStoreMaharashtraVenues(SimpleOSMDataCallback callback) {
        Log.d("OSMDataService", "Starting Maharashtra venues fetch (simple)...");

        fetchAllMaharashtraVenues(new OSMDataCallback() {
            @Override
            public void onSuccess(int venuesAdded, String city) {
                callback.onSuccess(venuesAdded);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }

            @Override
            public void onProgress(int progress, String message) {
                // Ignore progress for simple callback
            }
        });
    }

    // Original interface with progress
    public interface OSMDataCallback {
        void onSuccess(int venuesAdded, String city);
        void onFailure(String error);
        void onProgress(int progress, String message);
    }

    public void fetchAllMaharashtraVenues(OSMDataCallback callback) {
        Log.d("OSMDataService", "Starting Maharashtra venues fetch...");

        new Thread(() -> {
            int totalVenuesAdded = 0;
            List<String> cities = new ArrayList<>(MAHARASHTRA_CITIES.keySet());

            for (int i = 0; i < cities.size(); i++) {
                String city = cities.get(i);
                double[] coordinates = MAHARASHTRA_CITIES.get(city);

                callback.onProgress((i * 100) / cities.size(),
                        "Fetching venues for " + city + "...");

                try {
                    int venuesAdded = fetchCityVenues(city, coordinates[0], coordinates[1]);
                    totalVenuesAdded += venuesAdded;

                    Log.d("OSMDataService", "Added " + venuesAdded + " venues for " + city);

                    // Small delay to avoid rate limiting
                    Thread.sleep(2000);
                } catch (Exception e) {
                    Log.e("OSMDataService", "Error fetching " + city + ": " + e.getMessage());
                    callback.onFailure("Error fetching " + city + ": " + e.getMessage());
                }
            }

            callback.onSuccess(totalVenuesAdded, "Maharashtra");
        }).start();
    }

    private int fetchCityVenues(String city, double lat, double lon) throws Exception {
        String query = buildCityQuery(city, lat, lon);
        RequestBody body = RequestBody.create(query, MediaType.parse("text/plain"));

        Request request = new Request.Builder()
                .url(OSM_OVERPass_URL)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("HTTP error: " + response.code());
        }

        String jsonResponse = response.body().string();
        List<Venue> venues = parseOSMResponse(jsonResponse, city);

        if (venues.isEmpty()) {
            return 0;
        }

        // Store in Firestore
        return storeVenuesInFirestore(venues);
    }

    private String buildCityQuery(String city, double lat, double lon) {
        // Adjust radius based on city size
        int radius = city.equals("Mumbai") || city.equals("Pune") ? 25000 : 15000;

        return "[out:json][timeout:60];\n" +
                "(\n" +
                // Community centers and halls
                "  node[\"amenity\"=\"community_centre\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  node[\"amenity\"=\"events_venue\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  node[\"amenity\"=\"hall\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  node[\"building\"=\"hall\"][\"name\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  \n" +
                // Parks and open grounds
                "  node[\"leisure\"=\"park\"][\"name\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  node[\"leisure\"=\"garden\"][\"name\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  way[\"leisure\"=\"park\"][\"name\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  \n" +
                // Sports facilities
                "  node[\"leisure\"=\"stadium\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  node[\"leisure\"=\"sports_centre\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  node[\"building\"=\"stadium\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  \n" +
                // Conference and auditorium
                "  node[\"amenity\"=\"conference_centre\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  node[\"building\"=\"auditorium\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  \n" +
                // Hotels with event facilities
                "  node[\"tourism\"=\"hotel\"][\"name\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                "  \n" +
                // Restaurants with party halls
                "  node[\"amenity\"=\"restaurant\"][\"name\"](around:" + radius + "," + lat + "," + lon + ");\n" +
                ");\n" +
                "out body;\n" +
                ">;\n" +
                "out skel qt;";
    }

    private List<Venue> parseOSMResponse(String jsonResponse, String city) throws JSONException {
        List<Venue> venues = new ArrayList<>();
        JSONObject response = new JSONObject(jsonResponse);
        JSONArray elements = response.getJSONArray("elements");

        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            Venue venue = parseOSMElement(element, city);
            if (venue != null && !venue.getName().equals("Unknown Venue")) {
                venues.add(venue);
            }
        }

        Log.d("OSMDataService", "Parsed " + venues.size() + " venues for " + city);
        return venues;
    }

    private Venue parseOSMElement(JSONObject element, String city) {
        try {
            JSONObject tags = element.optJSONObject("tags");
            if (tags == null) return null;

            String name = tags.optString("name", "Unknown Venue");
            if (name.equals("Unknown Venue") || name.length() < 2) {
                return null; // Skip venues without proper names
            }

            // Get coordinates
            double lat, lon;
            if (element.has("lat") && element.has("lon")) {
                lat = element.getDouble("lat");
                lon = element.getDouble("lon");
            } else if (element.has("center")) {
                JSONObject center = element.getJSONObject("center");
                lat = center.getDouble("lat");
                lon = center.getDouble("lon");
            } else {
                return null; // No coordinates
            }

            Venue venue = new Venue();
            venue.setName(name);
            venue.setLatitude(lat);
            venue.setLongitude(lon);
            venue.setOsmId(String.valueOf(element.optLong("id", 0)));

            // Parse address
            String address = buildAddressFromTags(tags, city);
            venue.setAddress(address);
            venue.setCity(city);
            venue.setState("Maharashtra");
            venue.setCountry("India");

            // Determine category and type
            String category = determineCategory(tags);
            String type = determineType(tags, category);
            venue.setCategory(category);
            venue.setType(type);

            // Set capacity based on category
            int capacity = determineCapacity(category, tags);
            venue.setCapacity(capacity);

            // Parse amenities
            List<String> amenities = parseAmenities(tags);
            venue.setAmenities(amenities);

            // Set pricing based on category, capacity, and city
            double priceRange = calculatePriceRange(category, capacity, city);
            venue.setPriceRange(priceRange);

            // Set ratings and reviews
            venue.setRating(3.8 + (Math.random() * 1.2)); // 3.8-5.0
            venue.setReviewCount((int)(Math.random() * 200));

            // Contact information
            venue.setContactPhone(tags.optString("phone", ""));
            venue.setContactEmail(tags.optString("email", ""));
            venue.setWebsite(tags.optString("website", tags.optString("contact:website", "")));

            // Description
            venue.setDescription(generateDescription(name, category, city, tags));

            // Business hours
            venue.setBusinessHours(parseBusinessHours(tags));

            // Additional features
            venue.setHasParking(amenities.contains("parking"));
            venue.setParkingCapacity(capacity / 10);
            venue.setWifiAvailable(amenities.contains("wifi"));
            venue.setCateringAvailable(amenities.contains("catering"));
            venue.setWheelchairAccessible(tags.optString("wheelchair", "no").equals("yes"));

            // Timestamps
            venue.setCreatedAt(new Date());
            venue.setUpdatedAt(new Date());
            venue.setDataSource("osm");
            venue.setSyncStatus("synced");

            return venue;

        } catch (Exception e) {
            Log.e("OSMDataService", "Error parsing OSM element: " + e.getMessage());
            return null;
        }
    }

    private String buildAddressFromTags(JSONObject tags, String city) {
        StringBuilder address = new StringBuilder();

        if (tags.has("addr:street")) {
            address.append(tags.optString("addr:street"));
        }
        if (tags.has("addr:housenumber")) {
            if (address.length() > 0) address.append(" ");
            address.append(tags.optString("addr:housenumber"));
        }
        if (tags.has("addr:neighbourhood")) {
            if (address.length() > 0) address.append(", ");
            address.append(tags.optString("addr:neighbourhood"));
        }
        if (tags.has("addr:suburb")) {
            if (address.length() > 0) address.append(", ");
            address.append(tags.optString("addr:suburb"));
        }

        if (address.length() == 0) {
            address.append(city);
        } else {
            address.append(", ").append(city);
        }

        address.append(", Maharashtra");

        return address.toString();
    }

    private String determineCategory(JSONObject tags) {
        String amenity = tags.optString("amenity", "");
        String leisure = tags.optString("leisure", "");
        String building = tags.optString("building", "");
        String tourism = tags.optString("tourism", "");

        if (amenity.contains("community_centre")) return "community_center";
        if (amenity.contains("events_venue") || amenity.contains("hall")) return "banquet_hall";
        if (leisure.contains("stadium") || building.contains("stadium")) return "stadium";
        if (leisure.contains("sports_centre")) return "sports_complex";
        if (leisure.contains("park") || leisure.contains("garden")) return "open_ground";
        if (building.contains("auditorium")) return "auditorium";
        if (amenity.contains("conference_centre")) return "conference_center";
        if (tourism.contains("hotel")) return "hotel";
        if (amenity.contains("restaurant")) return "restaurant";

        return "event_venue";
    }

    private String determineType(JSONObject tags, String category) {
        String indoor = tags.optString("indoor", "");

        if (indoor.equals("yes")) return "indoor";
        if (indoor.equals("no")) return "outdoor";

        if (category.equals("open_ground") || category.equals("stadium")) {
            return "outdoor";
        }
        return "indoor";
    }

    private int determineCapacity(String category, JSONObject tags) {
        if (tags.has("capacity")) {
            try {
                return Integer.parseInt(tags.optString("capacity"));
            } catch (NumberFormatException ignored) {}
        }

        switch (category) {
            case "stadium": return 5000;
            case "sports_complex": return 1000;
            case "auditorium": return 800;
            case "banquet_hall": return 300;
            case "conference_center": return 200;
            case "hotel": return 150;
            case "community_center": return 200;
            case "open_ground": return 1000;
            case "restaurant": return 100;
            default: return 150;
        }
    }

    private List<String> parseAmenities(JSONObject tags) {
        List<String> amenities = new ArrayList<>();

        // Add amenities based on OSM tags
        if (tags.optString("parking", "").equals("yes")) amenities.add("parking");
        if (tags.optString("air_conditioning", "").equals("yes")) amenities.add("ac");
        if (tags.optString("wifi", "").equals("yes")) amenities.add("wifi");
        if (tags.optString("catering", "").equals("yes")) amenities.add("catering");
        if (tags.optString("stage", "").equals("yes")) amenities.add("stage");
        if (tags.optString("lighting", "").equals("yes")) amenities.add("lighting");
        if (tags.optString("sound_system", "").equals("yes")) amenities.add("sound_system");
        if (tags.optString("kitchen", "").equals("yes")) amenities.add("kitchen");
        if (tags.optString("bar", "").equals("yes")) amenities.add("bar");
        if (tags.optString("restrooms", "").equals("yes")) amenities.add("restrooms");

        // Default amenities based on venue type
        String category = determineCategory(tags);
        switch (category) {
            case "hotel":
                amenities.add("ac");
                amenities.add("wifi");
                amenities.add("restrooms");
                break;
            case "conference_center":
                amenities.add("ac");
                amenities.add("wifi");
                amenities.add("sound_system");
                break;
        }

        return amenities;
    }

    private double calculatePriceRange(String category, int capacity, String city) {
        double basePrice;

        switch (category) {
            case "stadium": basePrice = 500000; break;
            case "sports_complex": basePrice = 200000; break;
            case "auditorium": basePrice = 150000; break;
            case "banquet_hall": basePrice = 100000; break;
            case "conference_center": basePrice = 80000; break;
            case "hotel": basePrice = 120000; break;
            case "community_center": basePrice = 50000; break;
            case "open_ground": basePrice = 75000; break;
            case "restaurant": basePrice = 40000; break;
            default: basePrice = 60000;
        }

        // Adjust for capacity
        basePrice *= (capacity / 100.0);

        // Adjust for city (Mumbai and Pune are more expensive)
        if (city.equals("Mumbai")) basePrice *= 1.8;
        else if (city.equals("Pune")) basePrice *= 1.5;
        else if (city.equals("Nagpur") || city.equals("Nashik")) basePrice *= 1.2;

        // Minimum price
        return Math.max(basePrice, 10000);
    }

    private String generateDescription(String name, String category, String city, JSONObject tags) {
        StringBuilder description = new StringBuilder();
        description.append(name).append(" is a ");

        switch (category) {
            case "banquet_hall":
                description.append("spacious banquet hall");
                break;
            case "community_center":
                description.append("community center");
                break;
            case "conference_center":
                description.append("conference center");
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
            case "hotel":
                description.append("hotel");
                break;
            case "restaurant":
                description.append("restaurant");
                break;
            default:
                description.append("venue");
        }

        description.append(" located in ").append(city).append(", Maharashtra. ");

        // Add features from tags
        if (tags.has("description")) {
            description.append(tags.optString("description"));
        } else {
            description.append("Perfect for various events including weddings, corporate meetings, parties, and exhibitions.");
        }

        return description.toString();
    }

    private Map<String, String> parseBusinessHours(JSONObject tags) {
        Map<String, String> hours = new HashMap<>();

        if (tags.has("opening_hours")) {
            String openingHours = tags.optString("opening_hours");
            hours.put("Monday - Sunday", openingHours);
        } else {
            // Default business hours
            hours.put("Monday - Friday", "9:00 AM - 10:00 PM");
            hours.put("Saturday - Sunday", "10:00 AM - 11:00 PM");
        }

        return hours;
    }

    private int storeVenuesInFirestore(List<Venue> venues) {
        if (venues.isEmpty()) return 0;

        WriteBatch batch = firestore.batch();
        int venuesAdded = 0;

        for (Venue venue : venues) {
            // Generate document ID
            String docId = "osm_" + venue.getOsmId() + "_" +
                    venue.getCity().toLowerCase().replace(" ", "_");

            // Add to batch
            batch.set(firestore.collection("venues").document(docId), venue);
            venuesAdded++;
        }

        try {
            // Commit batch
            batch.commit();

            // Also cache locally
            cacheVenuesLocally(venues);

            return venuesAdded;
        } catch (Exception e) {
            Log.e("OSMDataService", "Error storing venues in Firestore: " + e.getMessage());
            return 0;
        }
    }

    private void cacheVenuesLocally(List<Venue> venues) {
        if (databaseHelper != null) {
            new Thread(() -> {
                for (Venue venue : venues) {
                    databaseHelper.insertVenue(venue);
                }
                Log.d("OSMDataService", "Cached " + venues.size() + " venues locally");
            }).start();
        }
    }

    // Reverse geocoding for coordinates
    public void getAddressFromCoordinates(double lat, double lon, GeocodeCallback callback) {
        String url = NOMINATIM_URL + "?format=json&lat=" + lat + "&lon=" + lon + "&zoom=18";

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "VenueGo/1.0")
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
                        JSONObject json = new JSONObject(jsonResponse);

                        String address = json.optString("display_name", "");
                        String city = json.optJSONObject("address")
                                .optString("city", json.optJSONObject("address")
                                        .optString("town", "Unknown"));

                        callback.onSuccess(address, city);
                    } catch (Exception e) {
                        callback.onFailure(e.getMessage());
                    }
                } else {
                    callback.onFailure("HTTP Error: " + response.code());
                }
            }
        });
    }

    public interface GeocodeCallback {
        void onSuccess(String address, String city);
        void onFailure(String error);
    }

    // Method to fetch venue photos
    public void getVenuePhotos(String venueName, String city, PhotosCallback callback) {
        // This would integrate with Flickr, Google Places, or other photo APIs
        // For now, return placeholder
        List<String> photos = new ArrayList<>();
        photos.add("https://source.unsplash.com/featured/?venue," + city.toLowerCase());
        photos.add("https://source.unsplash.com/featured/?event," + city.toLowerCase());
        photos.add("https://source.unsplash.com/featured/?hall," + city.toLowerCase());

        callback.onSuccess(photos);
    }

    public interface PhotosCallback {
        void onSuccess(List<String> photoUrls);
        void onFailure(String error);
    }
}