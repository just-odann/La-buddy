package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

/**
 * BookingActivity handles the laundry service selection logic,
 * dynamic dropdown population, and Firebase order submission.
 */
public class BookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AutoCompleteTextView autoCategory, autoItems, autoDetergent, autoFabcon;
    private SwitchMaterial switchStudent;
    private Button btnProcess;
    private MaterialCardView cardMap;
    private MaterialButtonToggleGroup toggleMethod;
    private DatabaseReference mDatabase;

    private GoogleMap mMap;
    private double selectedLat = 14.1167;
    private double selectedLng = 122.9500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        autoCategory = findViewById(R.id.autoCategory);
        autoItems = findViewById(R.id.autoItems);
        autoDetergent = findViewById(R.id.autoDetergent);
        autoFabcon = findViewById(R.id.autoFabcon);
        switchStudent = findViewById(R.id.switchStudent);
        btnProcess = findViewById(R.id.btnProcessOrder);
        cardMap = findViewById(R.id.cardMap);
        toggleMethod = findViewById(R.id.toggleMethod);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupDropdowns();

        toggleMethod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnModeDropoff) {
                    cardMap.setVisibility(View.GONE);
                } else {
                    cardMap.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "A logistics fee of ₱50 will be applied", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnProcess.setOnClickListener(v -> submitBooking());
    }

    private void setupDropdowns() {
        String[] categories = getResources().getStringArray(R.array.service_categories);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        autoCategory.setAdapter(catAdapter);

        autoCategory.setOnItemClickListener((parent, view, position, id) -> {
            ArrayAdapter<CharSequence> itemAdapter = null;
            switch (position) {
                case 0: // WASH, DRY & FOLD
                    itemAdapter = ArrayAdapter.createFromResource(this, R.array.regular_items, android.R.layout.simple_list_item_1);
                    break;
                case 1: // PREMIUM CARE / DRY CLEANING
                    itemAdapter = ArrayAdapter.createFromResource(this, R.array.specialty_items, android.R.layout.simple_list_item_1);
                    break;
                case 2: // WASH, DRY & IRONING
                    itemAdapter = ArrayAdapter.createFromResource(this, R.array.polo_items, android.R.layout.simple_list_item_1);
                    break;
            }

            if (itemAdapter != null) {
                autoItems.setAdapter(itemAdapter);
                // IMPORTANT: false prevents the AutoCompleteTextView from filtering out results
                autoItems.setText("", false);
            }
        });

        String[] detergents = getResources().getStringArray(R.array.detergent_options);
        ArrayAdapter<String> detAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, detergents);
        autoDetergent.setAdapter(detAdapter);

        String[] fabcons = getResources().getStringArray(R.array.fabcon_options);
        ArrayAdapter<String> fabAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fabcons);
        autoFabcon.setAdapter(fabAdapter);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng daet = new LatLng(selectedLat, selectedLng);
        mMap.addMarker(new MarkerOptions().position(daet).title("Current Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(daet, 15f));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Set Delivery Location"));
            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;
        });
    }

    private void submitBooking() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = autoCategory.getText().toString();
        String item = autoItems.getText().toString();

        if (category.isEmpty() || item.isEmpty()) {
            Toast.makeText(this, "Please select Service and Item", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        mDatabase.child("Orders").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String status = task.getResult().child("status").getValue(String.class);
                if (status != null && !status.equals("Completed") && !status.equals("Picked Up")) {
                    Toast.makeText(this, "You have an ongoing order!", Toast.LENGTH_LONG).show();
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

                String category = autoCategory.getText().toString();
                String item = autoItems.getText().toString();
                String detergent = autoDetergent.getText().toString();
                String fabcon = autoFabcon.getText().toString();
                String serviceType = (toggleMethod.getCheckedButtonId() == R.id.btnModePickup)
                        ? "Pickup & Delivery" : "Walk-in";

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                String currentDate = sdf.format(new java.util.Date());
                String addons = (detergent.isEmpty() ? "None" : detergent) + " | " + (fabcon.isEmpty() ? "None" : fabcon);

                HashMap<String, Object> order = new HashMap<>();
                order.put("name", realName);
                order.put("category", category);
                order.put("items", item);
                order.put("addons", addons);
                order.put("isStudent", switchStudent.isChecked());
                order.put("method", serviceType);
                order.put("status", "Order Placed");
                order.put("latitude", selectedLat);
                order.put("longitude", selectedLng);
                order.put("timestamp", System.currentTimeMillis());

                mDatabase.child("Orders").child(uid).setValue(order);

                DatabaseReference historyRef = mDatabase.child("OrderHistory").child(uid);
                String orderId = historyRef.push().getKey();
                if (orderId != null) {
                    order.put("date", currentDate);
                    historyRef.child(orderId).setValue(order).addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Success! Your order is placed.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(BookingActivity.this, HomeActivity.class));
                        finish();
                    });
                }
            }
        });
    }
}