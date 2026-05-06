package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import android.widget.Toast;

public class AdminFinanceActivity extends AppCompatActivity {

    private RecyclerView rvFinance;
    private AdminOrderAdapter adapter;
    private ArrayList<AdminOrderModel> fullFinanceList, filteredList;
    private TextView tvTotalRevenue;
    private String currentFilter = "Year";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_finance);

        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        rvFinance = findViewById(R.id.rvFinance);
        rvFinance.setLayoutManager(new LinearLayoutManager(this));

        fullFinanceList = new ArrayList<>();
        filteredList = new ArrayList<>();
        // Read-only adapter
        adapter = new AdminOrderAdapter(filteredList, order -> {});
        rvFinance.setAdapter(adapter);

        ChipGroup chipGroup = findViewById(R.id.chipGroupFinance);
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.contains(R.id.chipFinanceToday)) currentFilter = "Day";
                else if (checkedIds.contains(R.id.chipFinanceWeek)) currentFilter = "Week";
                else if (checkedIds.contains(R.id.chipFinanceMonth)) currentFilter = "Month";
                else currentFilter = "Year";
                applyFinanceFilters();
            });
        }

        loadFinanceData();
        setupNavigation();
    }

    private void loadFinanceData() {
        DatabaseReference financeRef = FirebaseDatabase.getInstance().getReference("Finances");
        financeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullFinanceList.clear();
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

                        // 🚨 PULL GPS COORDINATES FROM FIREBASE 🚨
                        Double latObj = orderSnap.child("latitude").getValue(Double.class);
                        Double lngObj = orderSnap.child("longitude").getValue(Double.class);
                        double latitude = (latObj != null) ? latObj : 0.0;
                        double longitude = (lngObj != null) ? lngObj : 0.0;

                        // 🚨 UPDATED CONSTRUCTOR: Now passing all 14 parameters 🚨
                        fullFinanceList.add(new AdminOrderModel(
                                uid, name, items, price, weight, status, historyId,
                                0, timestamp, category, addons, method,
                                latitude, longitude));
                    }
                }
                applyFinanceFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void applyFinanceFilters() {
        filteredList.clear();
        double total = 0;
        long now = System.currentTimeMillis();

        for (AdminOrderModel order : fullFinanceList) {
            boolean include = false;
            long time = order.getTimestamp();

            switch (currentFilter) {
                case "Day": include = isSameDay(now, time); break;
                case "Week": include = isSameWeek(now, time); break;
                case "Month": include = isSameMonth(now, time); break;
                case "Year": include = isSameYear(now, time); break;
            }

            if (include) {
                filteredList.add(order);
                try {
                    String cleanPrice = order.getPrice().replace("₱", "").replace(",", "").trim();
                    total += Double.parseDouble(cleanPrice);
                } catch (Exception e) { /* Ignore invalid formats */ }
            }
        }
        tvTotalRevenue.setText(String.format(Locale.US, "₱%.2f", total));
        adapter.notifyDataSetChanged();
    }

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

    private boolean isSameYear(long t1, long t2) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }

    public void syncHistoryToFinance() {
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("OrderHistory");
        DatabaseReference financeRef = FirebaseDatabase.getInstance().getReference("Finances");

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String userKey = userSnap.getKey();
                    if (userKey == null) continue;

                    for (DataSnapshot orderSnap : userSnap.getChildren()) {
                        String orderKey = orderSnap.getKey();
                        if (orderKey == null) continue;

                        String status = orderSnap.child("status").getValue(String.class);
                        if ("Picked Up".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                            financeRef.child(userKey).child(orderKey).setValue(orderSnap.getValue());
                        }
                    }
                }
                Toast.makeText(AdminFinanceActivity.this, "Finances Updated!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ===============================================================
    // 🚨 THE NUCLEAR FIX FOR NAVIGATION 🚨
    // ===============================================================
    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        // Highlights the Finance icon!
        bottomNav.setSelectedItemId(R.id.nav_finance);

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
            }

            if (intent != null) {
                // Strip all animations from the Intent
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
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