package com.arsalankhan.venuego;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

public class PaymentService implements PaymentResultListener {
    private Activity activity;
    private PaymentCallback callback;
    private static final String RAZORPAY_KEY = "YOUR_RAZORPAY_KEY";

    public PaymentService(Activity activity) {
        this.activity = activity;
        Checkout.preload(activity.getApplicationContext());
    }

    public interface PaymentCallback {
        void onPaymentSuccess(String paymentId);
        void onPaymentError(int code, String message);
        void onPaymentCancel();
    }

    public void startPayment(double amount, String bookingId, PaymentCallback callback) {
        this.callback = callback;

        try {
            Checkout checkout = new Checkout();
            checkout.setKeyID(RAZORPAY_KEY);

            JSONObject options = new JSONObject();
            options.put("name", "VenueGo");
            options.put("description", "Booking Payment");
            options.put("currency", "INR");
            options.put("amount", amount * 100); // Convert to paise

            JSONObject prefill = new JSONObject();
            prefill.put("email", "customer@email.com");
            prefill.put("contact", "9999999999");
            options.put("prefill", prefill);

            checkout.open(activity, options);
        } catch (Exception e) {
            Log.e("PaymentService", "Error in starting payment", e);
            callback.onPaymentError(0, e.getMessage());
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        Log.d("PaymentService", "Payment successful: " + razorpayPaymentId);
        if (callback != null) {
            callback.onPaymentSuccess(razorpayPaymentId);
        }
    }

    @Override
    public void onPaymentError(int code, String response) {
        Log.e("PaymentService", "Payment failed: " + code + " - " + response);
        if (callback != null) {
            callback.onPaymentError(code, response);
        }
    }

    public void handlePaymentResult(int requestCode, int resultCode, Intent data) {
        // Handle payment result if needed
    }
}