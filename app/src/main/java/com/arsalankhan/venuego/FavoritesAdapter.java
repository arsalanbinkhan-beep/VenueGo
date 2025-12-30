package com.arsalankhan.venuego;

import android.content.Context;
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

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {
    private List<Venue> venues;
    private Context context;

    public FavoritesAdapter(List<Venue> venues, Context context) {
        this.venues = venues;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_venue, parent, false);
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

        // Load image with Glide if available
        if (venue.getImages() != null && !venue.getImages().isEmpty()) {
            Glide.with(context)
                    .load(venue.getImages().get(0))
                    .placeholder(R.drawable.placeholder_venue_image)
                    .into(holder.ivVenueImage);
        }

        // Set capacity
        holder.tvCapacity.setText("Capacity: " + venue.getCapacity() + " guests");

        // Set price range
        holder.tvPrice.setText("₹" + String.format("%.0f", venue.getPriceRange()));

        // Remove from favorites click
        holder.btnRemove.setOnClickListener(v -> {
            removeFromFavorites(venue, position);
        });

        // Item click to view details
        holder.itemView.setOnClickListener(v -> {
            // Navigate to venue details
            Toast.makeText(context, "Opening " + venue.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    private void removeFromFavorites(Venue venue, int position) {
        // Implement remove from favorites logic
        Toast.makeText(context, "Removed " + venue.getName() + " from favorites", Toast.LENGTH_SHORT).show();
        venues.remove(position);
        notifyItemRemoved(position);
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
        ImageView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVenueImage = itemView.findViewById(R.id.iv_venue_image);
            tvVenueName = itemView.findViewById(R.id.tv_venue_name);
            tvVenueAddress = itemView.findViewById(R.id.tv_venue_address);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvReviewCount = itemView.findViewById(R.id.tv_review_count);
            tvCapacity = itemView.findViewById(R.id.tv_capacity);
            tvPrice = itemView.findViewById(R.id.tv_price);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}