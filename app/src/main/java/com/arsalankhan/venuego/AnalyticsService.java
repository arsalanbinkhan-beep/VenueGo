package com.arsalankhan.venuego;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsService {
    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseCrashlytics crashlytics;
    private Context context;
    private DatabaseHelper databaseHelper;

    public AnalyticsService(Context context) {
        this.context = context;
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        crashlytics = FirebaseCrashlytics.getInstance();
        databaseHelper = new DatabaseHelper(context);
    }

    // User Analytics
    public void logUserSignup(String userId, String method) {
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        bundle.putString("signup_method", method);
        firebaseAnalytics.logEvent("user_signup", bundle);

        crashlytics.setUserId(userId);
    }

    public void logUserLogin(String userId) {
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        firebaseAnalytics.logEvent("user_login", bundle);
    }

    // Venue Analytics
    public void logVenueView(String venueId, String venueName, String category) {
        Bundle bundle = new Bundle();
        bundle.putString("venue_id", venueId);
        bundle.putString("venue_name", venueName);
        bundle.putString("venue_category", category);
        firebaseAnalytics.logEvent("venue_view", bundle);

        // Update view count in local database
        databaseHelper.incrementVenueViews(venueId);
    }

    public void logVenueSearch(String query, Map<String, Object> filters, int resultCount) {
        Bundle bundle = new Bundle();
        bundle.putString("search_query", query);
        bundle.putString("filters", filters.toString());
        bundle.putInt("result_count", resultCount);
        firebaseAnalytics.logEvent("venue_search", bundle);
    }

    public void logVenueBooking(String venueId, String bookingId, double amount) {
        Bundle bundle = new Bundle();
        bundle.putString("venue_id", venueId);
        bundle.putString("booking_id", bookingId);
        bundle.putDouble("booking_amount", amount);
        firebaseAnalytics.logEvent("venue_booking", bundle);
    }

    // Event Analytics
    public void logEventCreated(String eventId, String eventType, int guestCount) {
        Bundle bundle = new Bundle();
        bundle.putString("event_id", eventId);
        bundle.putString("event_type", eventType);
        bundle.putInt("guest_count", guestCount);
        firebaseAnalytics.logEvent("event_created", bundle);
    }

    // AI Recommendation Analytics
    public void logAIRecommendation(String eventId, int recommendationCount, String weatherCondition) {
        Bundle bundle = new Bundle();
        bundle.putString("event_id", eventId);
        bundle.putInt("recommendation_count", recommendationCount);
        bundle.putString("weather_condition", weatherCondition);
        firebaseAnalytics.logEvent("ai_recommendation", bundle);
    }

    // Error Logging
    public void logError(String errorType, String errorMessage, Map<String, String> context) {
        Bundle bundle = new Bundle();
        bundle.putString("error_type", errorType);
        bundle.putString("error_message", errorMessage);

        if (context != null) {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                bundle.putString(entry.getKey(), entry.getValue());
            }
        }

        firebaseAnalytics.logEvent("error_occurred", bundle);

        // Also log to Crashlytics
        crashlytics.log(errorType + ": " + errorMessage);
        if (context != null) {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                crashlytics.setCustomKey(entry.getKey(), entry.getValue());
            }
        }
    }

    // Performance Monitoring
    public Trace startTrace(String traceName) {
        Trace trace = FirebasePerformance.getInstance().newTrace(traceName);
        trace.start();
        return trace;
    }

    public void stopTrace(Trace trace) {
        if (trace != null) {
            trace.stop();
        }
    }

    // User Properties
    public void setUserProperty(String propertyName, String propertyValue) {
        firebaseAnalytics.setUserProperty(propertyName, propertyValue);
    }

    // Screen Tracking
    public void setCurrentScreen(String screenName, String screenClass) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    // Revenue Tracking
    public void logPurchase(String transactionId, double revenue, String currency) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.TRANSACTION_ID, transactionId);
        bundle.putDouble(FirebaseAnalytics.Param.VALUE, revenue);
        bundle.putString(FirebaseAnalytics.Param.CURRENCY, currency);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, bundle);
    }

    // Custom Event
    public void logCustomEvent(String eventName, Map<String, Object> params) {
        Bundle bundle = new Bundle();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    bundle.putString(entry.getKey(), (String) value);
                } else if (value instanceof Integer) {
                    bundle.putInt(entry.getKey(), (Integer) value);
                } else if (value instanceof Double) {
                    bundle.putDouble(entry.getKey(), (Double) value);
                } else if (value instanceof Long) {
                    bundle.putLong(entry.getKey(), (Long) value);
                } else if (value instanceof Boolean) {
                    bundle.putBoolean(entry.getKey(), (Boolean) value);
                }
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle);
    }

    // App Performance Metrics
    public void logAppStartTime(long startTime) {
        long loadTime = System.currentTimeMillis() - startTime;
        Bundle bundle = new Bundle();
        bundle.putLong("app_start_time", loadTime);
        firebaseAnalytics.logEvent("app_start_performance", bundle);
    }

    public void logAPIResponseTime(String apiName, long responseTime) {
        Bundle bundle = new Bundle();
        bundle.putString("api_name", apiName);
        bundle.putLong("response_time", responseTime);
        firebaseAnalytics.logEvent("api_performance", bundle);
    }

    // Database Performance
    public void logDatabaseOperation(String operation, long executionTime) {
        Bundle bundle = new Bundle();
        bundle.putString("db_operation", operation);
        bundle.putLong("execution_time", executionTime);
        firebaseAnalytics.logEvent("database_performance", bundle);
    }
}