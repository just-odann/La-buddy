package com.example.la_buddy;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class BookingActivity extends AppCompatActivity {

    private Spinner spinnerCategory, spinnerItems, spinnerDetergent, spinnerFabcon;
    private SwitchMaterial switchStudent;
    private Button btnProcess;
    private LinearLayout layoutLogistics;
    private CardView cardMap;
    private MaterialButtonToggleGroup toggleMethod;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // 1. Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 2. Initialize Views
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerItems = findViewById(R.id.spinnerItems);
        spinnerDetergent = findViewById(R.id.spinnerDetergent);
        spinnerFabcon = findViewById(R.id.spinnerFabcon);
        switchStudent = findViewById(R.id.switchStudent);
        btnProcess = findViewById(R.id.btnProcessOrder);
        layoutLogistics = findViewById(R.id.layoutLogistics);
        cardMap = findViewById(R.id.cardMap);
        toggleMethod = findViewById(R.id.toggleMethod);

        // 3. Category Logic: Update Specific Items based on Category
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ArrayAdapter<CharSequence> adapter;
                switch (position) {
                    case 1: // Regular Services
                        adapter = ArrayAdapter.createFromResource(BookingActivity.this, R.array.regular_items, android.R.layout.simple_spinner_item);
                        spinnerItems.setAdapter(adapter);
                        break;
                    case 2: // Baro't Saya & Barong
                        adapter = ArrayAdapter.createFromResource(BookingActivity.this, R.array.specialty_items, android.R.layout.simple_spinner_item);
                        spinnerItems.setAdapter(adapter);
                        break;
                    case 3: // Polos & Suits
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

        // 4. Reveal Logistics Section only when an item is selected
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

        // 5. Map Visibility Logic (Hide map if customer selects Drop-off)
        toggleMethod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnModeDropoff) {
                    cardMap.setVisibility(View.GONE);
                } else {
                    cardMap.setVisibility(View.VISIBLE);
                }
            }
        });

        btnProcess.setOnClickListener(v -> submitBooking());
    }

    private void submitBooking() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get Selections
        String category = spinnerCategory.getSelectedItem().toString();
        String item = (spinnerItems.getSelectedItem() != null) ? spinnerItems.getSelectedItem().toString() : "N/A";
        String detergent = spinnerDetergent.getSelectedItem().toString();
        String fabcon = spinnerFabcon.getSelectedItem().toString();
        boolean isStudent = switchStudent.isChecked();

        // Check Delivery Mode
        String mode = (toggleMethod.getCheckedButtonId() == R.id.btnModePickup) ? "Pick-up" : "Drop-off";

        // Create Firebase Object
        HashMap<String, Object> order = new HashMap<>();
        order.put("category", category);
        order.put("item", item);
        order.put("detergent", detergent);
        order.put("fabcon", fabcon);
        order.put("isStudent", isStudent);
        order.put("deliveryMode", mode);
        order.put("status", "Received");
        order.put("timestamp", System.currentTimeMillis());

        // Push to Firebase
        mDatabase.child("Orders").child(uid).push().setValue(order).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Booking Successful! Buddy is notified.", Toast.LENGTH_LONG).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}