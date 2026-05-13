package com.example.la_buddy;

import android.content.Intent; // Added for navigation
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class OrderHistoryActivity extends AppCompatActivity {

    private ListView listViewHistory;
    private ArrayList<String> historyList;
    private ArrayAdapter<String> adapter;
    private TextView tvTotalSpent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        // 1. Initialize UI elements
        listViewHistory = findViewById(R.id.listViewHistory);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        historyList = new ArrayList<>();

        // 2. Setup the adapter with your custom card layout
        adapter = new ArrayAdapter<>(this, R.layout.item_history, R.id.tvHistoryText, historyList);
        listViewHistory.setAdapter(adapter);

        // 3. Setup Bottom Navigation Buttons
        setupNavigation();

        // 4. Start the data download
        loadOrderHistory();
    }

    private void setupNavigation() {
        // HOME BUTTON: Navigates back to HomeActivity
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(OrderHistoryActivity.this, HomeActivity.class);
            // This flag ensures we don't build up a massive stack of activities
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // HISTORY BUTTON: Already in this activity
        findViewById(R.id.navHistory).setOnClickListener(v -> {
            // Optional: Smooth scroll to top instead of a Toast
            listViewHistory.smoothScrollToPosition(0);
        });

        // SETTINGS BUTTON: Now correctly opens the SettingsActivity
        findViewById(R.id.navSettings).setOnClickListener(v -> {
            Intent intent = new Intent(OrderHistoryActivity.this, SettingsActivity.class);
            // We do NOT call finish() here so the user can press 'back' to return to History
            startActivity(intent);
        });
    }

    private void loadOrderHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("OrderHistory")
                .child(user.getUid());

        historyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();
                double totalSpent = 0.0;

                if (!snapshot.exists()) {
                    historyList.add("No past orders found.");
                } else {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        // 1. SMART CATEGORY FETCH
                        String category = ds.hasChild("category") ? ds.child("category").getValue(String.class) :
                                (ds.hasChild("laundryType") ? ds.child("laundryType").getValue(String.class) : "Standard");

                        // 2. SMART ITEMS FETCH (Check 'items' then 'item')
                        String items = ds.hasChild("items") ? ds.child("items").getValue(String.class) :
                                (ds.hasChild("item") ? ds.child("item").getValue(String.class) : "Standard");

                        // 3. SMART METHOD FETCH (Check 'method' then 'serviceType')
                        String method = ds.hasChild("method") ? ds.child("method").getValue(String.class) :
                                (ds.hasChild("serviceType") ? ds.child("serviceType").getValue(String.class) : "Walk-in");

                        // 4. SMART ADD-ONS FETCH
                        String addons = ds.child("addons").getValue(String.class);
                        if (addons == null) {
                            // If 'addons' doesn't exist yet, combine detergent and fabcon manually
                            String det = ds.child("detergent").getValue(String.class);
                            String fab = ds.child("fabcon").getValue(String.class);
                            addons = (det != null ? det : "Standard") + " | " + (fab != null ? fab : "None");
                        }

                        String date = ds.child("date").getValue(String.class);
                        String weight = ds.child("weight").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);

                        // 5. PRICE CALCULATION
                        Object rawPrice = ds.child("price").getValue();
                        String priceString = (rawPrice != null) ? String.valueOf(rawPrice) : "0";
                        String cleanPrice = priceString.replace("₱", "").replace(",", "").trim();

                        try {
                            if (!cleanPrice.equalsIgnoreCase("Pending")) {
                                totalSpent += Double.parseDouble(cleanPrice);
                            }
                        } catch (Exception e) { }

                        // 6. FORMAT DISPLAY
                        String displayText =
                                "📅  Date: " + (date != null ? date : "N/A") + "\n" +
                                        "👕  Category: " + category + "\n" +
                                        "✨  Item(s): " + items + "\n" +
                                        "🧴  Add-ons: " + addons + "\n" +
                                        "🚚  Method: " + method + "\n" +
                                        "⚖️  Weight: " + (weight != null ? weight : "Pending") + "\n" +
                                        "💰  Price: ₱" + cleanPrice + "\n" +
                                        "📌  Status: " + (status != null ? status : "N/A");

                        historyList.add(0, displayText);
                    }
                }

                if (tvTotalSpent != null) {
                    tvTotalSpent.setText(String.format("Total Spent: ₱%.2f", totalSpent));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OrderHistoryActivity.this, "Failed to load history.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}