package com.arsalankhan.venuego;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Event implements Serializable {
    private String id;
    private String userId;
    private String eventName;
    private String eventDescription;
    private String eventType;
    private Date eventDate;
    private String venueType; // indoor/outdoor
    private int guestCount;
    private String budgetRange;
    private String weatherCondition;
    private double temperature;
    private String status; // DRAFT, ACTIVE, COMPLETED, CANCELLED
    private Date createdAt;
    private Date updatedAt;
    private String venueId; // Selected venue
    private List<String> preferredAmenities;
    private String locationPreference; // city or area

    // Constructors
    public Event() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.status = "DRAFT";
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Date getEventDate() { return eventDate; }
    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
        this.updatedAt = new Date();
    }

    public String getVenueType() { return venueType; }
    public void setVenueType(String venueType) { this.venueType = venueType; }

    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }

    public String getBudgetRange() { return budgetRange; }
    public void setBudgetRange(String budgetRange) { this.budgetRange = budgetRange; }

    public String getWeatherCondition() { return weatherCondition; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }

    public List<String> getPreferredAmenities() { return preferredAmenities; }
    public void setPreferredAmenities(List<String> preferredAmenities) { this.preferredAmenities = preferredAmenities; }

    public String getLocationPreference() { return locationPreference; }
    public void setLocationPreference(String locationPreference) { this.locationPreference = locationPreference; }
}