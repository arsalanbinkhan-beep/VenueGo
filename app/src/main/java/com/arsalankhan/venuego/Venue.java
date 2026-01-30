package com.arsalankhan.venuego;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Venue implements Serializable {
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
    private String website;
    private Map<String, Boolean> availability;

    private String city;
    private String state;
    private String country;
    private String postalCode;

    // Social media
    private String facebookUrl;
    private String instagramUrl;
    private String twitterUrl;

    // Business hours
    private Map<String, String> businessHours; // Day -> Hours (e.g., "Monday": "9:00 AM - 10:00 PM")

    // Parking information
    private boolean hasParking;
    private int parkingCapacity;
    private boolean parkingPaid;

    // Additional features
    private boolean wifiAvailable;
    private boolean cateringAvailable;
    private boolean alcoholAllowed;
    private boolean smokingAllowed;
    private boolean wheelchairAccessible;

    // OSM specific fields
    private String osmId;
    private Map<String, String> osmTags;
    private Date lastUpdated;
    private String dataSource = "osm";

    // Firestore sync fields
    private Date firestoreUpdatedAt;
    private boolean syncPending;
    private String syncStatus;

    // Statistics
    private int viewCount;
    private int bookingCount;
    private Date createdAt;
    private Date updatedAt;

    // Constructors
    public Venue() {
        this.amenities = new ArrayList<>();
        this.images = new ArrayList<>();
        this.availability = new HashMap<>();
        this.osmTags = new HashMap<>();
        this.businessHours = new HashMap<>();
        this.lastUpdated = new Date();
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.country = "India"; // Default
        this.state = "Maharashtra"; // Default
    }

    public Venue(String name, String address, double lat, double lng, String city) {
        this();
        this.name = name;
        this.address = address;
        this.latitude = lat;
        this.longitude = lng;
        this.city = city;
    }

    // Getters and setters for all fields
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

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public Map<String, Boolean> getAvailability() { return availability; }
    public void setAvailability(Map<String, Boolean> availability) { this.availability = availability; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String facebookUrl) { this.facebookUrl = facebookUrl; }

    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }

    public String getTwitterUrl() { return twitterUrl; }
    public void setTwitterUrl(String twitterUrl) { this.twitterUrl = twitterUrl; }

    public Map<String, String> getBusinessHours() { return businessHours; }
    public void setBusinessHours(Map<String, String> businessHours) { this.businessHours = businessHours; }

    public boolean isHasParking() { return hasParking; }
    public void setHasParking(boolean hasParking) { this.hasParking = hasParking; }

    public int getParkingCapacity() { return parkingCapacity; }
    public void setParkingCapacity(int parkingCapacity) { this.parkingCapacity = parkingCapacity; }

    public boolean isParkingPaid() { return parkingPaid; }
    public void setParkingPaid(boolean parkingPaid) { this.parkingPaid = parkingPaid; }

    public boolean isWifiAvailable() { return wifiAvailable; }
    public void setWifiAvailable(boolean wifiAvailable) { this.wifiAvailable = wifiAvailable; }

    public boolean isCateringAvailable() { return cateringAvailable; }
    public void setCateringAvailable(boolean cateringAvailable) { this.cateringAvailable = cateringAvailable; }

    public boolean isAlcoholAllowed() { return alcoholAllowed; }
    public void setAlcoholAllowed(boolean alcoholAllowed) { this.alcoholAllowed = alcoholAllowed; }

    public boolean isSmokingAllowed() { return smokingAllowed; }
    public void setSmokingAllowed(boolean smokingAllowed) { this.smokingAllowed = smokingAllowed; }

    public boolean isWheelchairAccessible() { return wheelchairAccessible; }
    public void setWheelchairAccessible(boolean wheelchairAccessible) { this.wheelchairAccessible = wheelchairAccessible; }

    public String getOsmId() { return osmId; }
    public void setOsmId(String osmId) { this.osmId = osmId; }

    public Map<String, String> getOsmTags() { return osmTags; }
    public void setOsmTags(Map<String, String> osmTags) { this.osmTags = osmTags; }

    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public Date getFirestoreUpdatedAt() { return firestoreUpdatedAt; }
    public void setFirestoreUpdatedAt(Date firestoreUpdatedAt) { this.firestoreUpdatedAt = firestoreUpdatedAt; }

    public boolean isSyncPending() { return syncPending; }
    public void setSyncPending(boolean syncPending) { this.syncPending = syncPending; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getBookingCount() { return bookingCount; }
    public void setBookingCount(int bookingCount) { this.bookingCount = bookingCount; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public void addAmenity(String amenity) {
        if (amenities == null) {
            amenities = new ArrayList<>();
        }
        if (!amenities.contains(amenity)) {
            amenities.add(amenity);
        }
    }

    public void removeAmenity(String amenity) {
        if (amenities != null) {
            amenities.remove(amenity);
        }
    }

    public void addImage(String imageUrl) {
        if (images == null) {
            images = new ArrayList<>();
        }
        if (!images.contains(imageUrl)) {
            images.add(imageUrl);
        }
    }

    public void addBusinessHour(String day, String hours) {
        if (businessHours == null) {
            businessHours = new HashMap<>();
        }
        businessHours.put(day, hours);
    }

    public void addOsmTag(String key, String value) {
        if (osmTags == null) {
            osmTags = new HashMap<>();
        }
        osmTags.put(key, value);
    }

    public void incrementViewCount() {
        this.viewCount++;
        this.updatedAt = new Date();
    }

    public void incrementBookingCount() {
        this.bookingCount++;
        this.updatedAt = new Date();
    }

    public void updateRating(double newRating) {
        // Calculate new average rating
        double totalRating = this.rating * this.reviewCount;
        this.reviewCount++;
        this.rating = (totalRating + newRating) / this.reviewCount;
        this.updatedAt = new Date();
    }

    // Price range helper
    public String getPriceRangeFormatted() {
        if (priceRange < 1000) {
            return String.format("₹%.0f", priceRange);
        } else if (priceRange < 100000) {
            return String.format("₹%.1fK", priceRange / 1000);
        } else {
            return String.format("₹%.1fL", priceRange / 100000);
        }
    }

    // Distance calculation helper (Haversine formula)
    public double calculateDistance(double userLat, double userLng) {
        double earthRadius = 6371; // kilometers

        double dLat = Math.toRadians(this.latitude - userLat);
        double dLng = Math.toRadians(this.longitude - userLng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(this.latitude)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    // Get full address
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null) sb.append(address);
        if (city != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (state != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state);
        }
        if (postalCode != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(postalCode);
        }
        return sb.toString();
    }

    // Check if venue is open on a specific day
    public boolean isOpenOnDay(String day) {
        if (businessHours == null) return false;
        String hours = businessHours.get(day);
        return hours != null && !hours.trim().isEmpty() && !hours.equalsIgnoreCase("Closed");
    }

    // Get formatted business hours
    public String getFormattedBusinessHours() {
        if (businessHours == null || businessHours.isEmpty()) {
            return "Hours not available";
        }

        StringBuilder sb = new StringBuilder();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (String day : days) {
            String hours = businessHours.get(day);
            if (hours != null && !hours.trim().isEmpty()) {
                sb.append(day).append(": ").append(hours).append("\n");
            }
        }

        return sb.toString().trim();
    }

    // Get first image URL or placeholder
    public String getFirstImageUrl() {
        if (images != null && !images.isEmpty()) {
            return images.get(0);
        }
        return null; // Or return a placeholder URL
    }

    // Clone method for deep copy
    public Venue clone() {
        Venue cloned = new Venue();
        cloned.setId(this.id);
        cloned.setName(this.name);
        cloned.setAddress(this.address);
        cloned.setLatitude(this.latitude);
        cloned.setLongitude(this.longitude);
        cloned.setCategory(this.category);
        cloned.setType(this.type);
        cloned.setCapacity(this.capacity);
        cloned.setPriceRange(this.priceRange);

        if (this.amenities != null) {
            cloned.setAmenities(new ArrayList<>(this.amenities));
        }

        cloned.setRating(this.rating);
        cloned.setReviewCount(this.reviewCount);

        if (this.images != null) {
            cloned.setImages(new ArrayList<>(this.images));
        }

        cloned.setDescription(this.description);
        cloned.setContactPhone(this.contactPhone);
        cloned.setContactEmail(this.contactEmail);
        cloned.setWebsite(this.website);

        if (this.availability != null) {
            cloned.setAvailability(new HashMap<>(this.availability));
        }

        cloned.setCity(this.city);
        cloned.setState(this.state);
        cloned.setCountry(this.country);
        cloned.setPostalCode(this.postalCode);

        cloned.setFacebookUrl(this.facebookUrl);
        cloned.setInstagramUrl(this.instagramUrl);
        cloned.setTwitterUrl(this.twitterUrl);

        if (this.businessHours != null) {
            cloned.setBusinessHours(new HashMap<>(this.businessHours));
        }

        cloned.setHasParking(this.hasParking);
        cloned.setParkingCapacity(this.parkingCapacity);
        cloned.setParkingPaid(this.parkingPaid);
        cloned.setWifiAvailable(this.wifiAvailable);
        cloned.setCateringAvailable(this.cateringAvailable);
        cloned.setAlcoholAllowed(this.alcoholAllowed);
        cloned.setSmokingAllowed(this.smokingAllowed);
        cloned.setWheelchairAccessible(this.wheelchairAccessible);

        cloned.setOsmId(this.osmId);

        if (this.osmTags != null) {
            cloned.setOsmTags(new HashMap<>(this.osmTags));
        }

        cloned.setLastUpdated(this.lastUpdated != null ? new Date(this.lastUpdated.getTime()) : null);
        cloned.setDataSource(this.dataSource);
        cloned.setFirestoreUpdatedAt(this.firestoreUpdatedAt != null ? new Date(this.firestoreUpdatedAt.getTime()) : null);
        cloned.setSyncPending(this.syncPending);
        cloned.setSyncStatus(this.syncStatus);
        cloned.setViewCount(this.viewCount);
        cloned.setBookingCount(this.bookingCount);
        cloned.setCreatedAt(this.createdAt != null ? new Date(this.createdAt.getTime()) : null);
        cloned.setUpdatedAt(this.updatedAt != null ? new Date(this.updatedAt.getTime()) : null);

        return cloned;
    }

    @Override
    public String toString() {
        return "Venue{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", city='" + city + '\'' +
                ", category='" + category + '\'' +
                ", capacity=" + capacity +
                ", priceRange=" + priceRange +
                ", rating=" + rating +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Venue venue = (Venue) o;
        return id != null && id.equals(venue.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}