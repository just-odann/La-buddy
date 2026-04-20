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

        // Category Logic
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ArrayAdapter<CharSequence> adapter;
                switch (position) {
                    case 1:
                        adapter = ArrayAdapter.createFromResource(BookingActivity.this, R.array.regular_items, android.R.layout.simple_spinner_item);
                        spinnerItems.setAdapter(adapter);
                        break;
                    case 2:
                        adapter = ArrayAdapter.createFromResource(BookingActivity.this, R.array.specialty_items, android.R.layout.simple_spinner_item);
                        spinnerItems.setAdapter(adapter);
                        break;
                    case 3:
                        adapter = ArrayAdapter.createFromResource(BookingActivity.this, R.array.polo_items, android.R.layout.simple_spinner_item);
                        spinnerItems.setAdapter(adapter);
                        break;
                    default:
                        spinnerItems.setAdapter(null);
                        break;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && spinnerItems.getAdapter() != null) {
                    layoutLogistics.setVisibility(View.VISIBLE);
                    layoutLogistics.setAlpha(0f);
                    layoutLogistics.animate().alpha(1f).setDuration(500);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
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
        mDatabase.child("Orders").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // An order node exists. Now check if the status is actually "ongoing"
                String currentStatus = task.getResult().child("status").getValue(String.class);

                // If status is NOT null and NOT "Completed", block the new order
                if (currentStatus != null && !currentStatus.equalsIgnoreCase("Completed")
                        && !currentStatus.equalsIgnoreCase("Picked Up")) {

                    Toast.makeText(this, "You still have an ongoing order! Please wait for it to be completed.", Toast.LENGTH_LONG).show();
                    return; // STOP the method here
                }
            }

            // --- STEP 2: PROCEED WITH BOOKING ---
            // If the code reaches here, it means no active order was found
            processFinalBooking(uid);
        });
    }

    private void processFinalBooking(String uid) {
        mDatabase.child("Users").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // 1. Declare the variables inside this block so they are "visible"
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
                int basePrice = 100; // Default base price
                int deliveryFee = 0;
                if (serviceType.equals("Pickup & Delivery")) {
                    deliveryFee = 50; // The automatic 50 pesos fee
                }
                int totalAmount = basePrice + deliveryFee;

                // 5. Create the HashMap and put EVERYTHING inside
                HashMap<String, Object> order = new HashMap<>();
                order.put("name", realName);
                order.put("phone", realPhone);
                order.put("category", category);
                order.put("item", item);
                order.put("detergent", detergent); // Fixes "never accessed" warning
                order.put("fabcon", fabcon);       // Fixes "never accessed" warning
                order.put("isStudent", switchStudent.isChecked()); // Fixes "never accessed" warning
                order.put("serviceType", serviceType);
                order.put("price", String.valueOf(totalAmount));
                order.put("status", "Received");
                order.put("latitude", selectedLat);
                order.put("longitude", selectedLng);
                order.put("timestamp", System.currentTimeMillis());

                // 6. Save to Firebase
                mDatabase.child("Orders").child(uid).setValue(order)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Total: ₱" + totalAmount + " (Fee included)", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(BookingActivity.this, HomeActivity.class));
                            finish();
                        });
            }
        });
    }
}