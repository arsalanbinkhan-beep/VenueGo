// Enhanced WeatherService.java with caching
package com.arsalankhan.venuego;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherService {
    private static final String OPEN_WEATHER_API_KEY = "YOUR_API_KEY_HERE"; // Replace with your API key
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    private OkHttpClient client;
    private DatabaseHelper databaseHelper;
    private Gson gson;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public WeatherService(Context context) {
        this.client = new OkHttpClient();
        this.databaseHelper = new DatabaseHelper(context);
        this.gson = new Gson();
    }

    public interface WeatherCallback {
        void onSuccess(WeatherForecast forecast);
        void onFailure(String error);
    }

    public void getWeatherForecast(double lat, double lon, Date date, WeatherCallback callback) {
        String dateString = dateFormat.format(date);

        // Check cache first
        String cachedData = databaseHelper.getCachedWeather(lat, lon, dateString);
        if (cachedData != null) {
            try {
                WeatherForecast forecast = gson.fromJson(cachedData, WeatherForecast.class);
                callback.onSuccess(forecast);
                return;
            } catch (Exception e) {
                Log.w("WeatherService", "Failed to parse cached weather data");
            }
        }

        // If not cached or expired, fetch from API
        fetchFromAPI(lat, lon, date, dateString, callback);
    }

    private void fetchFromAPI(double lat, double lon, Date date, String dateString, WeatherCallback callback) {
        // Use forecast API for future dates
        String url;
        if (isWithin5Days(date)) {
            url = BASE_URL + "/forecast?lat=" + lat + "&lon=" + lon +
                    "&appid=" + OPEN_WEATHER_API_KEY + "&units=metric&cnt=40";
        } else {
            // For dates beyond 5 days, use historical data (requires premium subscription)
            // Fallback to current weather
            url = BASE_URL + "/weather?lat=" + lat + "&lon=" + lon +
                    "&appid=" + OPEN_WEATHER_API_KEY + "&units=metric";
        }

        Request request = new Request.Builder()
                .url(url)
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
                        String jsonData = response.body().string();
                        WeatherForecast forecast = parseWeatherData(jsonData, dateString);

                        // Cache the result
                        databaseHelper.cacheWeather(lat, lon, dateString,
                                gson.toJson(forecast), 24);

                        callback.onSuccess(forecast);
                    } catch (Exception e) {
                        callback.onFailure(e.getMessage());
                    }
                } else {
                    callback.onFailure("HTTP Error: " + response.code());
                }
            }
        });
    }

    private boolean isWithin5Days(Date date) {
        long diff = date.getTime() - System.currentTimeMillis();
        return diff <= 5 * 24 * 60 * 60 * 1000; // 5 days in milliseconds
    }

    private WeatherForecast parseWeatherData(String jsonData, String targetDate) throws Exception {
        JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();

        if (json.has("list")) {
            // Forecast data - find closest to target date
            return parseForecastData(json, targetDate);
        } else {
            // Current weather data
            return parseCurrentWeatherData(json);
        }
    }

    private WeatherForecast parseForecastData(JsonObject json, String targetDate) throws Exception {
        // Find forecast entry closest to target date
        // For simplicity, using first entry
        JsonObject forecast = json.getAsJsonArray("list").get(0).getAsJsonObject();

        String condition = forecast.getAsJsonArray("weather")
                .get(0).getAsJsonObject()
                .get("main").getAsString();

        double temperature = forecast.getAsJsonObject("main")
                .get("temp").getAsDouble();

        int humidity = forecast.getAsJsonObject("main")
                .get("humidity").getAsInt();

        double windSpeed = forecast.getAsJsonObject("wind")
                .get("speed").getAsDouble();

        double precipitation = 0;
        if (forecast.has("rain")) {
            precipitation = forecast.getAsJsonObject("rain")
                    .get("3h").getAsDouble();
        }

        return new WeatherForecast(condition, temperature, humidity, windSpeed, precipitation);
    }

    private WeatherForecast parseCurrentWeatherData(JsonObject json) throws Exception {
        String condition = json.getAsJsonArray("weather")
                .get(0).getAsJsonObject()
                .get("main").getAsString();

        double temperature = json.getAsJsonObject("main")
                .get("temp").getAsDouble();

        int humidity = json.getAsJsonObject("main")
                .get("humidity").getAsInt();

        double windSpeed = json.getAsJsonObject("wind")
                .get("speed").getAsDouble();

        return new WeatherForecast(condition, temperature, humidity, windSpeed, 0);
    }

    // Weather impact analysis
    public WeatherImpact analyzeWeatherImpact(WeatherForecast forecast, String eventType) {
        WeatherImpact impact = new WeatherImpact();

        if (forecast.isRainy()) {
            impact.setRecommendation("Strongly recommend indoor venue");
            impact.setRiskLevel("HIGH");
            impact.setRiskDetails("High chance of rain (" + forecast.getPrecipitation() + "mm)");
            impact.setAlternativeSuggestions("Consider venues with covered outdoor areas or indoor options");
        } else if (forecast.isExtremeHeat()) {
            impact.setRecommendation("Recommend air-conditioned indoor venue");
            impact.setRiskLevel("MEDIUM");
            impact.setRiskDetails("High temperature: " + forecast.getTemperature() + "°C");
            impact.setAlternativeSuggestions("Ensure venue has proper cooling and ventilation");
        } else if (forecast.isGoodWeather()) {
            impact.setRecommendation("Perfect for outdoor events");
            impact.setRiskLevel("LOW");
            impact.setRiskDetails("Clear weather with pleasant temperature");
            impact.setAlternativeSuggestions("Both indoor and outdoor options are suitable");
        } else {
            impact.setRecommendation("Monitor weather updates");
            impact.setRiskLevel("MEDIUM");
            impact.setRiskDetails("Variable weather conditions");
            impact.setAlternativeSuggestions("Have backup indoor option");
        }

        // Event-specific adjustments
        if (eventType.equalsIgnoreCase("wedding")) {
            if (forecast.isRainy()) {
                impact.setRecommendation("Must use indoor venue for wedding");
                impact.setRiskLevel("VERY HIGH");
            }
        } else if (eventType.equalsIgnoreCase("sports")) {
            if (forecast.isRainy()) {
                impact.setRecommendation("Consider indoor sports facility or postpone");
                impact.setRiskLevel("HIGH");
            }
        }

        return impact;
    }
}

