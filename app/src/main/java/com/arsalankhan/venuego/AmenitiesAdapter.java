package com.arsalankhan.venuego;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arsalankhan.venuego.R;

import java.util.List;

public class AmenitiesAdapter extends RecyclerView.Adapter<AmenitiesAdapter.ViewHolder> {
    private List<String> amenities;
    private Context context;

    public AmenitiesAdapter(List<String> amenities) {
        this.amenities = amenities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_amenity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String amenity = amenities.get(position);
        holder.amenityName.setText(getAmenityDisplayName(amenity));
    }

    @Override
    public int getItemCount() {
        return amenities.size();
    }

    public void updateData(List<String> newAmenities) {
        amenities.clear();
        amenities.addAll(newAmenities);
        notifyDataSetChanged();
    }

    private String getAmenityDisplayName(String amenity) {
        switch (amenity) {
            case "parking": return "Parking";
            case "ac": return "Air Conditioned";
            case "catering": return "Catering";
            case "wifi": return "WiFi";
            case "stage": return "Stage";
            case "lighting": return "Lighting";
            case "sound_system": return "Sound System";
            case "kitchen": return "Kitchen";
            case "bar": return "Bar";
            case "restrooms": return "Restrooms";
            case "wheelchair_accessible": return "Wheelchair Access";
            default: return amenity;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amenityName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            amenityName = itemView.findViewById(R.id.tv_amenity_name);
        }
    }
}