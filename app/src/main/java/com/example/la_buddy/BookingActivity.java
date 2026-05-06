package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class BookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Spinner spinnerCategory, spinnerItems, spinnerDetergent, spinnerFabcon;
    private SwitchMaterial switchStudent;
    private Button btnProcess;
    private LinearLayout layoutLogistics;
    private CardView cardMap;
    private MaterialButtonToggleGroup toggleMethod;
    private DatabaseReference mDatabase;

    // MAP VARIABLES
    private GoogleMap mMap;
    private double selectedLat = 14.1167; // Default: Daet
    private double selectedLng = 122.9500;

    private TextView tvSelectedRate;
    private TextView tvTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize Views
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerItems = findViewById(R.id.spinnerItems);
        spinnerDetergent = findViewById(R.id.spinnerDetergent);
        spinnerFabcon = findViewById(R.id.spinnerFabcon);
        switchStudent = findViewById(R.id.switchStudent);
        btnProcess = findViewById(R.id.btnProcessOrder);
        layoutLogistics = findViewById(R.id.layoutLogistics);
        cardMap = findViewById(R.id.cardMap);
        toggleMethod = findViewById(R.id.toggleMethod);

        // (Optional) If you have these in your XML, uncomment the next two lines:
        // tvSelectedRate = findViewById(R.id.tvSelectedRate);
        // tvTotal = findViewById(R.id.tvTotal);

        // --- 1. THE CATEGORY SPINNER ---
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ArrayAdapter<CharSequence> adapter = null;

                switch (position) {
                    case 1:
                        adapter = ArrayAdapter.createFromResource(BookingActivity.this, R.array.regular_items, android.R.layout.simple_spinner_item);
                        break;
                    case 2:
                        adapter = ArrayAdapter.createFromResource(BookingActivity.this, R.array.specialty_items, android.R.layout.simple_spinner_item);
                        break;
                    case 3:
                        adapter = ArrayAdapter.createFromResource(BookingActivity.this, R.array.polo_items, android.R.layout.simple_spinner_item);
                        break;
                }

                if (adapter != null) {
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerItems.setAdapter(adapter);
                } else {
                    spinnerItems.setAdapter(null);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // --- 2. THE SPECIFIC ITEM SPINNER ---
        spinnerItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position >= 0 && spinnerItems.getAdapter() != null) {
                    layoutLogistics.setVisibility(View.VISIBLE);
                    layoutLogistics.setAlpha(0f);
                    layoutLogistics.animate().alpha(1f).setDuration(500);
                }

                String selectedService = parent.getItemAtPosition(position).toString();
                String extractedRate = "0.00";

                if (selectedService.contains("₱")) {
                    extractedRate = selectedService.substring(selectedService.indexOf("₱") + 1).trim();
                }

                if (tvSelectedRate != null) {
                    tvSelectedRate.setText("Rate: ₱" + extractedRate + " / kg");
                }
                if (tvTotal != null) {
                    tvTotal.setText("Total: Pending Admin Weigh-in");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        toggleMethod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnModeDropoff) {
                    cardMap.setVisibility(View.GONE);
                } else {
                    cardMap.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Logistics fee: ₱50 added", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnProcess.setOnClickListener(v -> submitBooking());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng daet = new LatLng(selectedLat, selectedLng);
        mMap.addMarker(new MarkerOptions().position(daet).title("Pick-up/Delivery Point"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(daet, 15f));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Deliver Here"));
            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;
        });
    }

    private void submitBooking() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        mDatabase.child("Orders").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String currentStatus = task.getResult().child("status").getValue(String.class);

                if (currentStatus != null && !currentStatus.equalsIgnoreCase("Completed")
                        && !currentStatus.equalsIgnoreCase("Picked Up")) {

                    Toast.makeText(this, "You still have an ongoing order! Please wait for it to be completed.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            processFinalBooking(uid);
        });
    }

    private void processFinalBooking(String uid) {
        mDatabase.child("Users").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {

                String realName = task.getResult().child("name").getValue(String.class);
                String realPhone = task.getResult().child("phone").getValue(String.class);

                String category = (spinnerCategory.getSelectedItem() != null) ? spinnerCategory.getSelectedItem().toString() : "Regular";
                String item = (spinnerItems.getSelectedItem() != null) ? spinnerItems.getSelectedItem().toString() : "N/A";
                String detergent = (spinnerDetergent.getSelectedItem() != null) ? spinnerDetergent.getSelectedItem().toString() : "None";
                String fabcon = (spinnerFabcon.getSelectedItem() != null) ? spinnerFabcon.getSelectedItem().toString() : "None";

                String serviceType = (toggleMethod.getCheckedButtonId() == R.id.btnModePickup)
                        ? "Pickup & Delivery"
                        : "Walk-in";

                // Formatted Date
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                String currentDate = sdf.format(new java.util.Date());

                // ------------------------------------------------------------------
                // DUAL-SAVE ARCHITECTURE (Preserves Chat & Creates Permanent Record)
                // ------------------------------------------------------------------

                // Create the unified Addons String (Cleaned up redundant null checks)
                String combinedAddons = detergent + " | " + fabcon;

                // --- A. THE LIVE QUEUE UPDATE ---
                HashMap<String, Object> liveOrder = new HashMap<>();
                liveOrder.put("name", realName);
                liveOrder.put("phone", realPhone);
                liveOrder.put("category", category);
                liveOrder.put("items", item);
                liveOrder.put("addons", combinedAddons);
                liveOrder.put("isStudent", switchStudent.isChecked());
                liveOrder.put("method", serviceType);
                liveOrder.put("price", "Pending");
                liveOrder.put("weight", "Pending Drop-off");
                liveOrder.put("status", "Dropped Off");
                liveOrder.put("latitude", selectedLat);
                liveOrder.put("longitude", selectedLng);
                liveOrder.put("timestamp", System.currentTimeMillis());

                mDatabase.child("Orders").child(uid).updateChildren(liveOrder);

                // --- B. THE PERMANENT ORDER HISTORY ---
                DatabaseReference historyRef = mDatabase.child("OrderHistory").child(uid);
                String uniqueOrderId = historyRef.push().getKey();

                if (uniqueOrderId != null) {
                    mDatabase.child("Orders").child(uid).child("historyId").setValue(uniqueOrderId);

                    HashMap<String, Object> historyData = new HashMap<>();
                    historyData.put("orderId", uniqueOrderId);
                    historyData.put("date", currentDate);
                    historyData.put("status", "Dropped Off");
                    historyData.put("name", realName);
                    historyData.put("category", category);
                    historyData.put("items", item);
                    historyData.put("method", serviceType);
                    historyData.put("addons", combinedAddons);
                    historyData.put("price", "Pending");
                    historyData.put("weight", "Pending Drop-off");

                    historyRef.child(uniqueOrderId).setValue(historyData).addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Order Placed! Final price will be calculated at drop-off.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(BookingActivity.this, HomeActivity.class));
                        finish();
                    });
                }
            }
        });
    }
}