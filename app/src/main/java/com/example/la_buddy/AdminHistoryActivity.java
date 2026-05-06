package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private AdminOrderAdapter adapter;
    private ArrayList<AdminOrderModel> allHistoryList, filteredList;
    private String currentFilter = "All Time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_history);

        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        allHistoryList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new AdminOrderAdapter(filteredList, order -> {
            // This is where you could show a detailed breakdown dialog
        });
        rvHistory.setAdapter(adapter);

        ChipGroup chipGroup = findViewById(R.id.chipGroupFilters);
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.contains(R.id.chipToday)) currentFilter = "Today";
                else if (checkedIds.contains(R.id.chipThisWeek)) currentFilter = "This Week";
                else if (checkedIds.contains(R.id.chipThisMonth)) currentFilter = "This Month";
                else currentFilter = "All Time";
                applyFilters();
            });
        }

        loadHistoryFromFirebase();

        // 🚨 THE FIX: Calling the navigation setup! 🚨
        setupNavigation();
    }

    private void loadHistoryFromFirebase() {
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("OrderHistory");
        historyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allHistoryList.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    for (DataSnapshot orderSnap : userSnap.getChildren()) {
                        String uid = userSnap.getKey();
                        String name = orderSnap.child("name").getValue(String.class);
                        String items = orderSnap.child("items").getValue(String.class);
                        String price = orderSnap.child("price").getValue(String.class);
                        String weight = orderSnap.child("weight").getValue(String.class);
                        String status = orderSnap.child("status").getValue(String.class);
                        String historyId = orderSnap.getKey();

                        Long ts = orderSnap.child("timestamp").getValue(Long.class);
                        long timestamp = (ts != null) ? ts : 0;

                        String category = orderSnap.child("category").getValue(String.class);
                        String addons = orderSnap.child("addons").getValue(String.class);
                        String method = orderSnap.child("method").getValue(String.class);

                        // 🚨 PULL GPS COORDINATES FOR HISTORY RECORDS 🚨
                        Double latObj = orderSnap.child("latitude").getValue(Double.class);
                        Double lngObj = orderSnap.child("longitude").getValue(Double.class);
                        double latitude = (latObj != null) ? latObj : 0.0;
                        double longitude = (lngObj != null) ? lngObj : 0.0;

                        // 🚨 UPDATED: Using the 14-parameter constructor 🚨
                        allHistoryList.add(new AdminOrderModel(
                                uid, name, items, price, weight, status, historyId,
                                0, timestamp, category, addons, method,
                                latitude, longitude));
                    }
                }
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminHistoryActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Logic to ensure History displays the correct calculated price if needed
    private void applyFilters() {
        filteredList.clear();
        long now = System.currentTimeMillis();

        for (AdminOrderModel order : allHistoryList) {
            boolean include = false;
            long time = order.getTimestamp();

            // Apply Date Filters
            if (currentFilter.equals("All Time")) include = true;
            else if (currentFilter.equals("Today")) include = isSameDay(now, time);
            else if (currentFilter.equals("This Week")) include = isSameWeek(now, time);
            else if (currentFilter.equals("This Month")) include = isSameMonth(now, time);

            if (include) filteredList.add(order);
        }
        adapter.notifyDataSetChanged();
    }

    // --- BULLETPROOF HELPER METHODS (Updated to match Dashboard) ---

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

    // --- Date Utility Methods ---
    private boolean isSameDay(long t1, long t2) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2);
        return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }

    private boolean isSameWeek(long t1, long t2) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2);
        return c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR) && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }

    private boolean isSameMonth(long t1, long t2) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2);
        return c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
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
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_history);

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
}