package com.arsalankhan.venuego;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.ViewHolder> {
    private List<Booking> bookings;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public BookingsAdapter(List<Booking> bookings, Context context) {
        this.bookings = bookings;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);

        holder.tvVenueName.setText(booking.getVenueName());
        holder.tvEventType.setText(booking.getEventType());
        holder.tvGuestCount.setText(booking.getGuestCount() + " guests");
        holder.tvPrice.setText("₹" + String.format("%.0f", booking.getTotalPrice()));
        holder.tvStatus.setText(booking.getStatus());

        // Format date
        if (booking.getEventDate() != null) {
            holder.tvEventDate.setText(dateFormat.format(booking.getEventDate()));
        }

        // Set status color
        setStatusColor(holder.tvStatus, booking.getStatus());

        // Item click to view booking details
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, "Opening booking details", Toast.LENGTH_SHORT).show();
        });

        // Cancel booking button
        holder.btnCancel.setOnClickListener(v -> {
            cancelBooking(booking, position);
        });
    }

    private void setStatusColor(TextView textView, String status) {
        switch (status.toUpperCase()) {
            case "CONFIRMED":
                textView.setTextColor(context.getResources().getColor(R.color.green_success));
                break;
            case "PENDING":
                textView.setTextColor(context.getResources().getColor(R.color.blue_info));
                break;
            case "CANCELLED":
                textView.setTextColor(context.getResources().getColor(R.color.red_error));
                break;
            default:
                textView.setTextColor(context.getResources().getColor(android.R.color.white));
        }
    }

    private void cancelBooking(Booking booking, int position) {
        // Implement cancel booking logic
        Toast.makeText(context, "Cancelling booking for " + booking.getVenueName(), Toast.LENGTH_SHORT).show();
        // Update status and notify
        booking.setStatus("CANCELLED");
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVenueName;
        TextView tvEventType;
        TextView tvEventDate;
        TextView tvGuestCount;
        TextView tvPrice;
        TextView tvStatus;
        Button btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVenueName = itemView.findViewById(R.id.tv_venue_name);
            tvEventType = itemView.findViewById(R.id.tv_event_type);
            tvEventDate = itemView.findViewById(R.id.tv_event_date);
            tvGuestCount = itemView.findViewById(R.id.tv_guest_count);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
        }
    }
}