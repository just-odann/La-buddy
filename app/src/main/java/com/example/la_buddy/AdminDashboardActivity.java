package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminOrderAdapter adapter;
    private ArrayList<AdminOrderModel> orderList;
    private DatabaseReference ordersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        recyclerView = findViewById(R.id.adminRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();
        adapter = new AdminOrderAdapter(orderList, this::showUpdateDialog);
        recyclerView.setAdapter(adapter);

        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        loadLiveOrders();

        setupNavigation();
    }

    private void loadLiveOrders() {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear(); // Clears the list before fetching new data

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String uid = dataSnapshot.getKey();
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String items = dataSnapshot.child("items").getValue(String.class);
                    String price = dataSnapshot.child("price").getValue(String.class);
                    String weight = dataSnapshot.child("weight").getValue(String.class);
                    String status = dataSnapshot.child("status").getValue(String.class);
                    String historyId = dataSnapshot.child("historyId").getValue(String.class);
                    String category = dataSnapshot.child("category").getValue(String.class);
                    String addons = dataSnapshot.child("addons").getValue(String.class);
                    String method = dataSnapshot.child("method").getValue(String.class);

                    Integer unread = dataSnapshot.child("unreadAdmin").getValue(Integer.class);
                    int unreadCount = (unread != null) ? unread : 0;

                    Long ts = dataSnapshot.child("timestamp").getValue(Long.class);
                    long timestamp = (ts != null) ? ts : 0;

                    if (items == null || items.trim().isEmpty() || items.equalsIgnoreCase("No Active Service")) {
                        continue;
                    }

                    // 🚨 SAFE PARSING FOR GPS COORDINATES 🚨
                    // This prevents silent crashes if Firebase saves a whole number like '14' instead of '14.0'
                    double latitude = 0.0;
                    double longitude = 0.0;
                    if (dataSnapshot.child("latitude").getValue() != null) {
                        latitude = Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue()));
                    }
                    if (dataSnapshot.child("longitude").getValue() != null) {
                        longitude = Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue()));
                    }

                    // Create the model
                    AdminOrderModel order = new AdminOrderModel(
                            uid, name, items, price, weight, status, historyId,
                            unreadCount, timestamp, category, addons, method,
                            latitude, longitude
                    );

                    // 🚨 THE FIX: You must add the order to the list! 🚨
                    orderList.add(order);

                    // Fetch name if missing
                    if (name == null || name.equalsIgnoreCase("N/A") || name.trim().isEmpty()) {
                        fetchUserName(uid, order);
                    }
                }
                // Update the UI
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void fetchUserName(String uid, AdminOrderModel order) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                        String fetchedName = userSnapshot.child("name").getValue(String.class);
                        if (fetchedName != null) {
                            order.setName(fetchedName);
                            adapter.notifyDataSetChanged();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showUpdateDialog(AdminOrderModel order) {
        if (order.getStatus() != null && (order.getStatus().equalsIgnoreCase("Completed") || order.getStatus().equalsIgnoreCase("Picked Up"))) {
            Toast.makeText(this, "Order is already completed.", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_status, null);
        bottomSheetDialog.setContentView(dialogView);

        TextView tvName = dialogView.findViewById(R.id.tvDialogCustomerName);
        EditText etWeight = dialogView.findViewById(R.id.etUpdateWeight);
        Button btnCalculate = dialogView.findViewById(R.id.btnCalculatePrice);
        TextView tvTotal = dialogView.findViewById(R.id.tvCalculatedTotal);
        Button btnSendReply = dialogView.findViewById(R.id.btnSendReply);

        tvName.setText("Update Order: " + order.getName());

        boolean isCalculated = order.getPrice() != null &&
                !order.getPrice().equalsIgnoreCase("Pending") &&
                !order.getPrice().equals("0") &&
                !order.getPrice().equals("0.00");

        if (isCalculated) {
            tvTotal.setText("Total: ₱" + order.getPrice());
            etWeight.setText(order.getWeight() != null ? order.getWeight().replace(" kg", "") : "");

            etWeight.setEnabled(false);
            btnCalculate.setEnabled(false);
            btnCalculate.setText("Calculated & Locked");
            btnCalculate.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }

        btnCalculate.setOnClickListener(v -> {
            String weightStr = etWeight.getText().toString().trim();
            if (!weightStr.isEmpty()) {
                try {
                    double weightVal = Double.parseDouble(weightStr);

                    double baseRate = extractPrice(order.getItems());
                    double subtotal = weightVal * baseRate;

                    double addonsTotal = extractTotalAddons(order.getAddons());

                    double deliveryFee = 0.0;
                    String rawMethod = order.getMethod() != null ? order.getMethod().toLowerCase() : "";
                    if (rawMethod.contains("delivery") || rawMethod.contains("pickup")) {
                        deliveryFee = 50.00;
                    }

                    double finalTotal = subtotal + addonsTotal + deliveryFee;

                    String formattedPrice = String.format(java.util.Locale.US, "%.2f", finalTotal);
                    tvTotal.setText("Total: ₱" + formattedPrice);

                    order.setPrice(formattedPrice);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("weight", weightStr + " kg");
                    updates.put("price", formattedPrice);
                    pushToFirebase(order, updates, "Saved: ₱" + formattedPrice);

                    etWeight.setEnabled(false);
                    btnCalculate.setEnabled(false);
                    btnCalculate.setText("Calculated & Locked");
                    btnCalculate.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

                } catch (Exception e) {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSendReply.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, AdminMessagesActivity.class);
            intent.putExtra("CUSTOMER_UID", order.getUid());

            // Kill animation when jumping to messages from the dialog!
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        setupStatusButtons(dialogView, order, bottomSheetDialog);
        bottomSheetDialog.show();
    }

    private double extractPrice(String text) {
        if (text == null || text.trim().isEmpty()) return 35.00;
        double price = 35.00;
        Pattern p = Pattern.compile("(\\d+\\.\\d+|\\d+)");
        Matcher m = p.matcher(text.replace(",", ""));
        while (m.find()) {
            price = Double.parseDouble(m.group(1));
        }
        return price;
    }

    private double extractTotalAddons(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        double total = 0.0;

        String[] parts = text.split("\\+");
        for (int i = 1; i < parts.length; i++) {
            Matcher m = Pattern.compile("(\\d+\\.\\d+|\\d+)").matcher(parts[i].replace(",", ""));
            if (m.find()) {
                total += Double.parseDouble(m.group(1));
            }
        }
        return total;
    }

    private void setupStatusButtons(View view, AdminOrderModel order, BottomSheetDialog dialog) {
        int[] ids = {R.id.btnSetDroppedOff, R.id.btnSetWashing, R.id.btnSetDrying, R.id.btnSetFolding, R.id.btnSetReady, R.id.btnSetCompleted};
        for (int id : ids) {
            Button btn = view.findViewById(id);
            btn.setOnClickListener(v -> {
                String newStatus = btn.getText().toString().replace("Set: ", "").trim();
                if(newStatus.contains("Completed")) newStatus = "Completed";

                Map<String, Object> updates = new HashMap<>();
                updates.put("status", newStatus);
                pushToFirebase(order, updates, "Status: " + newStatus);
                dialog.dismiss();
            });
        }
    }

    private void pushToFirebase(AdminOrderModel order, Map<String, Object> updates, String successMessage) {
        String currentStatus = (String) updates.get("status");
        long timestamp = System.currentTimeMillis();
        updates.put("timestamp", timestamp);

        if ("Completed".equalsIgnoreCase(currentStatus) || "Picked Up".equalsIgnoreCase(currentStatus)) {
            Map<String, Object> fullFinanceData = new HashMap<>();
            fullFinanceData.put("name", order.getName());
            fullFinanceData.put("items", order.getItems());
            fullFinanceData.put("price", order.getPrice());
            fullFinanceData.put("weight", order.getWeight());
            fullFinanceData.put("status", currentStatus);
            fullFinanceData.put("timestamp", timestamp);
            fullFinanceData.put("uid", order.getUid());
            fullFinanceData.put("category", order.getCategory());
            fullFinanceData.put("addons", order.getAddons());
            fullFinanceData.put("method", order.getMethod());

            if (order.getHistoryId() != null) {
                FirebaseDatabase.getInstance().getReference("Finances")
                        .child(order.getUid())
                        .child(order.getHistoryId())
                        .setValue(fullFinanceData);
            }
            ordersRef.child(order.getUid()).removeValue();
        } else {
            ordersRef.child(order.getUid()).updateChildren(updates);
        }

        if (order.getHistoryId() != null && !order.getHistoryId().isEmpty()) {
            FirebaseDatabase.getInstance().getReference("OrderHistory")
                    .child(order.getUid())
                    .child(order.getHistoryId())
                    .updateChildren(updates);
        }
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
    }

    // 🚨 Kills the slide animation if you press the physical phone back button 🚨
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    // 🚨 THE NUCLEAR NAVIGATION FIX 🚨
    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // If they click the button of the screen they are already on, do nothing.
            if (id == bottomNav.getSelectedItemId()) {
                return true;
            }

            Intent intent = null;
            if (id == R.id.nav_dashboard) {
                intent = new Intent(this, AdminDashboardActivity.class);
            } else if (id == R.id.nav_messages) {
                intent = new Intent(this, AdminMessagesActivity.class);
            } else if (id == R.id.nav_history) {
                intent = new Intent(this, AdminHistoryActivity.class);
            } else if (id == R.id.nav_finance) {
                intent = new Intent(this, AdminFinanceActivity.class);
            }

            if (intent != null) {
                // Strip all animations and clear old screens from memory
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                // Override the entry animation
                overridePendingTransition(0, 0);
                finish();

                // Override the exit animation
                overridePendingTransition(0, 0);
            }
            return true;
        });
    }
}