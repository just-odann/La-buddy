package com.example.la_buddy;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.ViewHolder> {

    private ArrayList<AdminOrderModel> orderList;
    private OnOrderClickListener clickListener;

    public interface OnOrderClickListener {
        void onOrderClick(AdminOrderModel order);
    }

    public AdminOrderAdapter(ArrayList<AdminOrderModel> orderList, OnOrderClickListener clickListener) {
        this.orderList = orderList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminOrderModel order = orderList.get(position);

        // --- 1. BASIC DATA BINDING ---
        holder.tvCustomerName.setText(order.getName() != null ? order.getName() : "Customer");
        holder.tvServiceType.setText("Service: " + (order.getItems() != null ? order.getItems() : "N/A"));
        holder.tvOrderStatus.setText(order.getStatus());
        String safePrice = order.getPrice() != null ? order.getPrice() : "0.00";
        String safeWeight = order.getWeight() != null ? order.getWeight() : "Pending";
        holder.tvPriceWeight.setText("Price: ₱" + safePrice + " | " + safeWeight);

        // --- 2. DYNAMIC STYLING ---
        if (order.getStatus() != null && (order.getStatus().equalsIgnoreCase("Completed") || order.getStatus().equalsIgnoreCase("Picked Up"))) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
            holder.tvCustomerName.setTextColor(Color.GRAY);
            holder.tvOrderStatus.setTextColor(Color.GRAY);
            holder.btnNavigate.setVisibility(View.GONE); // Hide nav for finished orders
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvCustomerName.setTextColor(Color.BLACK);
            holder.tvOrderStatus.setTextColor(Color.parseColor("#1A73E8"));

            // --- 3. NAVIGATION LOGIC ---
            // Only show navigation if it's a Pickup & Delivery method with valid coordinates
            if ("Pickup & Delivery".equalsIgnoreCase(order.getMethod()) && order.getLatitude() != 0) {
                holder.btnNavigate.setVisibility(View.VISIBLE);
                holder.btnNavigate.setOnClickListener(v -> {
                    openGoogleMaps(v, order.getLatitude(), order.getLongitude());
                });
            } else {
                holder.btnNavigate.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onOrderClick(order);
        });
    }

    private void openGoogleMaps(View v, double lat, double lng) {
        // 🚨 Creates a direct navigation URI for Google Maps
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(v.getContext().getPackageManager()) != null) {
            v.getContext().startActivity(mapIntent);
        } else {
            Toast.makeText(v.getContext(), "Google Maps is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() { return orderList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvServiceType, tvOrderStatus, tvPriceWeight;
        CardView cardView;
        ImageButton btnNavigate; // 🚨 Added navigation button

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvPriceWeight = itemView.findViewById(R.id.tvPriceWeight);
            cardView = itemView.findViewById(R.id.cardViewAdmin);
            btnNavigate = itemView.findViewById(R.id.btnNavigate); // Ensure this ID is in item_admin_order.xml
        }
    }
}