// Enhanced WeatherForecast class
class WeatherForecast {
    private String condition;
    private double temperature;
    private int humidity;
    private double windSpeed;
    private double precipitation;
    private String icon;
    private long timestamp;

    public WeatherForecast(String condition, double temperature, int humidity,
                           double windSpeed, double precipitation) {
        this.condition = condition;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.precipitation = precipitation;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getCondition() { return condition; }
    public double getTemperature() { return temperature; }
    public int getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }
    public double getPrecipitation() { return precipitation; }
    public long getTimestamp() { return timestamp; }

    // Weather condition checks
    public boolean isRainy() {
        return condition.toLowerCase().contains("rain") ||
                condition.toLowerCase().contains("drizzle") ||
                precipitation > 1.0;
    }

    public boolean isExtremeHeat() {
        return temperature > 35.0;
    }

    public boolean isCold() {
        return temperature < 15.0;
    }

    public boolean isWindy() {
        return windSpeed > 20.0; // km/h
    }

    public boolean isGoodWeather() {
        return !isRainy() && !isExtremeHeat() && !isCold() && !isWindy() &&
                temperature >= 18 && temperature <= 30;
    }

    public String getWeatherDescription() {
        if (isRainy()) {
            return "Rainy - " + precipitation + "mm expected";
        } else if (isExtremeHeat()) {
            return "Hot - " + temperature + "°C";
        } else if (isCold()) {
            return "Cold - " + temperature + "°C";
        } else if (isWindy()) {
            return "Windy - " + windSpeed + " km/h winds";
        } else {
            return "Pleasant - " + temperature + "°C";
        }
    }
}

// Weather Impact Analysis
class WeatherImpact {
    private String recommendation;
    private String riskLevel;
    private String riskDetails;
    private String alternativeSuggestions;

    // Getters and setters
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getRiskDetails() { return riskDetails; }
    public void setRiskDetails(String riskDetails) { this.riskDetails = riskDetails; }

    public String getAlternativeSuggestions() { return alternativeSuggestions; }
    public void setAlternativeSuggestions(String alternativeSuggestions) {
        this.alternativeSuggestions = alternativeSuggestions;
    }
}