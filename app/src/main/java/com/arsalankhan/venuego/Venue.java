package com.arsalankhan.venuego;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Venue {
    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String category;
    private String type; // indoor/outdoor
    private int capacity;
    private double priceRange;
    private List<String> amenities;
    private double rating;
    private int reviewCount;
    private List<String> images;
    private String description;
    private String contactPhone;
    private String contactEmail;
    private Map<String, Boolean> availability;

    private String city;

    // OSM specific fields
    private String osmId;
    private Map<String, String> osmTags;
    private Date lastUpdated;
    private String dataSource = "osm";

    // Constructors
    public Venue() {
        this.amenities = new ArrayList<>();
        this.availability = new HashMap<>();
        this.osmTags = new HashMap<>();
        this.lastUpdated = new Date();
    }

    public Venue(String name, String address, double lat, double lng, String city) {
        this();
        this.name = name;
        this.address = address;
        this.latitude = lat;
        this.longitude = lng;
        this.city = city;
    }

    // Getters and setters for new fields
    public String getOsmId() { return osmId; }
    public void setOsmId(String osmId) { this.osmId = osmId; }

    public Map<String, String> getOsmTags() { return osmTags; }
    public void setOsmTags(Map<String, String> osmTags) { this.osmTags = osmTags; }

    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public double getPriceRange() { return priceRange; }
    public void setPriceRange(double priceRange) { this.priceRange = priceRange; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public Map<String, Boolean> getAvailability() { return availability; }
    public void setAvailability(Map<String, Boolean> availability) { this.availability = availability; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
