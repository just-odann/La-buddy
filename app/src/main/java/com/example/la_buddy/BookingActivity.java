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
                .findFragmentById(R.id.map); // Ensure your XML has a fragment with this ID
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

        // --- 1. THE CATEGORY SPINNER (Fills the second box) ---
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ArrayAdapter<CharSequence> adapter = null;

                // Based on your previous code: position 0 is likely "Select Category"
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

                // Apply the adapter to the second box
                if (adapter != null) {
                    // CRITICAL FIX: This line makes sure the dropdown actually looks like a dropdown menu!
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerItems.setAdapter(adapter);
                } else {
                    spinnerItems.setAdapter(null); // Clear it out if they haven't picked a category
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

// --- 2. THE SPECIFIC ITEM SPINNER (Calculates the Rate & Shows Logistics) ---
        spinnerItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                // 1. Show the Logistics Layout
                if (position >= 0 && spinnerItems.getAdapter() != null) {
                    layoutLogistics.setVisibility(View.VISIBLE);
                    layoutLogistics.setAlpha(0f);
                    layoutLogistics.animate().alpha(1f).setDuration(500);
                }

                // 2. Extract the price for the UI
                String selectedService = parent.getItemAtPosition(position).toString();
                String extractedRate = "0.00";

                // Only try to grab a price if the string actually has the Peso sign
                if (selectedService.contains("₱")) {
                    extractedRate = selectedService.substring(selectedService.indexOf("₱") + 1).trim();
                }

                // 3. Update the UI TextBoxes
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
                    // Optional: Update a TextView to show ₱0 fee
                } else {
                    cardMap.setVisibility(View.VISIBLE);
                    // Optional: Update a TextView to show ₱50 fee
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

        // Let user pick a custom location anywhere!
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

        // --- STEP 1: THE RESTRICTION CHECK ---
        // Look into the "Orders" node for this specific User ID
        // --- STEP 1: THE RESTRICTION CHECK ---
        mDatabase.child("Orders").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String currentStatus = task.getResult().child("status").getValue(String.class);

                // This line is the KEY:
                // It only blocks the user IF the status is NOT "Completed" and NOT "Picked Up"
                if (currentStatus != null && !currentStatus.equalsIgnoreCase("Completed")
                        && !currentStatus.equalsIgnoreCase("Picked Up")) {

                    Toast.makeText(this, "You still have an ongoing order! Please wait for it to be completed.", Toast.LENGTH_LONG).show();
                    return; // Stops them from booking
                }
            }

            // If the status IS "Completed", it skips the IF block and runs this:
            processFinalBooking(uid);
        });
    }

    private void processFinalBooking(String uid) {
        mDatabase.child("Users").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // 1. Declare the variables inside this block
                String realName = task.getResult().child("name").getValue(String.class);
                String realPhone = task.getResult().child("phone").getValue(String.class);

                // 2. Get your Spinner Selections
                String category = (spinnerCategory.getSelectedItem() != null) ? spinnerCategory.getSelectedItem().toString() : "Regular";
                String item = (spinnerItems.getSelectedItem() != null) ? spinnerItems.getSelectedItem().toString() : "N/A";
                String detergent = (spinnerDetergent.getSelectedItem() != null) ? spinnerDetergent.getSelectedItem().toString() : "None";
                String fabcon = (spinnerFabcon.getSelectedItem() != null) ? spinnerFabcon.getSelectedItem().toString() : "None";

                // 3. Logic for the Service Type and Fee
                String serviceType = (toggleMethod.getCheckedButtonId() == R.id.btnModePickup)
                        ? "Pickup & Delivery"
                        : "Walk-in";
                // 4. Calculate Total
                double basePrice = 0;

                // --- A. ITEM BASE PRICE ---
                // (Adjust these names to match EXACTLY what is in your strings.xml arrays)
                if (item.contains("Baro't Saya") || item.contains("Polo")) {
                    basePrice = 150.0;
                } else if (category.equalsIgnoreCase("Regular")) {
                    basePrice = 100.0; // Standard price for regular items
                } else if (category.equalsIgnoreCase("Specialty")) {
                    basePrice = 180.0; // Standard price for specialty items
                } else {
                    basePrice = 100.0; // Fallback price
                }

                // --- B. ADD-ONS (DETERGENT & FABCON) ---
                double addonsPrice = 0;
                // If they picked something other than "None" or standard, charge them
                if (!detergent.equalsIgnoreCase("None") && !detergent.equalsIgnoreCase("Standard")) {
                    addonsPrice += 15.0; // e.g., ₱15 for premium detergent
                }
                if (!fabcon.equalsIgnoreCase("None") && !fabcon.equalsIgnoreCase("Standard")) {
                    addonsPrice += 15.0; // e.g., ₱15 for premium fabcon
                }

                double subTotal = basePrice + addonsPrice;

                // --- C. STUDENT SAVER DISCOUNT ---
                if (switchStudent.isChecked()) {
                    // Applies a 10% discount to the laundry and add-ons
                    subTotal = subTotal - (subTotal * 0.10);
                }

                // --- D. LOGISTICS FEE ---
                double deliveryFee = 0;
                if (serviceType.equals("Pickup & Delivery")) {
                    deliveryFee = 50.0; // Flat 50 pesos fee
                }

                // --- E. FINAL TOTAL ---
                // Combine it all and round to the nearest whole number
                int totalAmount = (int) Math.round(subTotal + deliveryFee);

                // --- F. GET DATE FOR HISTORY ---
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                String currentDate = sdf.format(new java.util.Date());

                // ------------------------------------------------------------------
                // DUAL-SAVE ARCHITECTURE (Preserves Chat & Creates Permanent Record)
                // ------------------------------------------------------------------

                // A. THE LIVE QUEUE UPDATE
                HashMap<String, Object> liveOrder = new HashMap<>();
                liveOrder.put("name", realName);
                liveOrder.put("phone", realPhone);
                liveOrder.put("category", category);
                liveOrder.put("items", item);
                liveOrder.put("detergent", detergent);
                liveOrder.put("fabcon", fabcon);
                liveOrder.put("isStudent", switchStudent.isChecked());
                liveOrder.put("serviceType", serviceType);

                // THE FIX: Set price and weight to Pending
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
                    // Tell the Live Order about this receipt ID
                    mDatabase.child("Orders").child(uid).child("historyId").setValue(uniqueOrderId);

                    HashMap<String, Object> historyData = new HashMap<>();
                    historyData.put("orderId", uniqueOrderId);
                    historyData.put("date", currentDate);
                    historyData.put("status", "Dropped Off");
                    historyData.put("name", realName);

                    // --- CRITICAL KEY SYNCING ---
                    // 1. Fixed Category (Was missing!)
                    historyData.put("category", category);

                    // 2. Fixed Items (Added the 's' to match your Activity)
                    historyData.put("items", item);

                    // 3. Fixed Method (Changed from serviceType to method)
                    historyData.put("method", serviceType);

                    // 4. Fixed Add-ons (Combined them into one string like your History Activity expects)
                    String combinedAddons = (detergent != null ? detergent : "Standard") + " | " + (fabcon != null ? fabcon : "None");
                    historyData.put("addons", combinedAddons);

                    // 5. Fixed Price and Weight
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