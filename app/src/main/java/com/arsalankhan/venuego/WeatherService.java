package com.arsalankhan.venuego;

import android.util.Log;

import org.json.JSONObject;

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
    private static final String OPEN_WEATHER_API_KEY = "YOUR_API_KEY"; // Get from https://openweathermap.org/api
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    private OkHttpClient client;

    public WeatherService() {
        client = new OkHttpClient();
    }

    public interface WeatherCallback {
        void onSuccess(WeatherForecast forecast);
        void onFailure(String error);
    }

    public void getWeatherForecast(double lat, double lon, Date date, WeatherCallback callback) {
        // Format date for API call
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = dateFormat.format(date);

        String url = BASE_URL + "/forecast?lat=" + lat + "&lon=" + lon +
                "&appid=" + OPEN_WEATHER_API_KEY + "&units=metric";

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
                        JSONObject json = new JSONObject(jsonData);
                        WeatherForecast forecast = parseWeatherData(json, dateString);
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

    private WeatherForecast parseWeatherData(JSONObject json, String targetDate) throws Exception {
        // Parse weather data for the target date
        JSONObject forecast = findForecastForDate(json, targetDate);

        if (forecast != null) {
            String condition = forecast.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("main");

            double temperature = forecast.getJSONObject("main")
                    .getDouble("temp");

            int humidity = forecast.getJSONObject("main")
                    .getInt("humidity");

            double windSpeed = forecast.getJSONObject("wind")
                    .getDouble("speed");

            return new WeatherForecast(condition, temperature, humidity, windSpeed);
        }

        return new WeatherForecast("Clear", 25.0, 60, 5.0); // Default fallback
    }

    private JSONObject findForecastForDate(JSONObject json, String targetDate) throws Exception {
        // Find the forecast entry closest to the target date
        // OpenWeather API returns forecasts for every 3 hours for 5 days
        return json.getJSONArray("list").getJSONObject(0); // Simplified - use first forecast
    }
}

// Weather Forecast Model
class WeatherForecast {
    private String condition;
    private double temperature;
    private int humidity;
    private double windSpeed;

    public WeatherForecast(String condition, double temperature, int humidity, double windSpeed) {
        this.condition = condition;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
    }

    // Getters
    public String getCondition() { return condition; }
    public double getTemperature() { return temperature; }
    public int getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }

    // Helper methods
    public boolean isRainy() {
        return condition.toLowerCase().contains("rain");
    }

    public boolean isExtremeHeat() {
        return temperature > 35.0;
    }

    public boolean isGoodWeather() {
        return !isRainy() && temperature >= 15 && temperature <= 30;
    }
}