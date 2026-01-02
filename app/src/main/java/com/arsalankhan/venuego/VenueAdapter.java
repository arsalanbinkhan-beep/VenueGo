package com.arsalankhan.venuego;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;



import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.List;

public class VenueAdapter extends RecyclerView.Adapter<VenueAdapter.ViewHolder> {
    private List<Venue> venues;
    private Context context;
    private boolean detailedView;
    private DecimalFormat priceFormat = new DecimalFormat("₹#,##,###");

    public VenueAdapter(List<Venue> venues, Context context) {
        this(venues, context, false);
    }

    public VenueAdapter(List<Venue> venues, Context context, boolean detailedView) {
        this.venues = venues;
        this.context = context;
        this.detailedView = detailedView;
    }

    public void updateData(List<Venue> newVenues) {
        this.venues = newVenues;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = detailedView ? R.layout.item_venue_detailed : R.layout.item_venue_simple;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Venue venue = venues.get(position);

        holder.tvVenueName.setText(venue.getName());
        holder.tvVenueAddress.setText(venue.getAddress());
        holder.ratingBar.setRating((float) venue.getRating());
        holder.tvRating.setText(String.format("%.1f", venue.getRating()));
        holder.tvReviewCount.setText("(" + venue.getReviewCount() + " reviews)");

        // Load image
        if (venue.getImages() != null && !venue.getImages().isEmpty()) {
            Glide.with(context)
                    .load(venue.getImages().get(0))
                    .placeholder(R.drawable.placeholder_venue_image)
                    .into(holder.ivVenueImage);
        }

        if (detailedView) {
            // Show detailed information
            holder.tvCapacity.setText("Capacity: " + venue.getCapacity() + " guests");
            holder.tvPrice.setText(priceFormat.format(venue.getPriceRange()));
            holder.tvCategory.setText(getCategoryDisplayName(venue.getCategory()));
            holder.tvType.setText(venue.getType().equals("indoor") ? "Indoor" : "Outdoor");

            // Show amenities (first 3)
            if (venue.getAmenities() != null && !venue.getAmenities().isEmpty()) {
                StringBuilder amenities = new StringBuilder();
                int limit = Math.min(3, venue.getAmenities().size());
                for (int i = 0; i < limit; i++) {
                    amenities.append(getAmenityDisplayName(venue.getAmenities().get(i)));
                    if (i < limit - 1) amenities.append(" • ");
                }
                holder.tvAmenities.setText(amenities.toString());
            }

            // Distance calculation (if location available)
            if (context instanceof VenueListActivity) {
                // Calculate and show distance
                // This would require user's current location
            }
        }

        // Item click
        holder.itemView.setOnClickListener(v -> openVenueDetails(venue));

        // Quick actions
        if (detailedView) {
            holder.btnBookNow.setOnClickListener(v -> bookVenue(venue));
            holder.btnViewOnMap.setOnClickListener(v -> showOnMap(venue));
            holder.btnAddToFavorites.setOnClickListener(v -> addToFavorites(venue));
        }
    }

    private String getCategoryDisplayName(String category) {
        switch (category) {
            case "banquet_hall": return "Banquet Hall";
            case "community_center": return "Community Center";
            case "event_venue": return "Event Venue";
            case "open_ground": return "Open Ground";
            case "stadium": return "Stadium";
            case "auditorium": return "Auditorium";
            case "restaurant": return "Restaurant";
            case "hotel": return "Hotel";
            default: return "Venue";
        }
    }

    private String getAmenityDisplayName(String amenity) {
        switch (amenity) {
            case "ac": return "AC";
            case "wifi": return "WiFi";
            case "parking": return "Parking";
            case "catering": return "Catering";
            case "sound_system": return "Sound System";
            case "stage": return "Stage";
            case "lighting": return "Lighting";
            case "kitchen": return "Kitchen";
            case "bar": return "Bar";
            case "restrooms": return "Restrooms";
            default: return amenity;
        }
    }

    private void openVenueDetails(Venue venue) {
        Intent intent = new Intent(context, VenueDetailActivity.class);
        intent.putExtra("venueId", venue.getId());
        context.startActivity(intent);
    }

    private void bookVenue(Venue venue) {
        Intent intent = new Intent(context, BookingActivity.class);
        intent.putExtra("venue", (Serializable) venue);
        context.startActivity(intent);
    }

    private void showOnMap(Venue venue) {
        // Open Google Maps with venue location
        String uri = "http://maps.google.com/maps?q=" +
                venue.getLatitude() + "," + venue.getLongitude() +
                "(" + venue.getName() + ")";
        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
        context.startActivity(intent);
    }

    private void addToFavorites(Venue venue) {
        // Implement add to favorites
        Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return venues.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVenueImage;
        TextView tvVenueName;
        TextView tvVenueAddress;
        RatingBar ratingBar;
        TextView tvRating;
        TextView tvReviewCount;
        TextView tvCapacity;
        TextView tvPrice;
        TextView tvCategory;
        TextView tvType;
        TextView tvAmenities;
        ImageView btnBookNow;
        ImageView btnViewOnMap;
        ImageView btnAddToFavorites;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVenueImage = itemView.findViewById(R.id.iv_venue_image);
            tvVenueName = itemView.findViewById(R.id.tv_venue_name);
            tvVenueAddress = itemView.findViewById(R.id.tv_venue_address);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvReviewCount = itemView.findViewById(R.id.tv_review_count);

            // Detailed view elements
            tvCapacity = itemView.findViewById(R.id.tv_capacity);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvType = itemView.findViewById(R.id.tv_type);
            tvAmenities = itemView.findViewById(R.id.tv_amenities);
            btnBookNow = itemView.findViewById(R.id.btn_book_now);
            btnViewOnMap = itemView.findViewById(R.id.btn_view_on_map);
            btnAddToFavorites = itemView.findViewById(R.id.btn_add_to_favorites);
        }
    }
}