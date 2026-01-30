package com.arsalankhan.venuego;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Booking {

    // IDs
    private int bookingId;
    private String id;
    private String userId;
    private String venueId;

    // Venue Details
    private String venueName;
    private String venueAddress;

    // Event Details
    private String eventName;
    private Date eventDate;
    private String eventTime;
    private String eventType;

    // Booking Info
    private int guestCount;
    private double totalAmount;
    private double totalPrice;
    private String bookingStatus;   // PENDING, CONFIRMED, CANCELLED
    private String paymentStatus;
    private Date bookingDate;
    private String createdAt;

    // Extra
    private String specialRequirements;

    // ---------------- CONSTRUCTORS ----------------

    public Booking() {
        this.bookingDate = new Date();
        this.bookingStatus = "PENDING";
        this.paymentStatus = "UNPAID";
    }

    public Booking(String userId, String venueId, String venueName,
                   Date eventDate, String eventType, int guestCount) {

        this.userId = userId;
        this.venueId = venueId;
        this.venueName = venueName;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.guestCount = guestCount;
        this.bookingStatus = "PENDING";
        this.paymentStatus = "UNPAID";
        this.bookingDate = new Date();
    }

    // ---------------- GETTERS & SETTERS ----------------

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getVenueAddress() { return venueAddress; }
    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public Date getEventDate() { return eventDate; }
    public void setEventDate(Date eventDate) { this.eventDate = eventDate; }

    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
        this.totalPrice = totalAmount; // Keep both synced
    }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
        this.totalAmount = totalPrice; // Keep both synced
    }

    // ALIAS METHODS FOR getStatus() and setStatus() - Added these
    public String getStatus() { return bookingStatus; }
    public void setStatus(String status) { this.bookingStatus = status; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Date getBookingDate() { return bookingDate; }
    public void setBookingDate(Date bookingDate) { this.bookingDate = bookingDate; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }

    // ---------------- HELPER METHODS ----------------

    public boolean isConfirmed() {
        return "CONFIRMED".equalsIgnoreCase(bookingStatus);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(bookingStatus);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(bookingStatus);
    }

    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(paymentStatus);
    }

    public String getFormattedEventDate() {
        if (eventDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault());
            return sdf.format(eventDate);
        }
        return "Date not set";
    }

    public String getFormattedBookingDate() {
        if (bookingDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault());
            return sdf.format(bookingDate);
        }
        return "Date not set";
    }

    public String getFormattedPrice() {
        return String.format("₹%.2f", totalPrice);
    }

    // Get status with color (for display purposes)
    public int getStatusColor() {
        switch (bookingStatus.toUpperCase()) {
            case "CONFIRMED":
                return R.color.green_success; // Make sure this color exists in your colors.xml
            case "PENDING":
                return R.color.blue_info; // Make sure this color exists in your colors.xml
            case "CANCELLED":
                return R.color.red_error; // Make sure this color exists in your colors.xml
            default:
                return android.R.color.white;
        }
    }

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId=" + bookingId +
                ", venueName='" + venueName + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventDate=" + getFormattedEventDate() +
                ", status='" + bookingStatus + '\'' +
                ", totalPrice=" + getFormattedPrice() +
                '}';
    }
}