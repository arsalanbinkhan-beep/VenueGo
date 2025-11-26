package com.arsalankhan.venuego;

import java.util.*;

public class Booking {
    private String id;
    private String userId;
    private String venueId;
    private String venueName;
    private Date eventDate;
    private String eventType;
    private int guestCount;
    private double totalPrice;
    private String status; // PENDING, CONFIRMED, CANCELLED
    private Date bookingDate;
    private String specialRequirements;

    // Constructors
    public Booking() {}

    public Booking(String userId, String venueId, String venueName, Date eventDate, String eventType, int guestCount) {
        this.userId = userId;
        this.venueId = venueId;
        this.venueName = venueName;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.guestCount = guestCount;
        this.status = "PENDING";
        this.bookingDate = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public Date getEventDate() { return eventDate; }
    public void setEventDate(Date eventDate) { this.eventDate = eventDate; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getBookingDate() { return bookingDate; }
    public void setBookingDate(Date bookingDate) { this.bookingDate = bookingDate; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }
}