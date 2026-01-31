package com.arsalankhan.venuego;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.facebook.shimmer.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AppUtils {

    // Date and Time Utilities
    public static String formatDate(Date date, String pattern) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(date);
    }

    public static String getRelativeTime(Date date) {
        if (date == null) return "";

        long now = System.currentTimeMillis();
        long diff = now - date.getTime();

        if (diff < 60000) return "Just now";
        if (diff < 3600000) return TimeUnit.MILLISECONDS.toMinutes(diff) + " min ago";
        if (diff < 86400000) return TimeUnit.MILLISECONDS.toHours(diff) + " hours ago";
        if (diff < 604800000) return TimeUnit.MILLISECONDS.toDays(diff) + " days ago";

        return formatDate(date, "dd MMM yyyy");
    }

    public static boolean isDateInFuture(Date date) {
        return date != null && date.after(new Date());
    }

    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTime();
    }

    // Number Formatting
    public static String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }

    public static String formatCompactCurrency(double amount) {
        if (amount < 1000) return "₹" + (int) amount;
        if (amount < 100000) return "₹" + String.format("%.1fK", amount / 1000);
        if (amount < 10000000) return "₹" + String.format("%.1fL", amount / 100000);
        return "₹" + String.format("%.1fCr", amount / 10000000);
    }

    public static String formatDistance(double distanceInKm) {
        if (distanceInKm < 1) return String.format("%.0f m", distanceInKm * 1000);
        return String.format("%.1f km", distanceInKm);
    }

    // Permission Utilities
    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    public static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    // UI Utilities
    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // Validation Utilities
    public static boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return android.util.Patterns.PHONE.matcher(phone).matches()
                && phone.length() >= 10;
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    // File and Image Utilities
    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public static Bitmap base64ToBitmap(String base64) {
        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    public static File saveBitmapToFile(Context context, Bitmap bitmap, String filename) {
        File file = new File(context.getCacheDir(), filename);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            return file;
        } catch (IOException e) {
            Log.e("AppUtils", "Error saving bitmap to file", e);
            return null;
        }
    }

    // Network Utilities
    public static boolean isNetworkAvailable(Context context) {
        android.net.ConnectivityManager connectivityManager =
                (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    // Random Utilities
    public static String generateBookingId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return "BOOK" + sb.toString();
    }

    public static String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + new Random().nextInt(1000);
    }

    // Color Utilities
    public static int getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "confirmed":
            case "success":
            case "completed":
                return R.color.green_success;
            case "pending":
            case "processing":
                return R.color.blue_info;
            case "cancelled":
            case "failed":
            case "error":
                return R.color.red_error;
            default:
                return R.color.dark_purple_background;
        }
    }

    // Rating Utilities
    public static float calculateAverageRating(List<Float> ratings) {
        if (ratings == null || ratings.isEmpty()) return 0.0f;

        float sum = 0;
        for (float rating : ratings) {
            sum += rating;
        }
        return sum / ratings.size();
    }

    // String Utilities
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    // Logging Utilities
    public static void logInfo(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message);
        }
    }

    public static void logError(String tag, String message, Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable);
        }
    }

    // Device Information
    public static String getDeviceModel() {
        return Build.MODEL;
    }

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getAppVersion(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }
}