package com.arsalankhan.venuego;

import com.arsalankhan.venuego.*;


public class Constants {
    // API Keys (These should be in build.gradle for security)
    public static final String OPEN_WEATHER_API_KEY = "2e67d0f5d7b3d40d7527318533e27d30";
    public static final String RAZORPAY_API_KEY = "EMPTY";
    public static final String GOOGLE_MAPS_API_KEY = "EMPTY";

    // Firebase Collections
    public static final String COLLECTION_VENUES = "venues";
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_BOOKINGS = "bookings";
    public static final String COLLECTION_EVENTS = "events";
    public static final String COLLECTION_REVIEWS = "reviews";

    // Shared Preferences Keys
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_USER_PHONE = "user_phone";
    public static final String PREF_FIRST_LAUNCH = "first_launch";
    public static final String PREF_LAST_SYNC = "last_sync";
    public static final String PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";

    // Request Codes
    public static final int RC_SIGN_IN = 1001;
    public static final int RC_LOCATION_PERMISSION = 1002;
    public static final int RC_CAMERA_PERMISSION = 1003;
    public static final int RC_GALLERY_PERMISSION = 1004;
    public static final int RC_BOOKING_CONFIRMATION = 1005;

    // Intent Extras
    public static final String EXTRA_VENUE_ID = "venue_id";
    public static final String EXTRA_BOOKING_ID = "booking_id";
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_VENUE = "venue";
    public static final String EXTRA_EVENT = "event";
    public static final String EXTRA_FILTERS = "filters";
    public static final String EXTRA_SEARCH_QUERY = "search_query";

    // Notification IDs
    public static final int NOTIFICATION_BOOKING_CONFIRMED = 1;
    public static final int NOTIFICATION_BOOKING_REMINDER = 2;
    public static final int NOTIFICATION_VENUE_RECOMMENDATION = 3;
    public static final int NOTIFICATION_PROMOTION = 4;

    // Cache Durations (in hours)
    public static final long CACHE_DURATION_WEATHER = 24;
    public static final long CACHE_DURATION_VENUES = 168; // 1 week
    public static final long CACHE_DURATION_RECOMMENDATIONS = 72; // 3 days

    // Pagination
    public static final int PAGE_SIZE_VENUES = 20;
    public static final int PAGE_SIZE_BOOKINGS = 15;
    public static final int PAGE_SIZE_REVIEWS = 10;

    // URLs
    public static final String URL_PRIVACY_POLICY = "https://venuego.app/privacy";
    public static final String URL_TERMS_CONDITIONS = "https://venuego.app/terms";
    public static final String URL_SUPPORT = "https://venuego.app/support";
    public static final String URL_FACEBOOK = "https://facebook.com/venuego";
    public static final String URL_INSTAGRAM = "https://instagram.com/venuego";
    public static final String URL_TWITTER = "https://twitter.com/venuego";

    // Default Values
    public static final double DEFAULT_LATITUDE = 19.0760; // Mumbai
    public static final double DEFAULT_LONGITUDE = 72.8777;
    public static final String DEFAULT_CITY = "Mumbai";
    public static final int DEFAULT_GUEST_COUNT = 100;
    public static final double DEFAULT_BUDGET = 100000;

    // Analytics Events
    public static final String EVENT_APP_LAUNCH = "app_launch";
    public static final String EVENT_VENUE_VIEW = "venue_view";
    public static final String EVENT_VENUE_SEARCH = "venue_search";
    public static final String EVENT_BOOKING_CREATED = "booking_created";
    public static final String EVENT_PAYMENT_COMPLETED = "payment_completed";
    public static final String EVENT_USER_SIGNUP = "user_signup";
    public static final String EVENT_USER_LOGIN = "user_login";
}