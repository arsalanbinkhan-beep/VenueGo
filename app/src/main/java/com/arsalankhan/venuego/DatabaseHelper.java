package com.arsalankhan.venuego;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "venuego.db";
    private static final int DATABASE_VERSION = 5;
    private Gson gson = new Gson();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat eventDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Table names
    private static final String TABLE_VENUES = "venues";
    private static final String TABLE_BOOKINGS = "bookings";
    private static final String TABLE_FAVORITES = "favorites";
    private static final String TABLE_AI_RECOMMENDATIONS = "ai_recommendations";
    private static final String TABLE_SEARCH_HISTORY = "search_history";
    private static final String TABLE_WEATHER_CACHE = "weather_cache";

    // Venues table columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_VENUE_ID = "venue_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_ADDRESS = "address";
    private static final String COLUMN_CITY = "city";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_CAPACITY = "capacity";
    private static final String COLUMN_PRICE_RANGE = "price_range";
    private static final String COLUMN_RATING = "rating";
    private static final String COLUMN_REVIEW_COUNT = "review_count";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_IMAGES = "images";
    private static final String COLUMN_AMENITIES = "amenities";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_CONTACT = "contact";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_WEBSITE = "website";
    private static final String COLUMN_UPDATED_AT = "updated_at";

    // User columns
    private static final String COLUMN_USER_ID = "user_id";

    // AI Recommendations table columns
    private static final String COLUMN_RECOMMENDATION_ID = "recommendation_id";
    private static final String COLUMN_EVENT_ID = "event_id";
    private static final String COLUMN_RECOMMENDATION_DATA = "recommendation_data";
    private static final String COLUMN_CREATED_AT = "created_at";

    // Search History table columns
    private static final String COLUMN_SEARCH_ID = "search_id";
    private static final String COLUMN_SEARCH_QUERY = "search_query";
    private static final String COLUMN_SEARCH_FILTERS = "search_filters";
    private static final String COLUMN_RESULT_COUNT = "result_count";

    // Weather Cache table columns
    private static final String COLUMN_WEATHER_ID = "weather_id";
    private static final String COLUMN_WEATHER_LAT = "weather_lat";
    private static final String COLUMN_WEATHER_LNG = "weather_lng";
    private static final String COLUMN_WEATHER_DATE = "weather_date";
    private static final String COLUMN_WEATHER_DATA = "weather_data";
    private static final String COLUMN_EXPIRES_AT = "expires_at";

    // Table creation statements
    private static final String CREATE_TABLE_VENUES =
            "CREATE TABLE " + TABLE_VENUES + "("
                    + COLUMN_ID + " TEXT PRIMARY KEY,"
                    + COLUMN_NAME + " TEXT NOT NULL,"
                    + COLUMN_ADDRESS + " TEXT,"
                    + COLUMN_CITY + " TEXT,"
                    + COLUMN_CATEGORY + " TEXT,"
                    + COLUMN_TYPE + " TEXT,"
                    + COLUMN_CAPACITY + " INTEGER DEFAULT 100,"
                    + COLUMN_PRICE_RANGE + " REAL DEFAULT 0.0,"
                    + COLUMN_RATING + " REAL DEFAULT 0.0,"
                    + COLUMN_REVIEW_COUNT + " INTEGER DEFAULT 0,"
                    + COLUMN_LATITUDE + " REAL,"
                    + COLUMN_LONGITUDE + " REAL,"
                    + COLUMN_IMAGES + " TEXT,"
                    + COLUMN_AMENITIES + " TEXT,"
                    + COLUMN_DESCRIPTION + " TEXT,"
                    + COLUMN_CONTACT + " TEXT,"
                    + COLUMN_EMAIL + " TEXT,"
                    + COLUMN_WEBSITE + " TEXT,"
                    + COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ")";

    private static final String CREATE_TABLE_BOOKINGS =
            "CREATE TABLE " + TABLE_BOOKINGS + "("
                    + "booking_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_VENUE_ID + " TEXT,"
                    + COLUMN_USER_ID + " TEXT,"
                    + "event_name TEXT,"
                    + "event_date TEXT,"
                    + "event_time TEXT,"
                    + "guest_count INTEGER,"
                    + "total_amount REAL,"
                    + "booking_status TEXT DEFAULT 'pending',"
                    + "payment_status TEXT DEFAULT 'pending',"
                    + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "FOREIGN KEY(" + COLUMN_VENUE_ID + ") REFERENCES " + TABLE_VENUES + "(" + COLUMN_ID + ")"
                    + ")";

    private static final String CREATE_TABLE_FAVORITES =
            "CREATE TABLE " + TABLE_FAVORITES + "("
                    + "favorite_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_VENUE_ID + " TEXT,"
                    + COLUMN_USER_ID + " TEXT,"
                    + "added_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE(" + COLUMN_VENUE_ID + ", " + COLUMN_USER_ID + "),"
                    + "FOREIGN KEY(" + COLUMN_VENUE_ID + ") REFERENCES " + TABLE_VENUES + "(" + COLUMN_ID + ")"
                    + ")";

    private static final String CREATE_TABLE_AI_RECOMMENDATIONS =
            "CREATE TABLE " + TABLE_AI_RECOMMENDATIONS + "("
                    + COLUMN_RECOMMENDATION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_EVENT_ID + " TEXT,"
                    + COLUMN_RECOMMENDATION_DATA + " TEXT,"
                    + COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ")";

    private static final String CREATE_TABLE_SEARCH_HISTORY =
            "CREATE TABLE " + TABLE_SEARCH_HISTORY + "("
                    + COLUMN_SEARCH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_USER_ID + " TEXT,"
                    + COLUMN_SEARCH_QUERY + " TEXT,"
                    + COLUMN_SEARCH_FILTERS + " TEXT,"
                    + COLUMN_RESULT_COUNT + " INTEGER,"
                    + "searched_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ")";

    private static final String CREATE_TABLE_WEATHER_CACHE =
            "CREATE TABLE " + TABLE_WEATHER_CACHE + "("
                    + COLUMN_WEATHER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_WEATHER_LAT + " REAL,"
                    + COLUMN_WEATHER_LNG + " REAL,"
                    + COLUMN_WEATHER_DATE + " TEXT,"
                    + COLUMN_WEATHER_DATA + " TEXT,"
                    + COLUMN_EXPIRES_AT + " DATETIME,"
                    + "UNIQUE(" + COLUMN_WEATHER_LAT + ", " + COLUMN_WEATHER_LNG + ", " + COLUMN_WEATHER_DATE + ")"
                    + ")";

    // Index creation statements
    private static final String CREATE_SPATIAL_INDEX =
            "CREATE INDEX idx_venues_location ON " + TABLE_VENUES + "(" + COLUMN_LATITUDE + ", " + COLUMN_LONGITUDE + ")";

    private static final String CREATE_CATEGORY_INDEX =
            "CREATE INDEX idx_venues_category ON " + TABLE_VENUES + "(" + COLUMN_CATEGORY + ")";

    private static final String CREATE_CITY_INDEX =
            "CREATE INDEX idx_venues_city ON " + TABLE_VENUES + "(" + COLUMN_CITY + ")";

    private static final String CREATE_PRICE_INDEX =
            "CREATE INDEX idx_venues_price ON " + TABLE_VENUES + "(" + COLUMN_PRICE_RANGE + ")";

    private static final String CREATE_RATING_INDEX =
            "CREATE INDEX idx_venues_rating ON " + TABLE_VENUES + "(" + COLUMN_RATING + ")";

    private static final String CREATE_TYPE_INDEX =
            "CREATE INDEX idx_venues_type ON " + TABLE_VENUES + "(" + COLUMN_TYPE + ")";

    private static final String CREATE_CAPACITY_INDEX =
            "CREATE INDEX idx_venues_capacity ON " + TABLE_VENUES + "(" + COLUMN_CAPACITY + ")";

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create all tables
        db.execSQL(CREATE_TABLE_VENUES);
        db.execSQL(CREATE_TABLE_BOOKINGS);
        db.execSQL(CREATE_TABLE_FAVORITES);
        db.execSQL(CREATE_TABLE_AI_RECOMMENDATIONS);
        db.execSQL(CREATE_TABLE_SEARCH_HISTORY);
        db.execSQL(CREATE_TABLE_WEATHER_CACHE);

        // Create indexes
        db.execSQL(CREATE_SPATIAL_INDEX);
        db.execSQL(CREATE_CATEGORY_INDEX);
        db.execSQL(CREATE_CITY_INDEX);
        db.execSQL(CREATE_PRICE_INDEX);
        db.execSQL(CREATE_RATING_INDEX);
        db.execSQL(CREATE_TYPE_INDEX);
        db.execSQL(CREATE_CAPACITY_INDEX);

        Log.d("DatabaseHelper", "Database created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop all tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VENUES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AI_RECOMMENDATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCH_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEATHER_CACHE);

        // Drop indexes
        db.execSQL("DROP INDEX IF EXISTS idx_venues_location");
        db.execSQL("DROP INDEX IF EXISTS idx_venues_category");
        db.execSQL("DROP INDEX IF EXISTS idx_venues_city");
        db.execSQL("DROP INDEX IF EXISTS idx_venues_price");
        db.execSQL("DROP INDEX IF EXISTS idx_venues_rating");
        db.execSQL("DROP INDEX IF EXISTS idx_venues_type");
        db.execSQL("DROP INDEX IF EXISTS idx_venues_capacity");

        // Recreate database
        onCreate(db);
        Log.d("DatabaseHelper", "Database upgraded from version " + oldVersion + " to " + newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    // ==================== VENUE METHODS ====================

    public long insertVenue(Venue venue) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_ID, venue.getId());
        values.put(COLUMN_NAME, venue.getName());
        values.put(COLUMN_ADDRESS, venue.getAddress());
        values.put(COLUMN_CITY, venue.getCity());
        values.put(COLUMN_CATEGORY, venue.getCategory());
        values.put(COLUMN_TYPE, venue.getType());
        values.put(COLUMN_CAPACITY, venue.getCapacity());
        values.put(COLUMN_PRICE_RANGE, venue.getPriceRange());
        values.put(COLUMN_RATING, venue.getRating());
        values.put(COLUMN_REVIEW_COUNT, venue.getReviewCount());
        values.put(COLUMN_LATITUDE, venue.getLatitude());
        values.put(COLUMN_LONGITUDE, venue.getLongitude());

        // Convert lists to JSON strings
        if (venue.getImages() != null) {
            values.put(COLUMN_IMAGES, gson.toJson(venue.getImages()));
        }

        if (venue.getAmenities() != null) {
            values.put(COLUMN_AMENITIES, gson.toJson(venue.getAmenities()));
        }

        values.put(COLUMN_DESCRIPTION, venue.getDescription());
        values.put(COLUMN_CONTACT, venue.getContactPhone()); // Fixed: getContactPhone()
        values.put(COLUMN_EMAIL, venue.getContactEmail()); // Fixed: getContactEmail()
        values.put(COLUMN_WEBSITE, venue.getWebsite());
        values.put(COLUMN_UPDATED_AT, dateFormat.format(new Date()));

        // Insert or replace
        long result = db.insertWithOnConflict(TABLE_VENUES, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        db.close();

        Log.d("DatabaseHelper", "Inserted venue: " + venue.getName() + " (ID: " + venue.getId() + ")");
        return result;
    }

    public Venue getVenue(String venueId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Venue venue = null;

        String query = "SELECT * FROM " + TABLE_VENUES + " WHERE " + COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{venueId});

        if (cursor.moveToFirst()) {
            venue = cursorToVenue(cursor);
        }

        cursor.close();
        db.close();
        return venue;
    }

    public List<Venue> getAllVenues() {
        List<Venue> venues = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_VENUES + " ORDER BY " + COLUMN_NAME;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                venues.add(cursorToVenue(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return venues;
    }

    public List<Venue> getVenuesNearby(double lat, double lon, double radiusKm) {
        List<Venue> venues = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT *, "
                + "(6371 * acos(cos(radians(?)) * cos(radians(" + COLUMN_LATITUDE + ")) * "
                + "cos(radians(" + COLUMN_LONGITUDE + ") - radians(?)) + "
                + "sin(radians(?)) * sin(radians(" + COLUMN_LATITUDE + ")))) AS distance "
                + "FROM " + TABLE_VENUES + " "
                + "WHERE distance <= ? "
                + "ORDER BY distance "
                + "LIMIT 50";

        String[] args = {
                String.valueOf(lat),
                String.valueOf(lon),
                String.valueOf(lat),
                String.valueOf(radiusKm)
        };

        Cursor cursor = db.rawQuery(query, args);

        if (cursor.moveToFirst()) {
            do {
                Venue venue = cursorToVenue(cursor);
                venues.add(venue);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return venues;
    }

    public List<Venue> searchVenuesWithFilters(String city, String category, String type,
                                               int minCapacity, double maxPrice) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Venue> venues = new ArrayList<>();

        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + TABLE_VENUES + " WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (city != null && !city.isEmpty()) {
            queryBuilder.append(" AND ").append(COLUMN_CITY).append(" LIKE ?");
            args.add("%" + city + "%");
        }

        if (category != null && !category.isEmpty()) {
            queryBuilder.append(" AND ").append(COLUMN_CATEGORY).append(" = ?");
            args.add(category);
        }

        if (type != null && !type.isEmpty()) {
            queryBuilder.append(" AND ").append(COLUMN_TYPE).append(" = ?");
            args.add(type);
        }

        queryBuilder.append(" AND ").append(COLUMN_CAPACITY).append(" >= ?");
        args.add(String.valueOf(minCapacity));

        queryBuilder.append(" AND ").append(COLUMN_PRICE_RANGE).append(" <= ?");
        args.add(String.valueOf(maxPrice));

        queryBuilder.append(" ORDER BY ").append(COLUMN_RATING).append(" DESC");
        queryBuilder.append(" LIMIT 100");

        Cursor cursor = db.rawQuery(queryBuilder.toString(), args.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                venues.add(cursorToVenue(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return venues;
    }

    public List<Venue> advancedSearch(Map<String, Object> filters) {
        List<Venue> venues = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + TABLE_VENUES + " WHERE 1=1");
        List<String> selectionArgs = new ArrayList<>();

        // Apply filters
        if (filters.containsKey("city")) {
            queryBuilder.append(" AND ").append(COLUMN_CITY).append(" LIKE ?");
            selectionArgs.add("%" + filters.get("city") + "%");
        }

        if (filters.containsKey("category")) {
            queryBuilder.append(" AND ").append(COLUMN_CATEGORY).append(" = ?");
            selectionArgs.add(filters.get("category").toString());
        }

        if (filters.containsKey("type")) {
            queryBuilder.append(" AND ").append(COLUMN_TYPE).append(" = ?");
            selectionArgs.add(filters.get("type").toString());
        }

        if (filters.containsKey("minCapacity")) {
            queryBuilder.append(" AND ").append(COLUMN_CAPACITY).append(" >= ?");
            selectionArgs.add(filters.get("minCapacity").toString());
        }

        if (filters.containsKey("maxPrice")) {
            queryBuilder.append(" AND ").append(COLUMN_PRICE_RANGE).append(" <= ?");
            selectionArgs.add(filters.get("maxPrice").toString());
        }

        if (filters.containsKey("minRating")) {
            queryBuilder.append(" AND ").append(COLUMN_RATING).append(" >= ?");
            selectionArgs.add(filters.get("minRating").toString());
        }

        // Sort order
        String sortBy = filters.containsKey("sortBy") ? filters.get("sortBy").toString() : COLUMN_RATING;
        String sortOrder = filters.containsKey("sortOrder") ? filters.get("sortOrder").toString() : "DESC";

        queryBuilder.append(" ORDER BY ").append(sortBy).append(" ").append(sortOrder);
        queryBuilder.append(" LIMIT 100");

        Cursor cursor = db.rawQuery(queryBuilder.toString(),
                selectionArgs.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                Venue venue = cursorToVenue(cursor);
                venues.add(venue);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return venues;
    }

    public int deleteVenue(String venueId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_VENUES, COLUMN_ID + " = ?", new String[]{venueId});
        db.close();
        return result;
    }

    // ==================== VENUE SCORING METHODS ====================

    public List<VenueScore> getVenuesWithScore(double lat, double lon,
                                               int guestCount, double budget,
                                               String eventType) {
        List<VenueScore> scoredVenues = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT *, "
                + "(6371 * acos(cos(radians(?)) * cos(radians(" + COLUMN_LATITUDE + ")) * "
                + "cos(radians(" + COLUMN_LONGITUDE + ") - radians(?)) + "
                + "sin(radians(?)) * sin(radians(" + COLUMN_LATITUDE + ")))) AS distance "
                + "FROM " + TABLE_VENUES + " "
                + "WHERE " + COLUMN_CAPACITY + " >= ? "
                + "AND " + COLUMN_PRICE_RANGE + " <= ? "
                + "ORDER BY distance "
                + "LIMIT 100";

        String[] selectionArgs = {
                String.valueOf(lat),
                String.valueOf(lon),
                String.valueOf(lat),
                String.valueOf(guestCount),
                String.valueOf(budget * 1.2) // Allow 20% over budget for scoring
        };

        Cursor cursor = db.rawQuery(query, selectionArgs);

        if (cursor.moveToFirst()) {
            do {
                Venue venue = cursorToVenue(cursor);

                // Get distance safely
                int distanceIndex = cursor.getColumnIndex("distance");
                double distance = 0;
                if (distanceIndex >= 0) {
                    distance = cursor.getDouble(distanceIndex);
                }

                // Calculate score
                double score = calculateLocalScore(venue, guestCount, budget, distance, eventType);
                scoredVenues.add(new VenueScore(venue, score));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        // Sort by score descending
        scoredVenues.sort((a, b) -> Double.compare(b.score, a.score));
        return scoredVenues;
    }

    private double calculateLocalScore(Venue venue, int guestCount, double budget,
                                       double distance, String eventType) {
        double totalScore = 0;

        // Capacity match (30%)
        double capacityScore = 100 - Math.abs(venue.getCapacity() - guestCount) * 100.0 / Math.max(guestCount, 1);
        capacityScore = Math.max(0, Math.min(100, capacityScore));
        totalScore += capacityScore * 0.3;

        // Budget match (25%)
        double budgetScore = 100 - (Math.max(0, venue.getPriceRange() - budget) * 100.0 / Math.max(budget, 1));
        budgetScore = Math.max(0, Math.min(100, budgetScore));
        totalScore += budgetScore * 0.25;

        // Distance score (20%)
        double distanceScore = Math.max(0, 100 - (distance * 10)); // 10 points per km
        totalScore += distanceScore * 0.2;

        // Rating score (15%)
        double ratingScore = venue.getRating() * 20; // Convert 0-5 to 0-100
        totalScore += ratingScore * 0.15;

        // Category suitability (10%)
        double categoryScore = isCategorySuitable(venue.getCategory(), eventType) ? 100 : 50;
        totalScore += categoryScore * 0.1;

        return totalScore;
    }

    private boolean isCategorySuitable(String category, String eventType) {
        Map<String, List<String>> suitabilityMap = new HashMap<>();
        suitabilityMap.put("wedding", Arrays.asList("banquet_hall", "hotel", "community_center"));
        suitabilityMap.put("corporate", Arrays.asList("conference_center", "auditorium", "hotel"));
        suitabilityMap.put("party", Arrays.asList("open_ground", "banquet_hall", "restaurant"));
        suitabilityMap.put("sports", Arrays.asList("stadium", "open_ground"));
        suitabilityMap.put("conference", Arrays.asList("conference_center", "auditorium", "hotel"));
        suitabilityMap.put("birthday", Arrays.asList("restaurant", "banquet_hall", "open_ground"));
        suitabilityMap.put("exhibition", Arrays.asList("conference_center", "exhibition_hall", "open_ground"));

        List<String> suitable = suitabilityMap.getOrDefault(eventType.toLowerCase(),
                Arrays.asList("event_venue"));
        return suitable.contains(category);
    }

    // ==================== CURSOR TO VENUE CONVERSION ====================

    private Venue cursorToVenue(Cursor cursor) {
        Venue venue = new Venue();

        venue.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        venue.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
        venue.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)));
        venue.setCity(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CITY)));
        venue.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
        venue.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
        venue.setCapacity(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAPACITY)));
        venue.setPriceRange(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE_RANGE)));
        venue.setRating(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_RATING)));
        venue.setReviewCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REVIEW_COUNT)));
        venue.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)));
        venue.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)));
        venue.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));

        // FIXED: Use correct column names
        venue.setContactPhone(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT)));
        venue.setContactEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));

        venue.setWebsite(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WEBSITE)));

        // Parse JSON strings for lists
        String imagesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGES));
        if (imagesJson != null && !imagesJson.isEmpty()) {
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> images = gson.fromJson(imagesJson, listType);
            venue.setImages(images);
        }

        String amenitiesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AMENITIES));
        if (amenitiesJson != null && !amenitiesJson.isEmpty()) {
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> amenities = gson.fromJson(amenitiesJson, listType);
            venue.setAmenities(amenities);
        }

        return venue;
    }

    // ==================== BOOKING METHODS ====================

    public long addBooking(String venueId, String userId, String eventName, String eventDate,
                           String eventTime, int guestCount, double totalAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_VENUE_ID, venueId);
        values.put(COLUMN_USER_ID, userId);
        values.put("event_name", eventName);
        values.put("event_date", eventDate);
        values.put("event_time", eventTime);
        values.put("guest_count", guestCount);
        values.put("total_amount", totalAmount);

        long result = db.insert(TABLE_BOOKINGS, null, values);
        db.close();
        return result;
    }

    public List<Booking> getUserBookings(String userId) {
        List<Booking> bookings = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT b.*, v." + COLUMN_NAME + " as venue_name, v." + COLUMN_ADDRESS +
                " FROM " + TABLE_BOOKINGS + " b " +
                "LEFT JOIN " + TABLE_VENUES + " v ON b." + COLUMN_VENUE_ID + " = v." + COLUMN_ID +
                " WHERE b." + COLUMN_USER_ID + " = ? " +
                "ORDER BY b.created_at DESC";

        Cursor cursor = db.rawQuery(query, new String[]{userId});

        if (cursor.moveToFirst()) {
            do {
                Booking booking = new Booking();
                booking.setBookingId(cursor.getInt(cursor.getColumnIndexOrThrow("booking_id")));
                booking.setVenueId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VENUE_ID)));
                booking.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
                booking.setEventName(cursor.getString(cursor.getColumnIndexOrThrow("event_name")));

                // FIXED: Parse date string to Date object
                String eventDateStr = cursor.getString(cursor.getColumnIndexOrThrow("event_date"));
                if (eventDateStr != null) {
                    try {
                        Date eventDate = eventDateFormat.parse(eventDateStr);
                        booking.setEventDate(eventDate);
                    } catch (ParseException e) {
                        Log.e("DatabaseHelper", "Error parsing event date: " + e.getMessage());
                    }
                }

                booking.setEventTime(cursor.getString(cursor.getColumnIndexOrThrow("event_time")));
                booking.setGuestCount(cursor.getInt(cursor.getColumnIndexOrThrow("guest_count")));
                booking.setTotalAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount")));
                booking.setBookingStatus(cursor.getString(cursor.getColumnIndexOrThrow("booking_status")));
                booking.setPaymentStatus(cursor.getString(cursor.getColumnIndexOrThrow("payment_status")));

                // FIXED: Parse created_at string to Date object
                String createdAtStr = cursor.getString(cursor.getColumnIndexOrThrow("created_at"));
                if (createdAtStr != null) {
                    try {
                        Date createdAt = dateFormat.parse(createdAtStr);
                        booking.setCreatedAt(String.valueOf(createdAt));
                    } catch (ParseException e) {
                        Log.e("DatabaseHelper", "Error parsing created_at: " + e.getMessage());
                    }
                }

                booking.setVenueName(cursor.getString(cursor.getColumnIndexOrThrow("venue_name")));
                booking.setVenueAddress(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)));

                bookings.add(booking);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return bookings;
    }

    // ==================== FAVORITES METHODS ====================

    public long addFavorite(String venueId, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_VENUE_ID, venueId);
        values.put(COLUMN_USER_ID, userId);

        long result = db.insertWithOnConflict(TABLE_FAVORITES, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return result;
    }

    public boolean removeFavorite(String venueId, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_FAVORITES,
                COLUMN_VENUE_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{venueId, userId});
        db.close();
        return result > 0;
    }

    public List<Venue> getUserFavorites(String userId) {
        List<Venue> favorites = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT v.* FROM " + TABLE_VENUES + " v " +
                "INNER JOIN " + TABLE_FAVORITES + " f ON v." + COLUMN_ID + " = f." + COLUMN_VENUE_ID +
                " WHERE f." + COLUMN_USER_ID + " = ? " +
                "ORDER BY f.added_at DESC";

        Cursor cursor = db.rawQuery(query, new String[]{userId});

        if (cursor.moveToFirst()) {
            do {
                favorites.add(cursorToVenue(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return favorites;
    }

    public boolean isFavorite(String venueId, String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT 1 FROM " + TABLE_FAVORITES +
                " WHERE " + COLUMN_VENUE_ID + " = ? AND " + COLUMN_USER_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{venueId, userId});
        boolean isFavorite = cursor.moveToFirst();

        cursor.close();
        db.close();
        return isFavorite;
    }

    // ==================== AI RECOMMENDATION METHODS ====================

    public long saveAIRecommendation(String eventId, List<Venue> recommendations) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_EVENT_ID, eventId);
        values.put(COLUMN_RECOMMENDATION_DATA, gson.toJson(recommendations));

        long result = db.insert(TABLE_AI_RECOMMENDATIONS, null, values);
        db.close();
        return result;
    }

    public List<Venue> getAIRecommendations(String eventId) {
        List<Venue> recommendations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " + COLUMN_RECOMMENDATION_DATA + " FROM " +
                TABLE_AI_RECOMMENDATIONS + " WHERE " + COLUMN_EVENT_ID + " = ? " +
                "ORDER BY " + COLUMN_CREATED_AT + " DESC LIMIT 1";

        Cursor cursor = db.rawQuery(query, new String[]{eventId});

        if (cursor.moveToFirst()) {
            String jsonData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECOMMENDATION_DATA));
            Type listType = new TypeToken<List<Venue>>(){}.getType();
            recommendations = gson.fromJson(jsonData, listType);
        }

        cursor.close();
        db.close();
        return recommendations;
    }

    // ==================== WEATHER CACHE METHODS ====================

    public void cacheWeather(double lat, double lng, String date, String weatherData, long expiresInHours) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_WEATHER_LAT, lat);
        values.put(COLUMN_WEATHER_LNG, lng);
        values.put(COLUMN_WEATHER_DATE, date);
        values.put(COLUMN_WEATHER_DATA, weatherData);

        // Set expiration (default 24 hours)
        long expiresAt = System.currentTimeMillis() + (expiresInHours * 3600000);
        values.put(COLUMN_EXPIRES_AT, expiresAt);

        db.insertWithOnConflict(TABLE_WEATHER_CACHE, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public String getCachedWeather(double lat, double lng, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String weatherData = null;

        String query = "SELECT " + COLUMN_WEATHER_DATA + " FROM " + TABLE_WEATHER_CACHE +
                " WHERE " + COLUMN_WEATHER_LAT + " = ? AND " + COLUMN_WEATHER_LNG + " = ? " +
                " AND " + COLUMN_WEATHER_DATE + " = ? AND " + COLUMN_EXPIRES_AT + " > ?";

        String[] selectionArgs = {
                String.valueOf(lat),
                String.valueOf(lng),
                date,
                String.valueOf(System.currentTimeMillis())
        };

        Cursor cursor = db.rawQuery(query, selectionArgs);

        if (cursor.moveToFirst()) {
            weatherData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WEATHER_DATA));
        }

        cursor.close();
        db.close();
        return weatherData;
    }

    // ==================== SEARCH HISTORY METHODS ====================

    public void saveSearchHistory(String userId, String query, Map<String, Object> filters, int resultCount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_SEARCH_QUERY, query);
        values.put(COLUMN_SEARCH_FILTERS, gson.toJson(filters));
        values.put(COLUMN_RESULT_COUNT, resultCount);

        db.insert(TABLE_SEARCH_HISTORY, null, values);
        db.close();
    }

    public List<SearchHistory> getRecentSearches(String userId, int limit) {
        List<SearchHistory> searches = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_SEARCH_HISTORY +
                " WHERE " + COLUMN_USER_ID + " = ? " +
                "ORDER BY searched_at DESC LIMIT ?";

        Cursor cursor = db.rawQuery(query, new String[]{userId, String.valueOf(limit)});

        if (cursor.moveToFirst()) {
            do {
                SearchHistory search = new SearchHistory();
                search.setSearchId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SEARCH_ID)));
                search.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
                search.setSearchQuery(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SEARCH_QUERY)));

                String filtersJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SEARCH_FILTERS));
                if (filtersJson != null) {
                    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> filters = gson.fromJson(filtersJson, mapType);
                    search.setFilters(filters);
                }

                search.setResultCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RESULT_COUNT)));
                search.setSearchedAt(cursor.getString(cursor.getColumnIndexOrThrow("searched_at")));

                searches.add(search);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return searches;
    }

    // ==================== HELPER CLASSES ====================

    public class VenueScore {
        public Venue venue;
        public double score;

        public VenueScore(Venue venue, double score) {
            this.venue = venue;
            this.score = score;
        }
    }

    // ==================== DATABASE MAINTENANCE ====================

    public void clearOldWeatherCache() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEATHER_CACHE, COLUMN_EXPIRES_AT + " < ?",
                new String[]{String.valueOf(System.currentTimeMillis())});
        db.close();
    }

    public void clearOldSearchHistory(int daysToKeep) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_SEARCH_HISTORY +
                " WHERE searched_at < datetime('now', '-" + daysToKeep + " days')";
        db.execSQL(query);
        db.close();
    }

    public void vacuumDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("VACUUM");
        db.close();
    }

    // ==================== STATISTICS ====================

    public int getVenueCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_VENUES;
        Cursor cursor = db.rawQuery(query, null);
        int count = 0;

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }

    public Map<String, Integer> getVenueStats() {
        Map<String, Integer> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Total venues
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_VENUES;
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.moveToFirst()) {
            stats.put("total_venues", cursor.getInt(0));
        }
        cursor.close();

        // By city
        String cityQuery = "SELECT " + COLUMN_CITY + ", COUNT(*) as count FROM " + TABLE_VENUES +
                " GROUP BY " + COLUMN_CITY;
        cursor = db.rawQuery(cityQuery, null);
        while (cursor.moveToNext()) {
            String city = cursor.getString(0);
            int count = cursor.getInt(1);
            stats.put("city_" + city, count);
        }
        cursor.close();

        // By category
        String categoryQuery = "SELECT " + COLUMN_CATEGORY + ", COUNT(*) as count FROM " + TABLE_VENUES +
                " GROUP BY " + COLUMN_CATEGORY;
        cursor = db.rawQuery(categoryQuery, null);
        while (cursor.moveToNext()) {
            String category = cursor.getString(0);
            int count = cursor.getInt(1);
            stats.put("category_" + category, count);
        }
        cursor.close();

        db.close();
        return stats;
    }
    // Add these methods to your existing DatabaseHelper class:

    public void incrementVenueViews(String venueId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("view_count", getVenueViews(venueId) + 1);
        values.put(COLUMN_UPDATED_AT, dateFormat.format(new Date()));

        db.update(TABLE_VENUES, values, COLUMN_ID + " = ?", new String[]{venueId});
        db.close();
    }

    private int getVenueViews(String venueId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int views = 0;

        String query = "SELECT view_count FROM " + TABLE_VENUES +
                " WHERE " + COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{venueId});

        if (cursor.moveToFirst()) {
            views = cursor.getInt(cursor.getColumnIndexOrThrow("view_count"));
        }

        cursor.close();
        db.close();
        return views;
    }

    public List<Venue> getTrendingVenues(int limit) {
        List<Venue> venues = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_VENUES +
                " ORDER BY view_count DESC, rating DESC" +
                " LIMIT " + limit;

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                venues.add(cursorToVenue(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return venues;
    }

    public List<Venue> getRecommendedVenues(String userId) {
        List<Venue> venues = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Get user's booking history
        List<Booking> bookings = getUserBookings(userId);

        if (bookings.isEmpty()) {
            // If no bookings, return trending venues
            return getTrendingVenues(10);
        }

        // Analyze user preferences from booking history
        String preferredCategory = getPreferredCategory(bookings);
        String preferredCity = getPreferredCity(bookings);
        int avgGuestCount = getAverageGuestCount(bookings);

        // Query venues matching preferences
        String query = "SELECT * FROM " + TABLE_VENUES +
                " WHERE " + COLUMN_CATEGORY + " LIKE ?" +
                " AND " + COLUMN_CITY + " LIKE ?" +
                " AND " + COLUMN_CAPACITY + " >= ?" +
                " ORDER BY rating DESC" +
                " LIMIT 10";

        Cursor cursor = db.rawQuery(query, new String[]{
                "%" + preferredCategory + "%",
                "%" + preferredCity + "%",
                String.valueOf(avgGuestCount)
        });

        if (cursor.moveToFirst()) {
            do {
                venues.add(cursorToVenue(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return venues;
    }

    private String getPreferredCategory(List<Booking> bookings) {
        // Simple implementation - return most frequent category
        Map<String, Integer> categoryCount = new HashMap<>();

        for (Booking booking : bookings) {
            // Get venue category from venueId
            Venue venue = getVenue(booking.getVenueId());
            if (venue != null) {
                String category = venue.getCategory();
                categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
            }
        }

        return categoryCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("banquet_hall");
    }

    private String getPreferredCity(List<Booking> bookings) {
        // Similar implementation for city
        return "Mumbai"; // Default
    }

    private int getAverageGuestCount(List<Booking> bookings) {
        if (bookings.isEmpty()) return 100;

        int total = 0;
        for (Booking booking : bookings) {
            total += booking.getGuestCount();
        }

        return total / bookings.size();
    }
}
