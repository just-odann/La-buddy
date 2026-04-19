package com.example.la_buddy;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class BookingActivity extends AppCompatActivity {

    private Spinner spinnerCategory, spinnerItems;
    private CheckBox cbAriel, cbDowny;
    private Switch switchStudent;
    private Button btnProcess;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerItems = findViewById(R.id.spinnerItems);
        cbAriel = findViewById(R.id.cbAriel);
        cbDowny = findViewById(R.id.cbDowny);
        switchStudent = findViewById(R.id.switchStudent);
        btnProcess = findViewById(R.id.btnProcessOrder);

        // Logic to update second spinner based on first spinner
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) { // Regular
                    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(BookingActivity.this, R.array.regular_items, android.R.layout.simple_spinner_item);
                    spinnerItems.setAdapter(adapter);
                } else if (position == 2) { // Premium
                    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(BookingActivity.this, R.array.premium_items, android.R.layout.simple_spinner_item);
                    spinnerItems.setAdapter(adapter);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnProcess.setOnClickListener(v -> submitBooking());
    }

    private void submitBooking() {
        String uid = FirebaseAuth.getInstance().getUid();
        String selectedItem = spinnerItems.getSelectedItem().toString();
        boolean isStudent = switchStudent.isChecked();

        HashMap<String, Object> order = new HashMap<>();
        order.put("item", selectedItem);
        order.put("studentVerified", isStudent);
        order.put("status", "Received");
        order.put("timestamp", System.currentTimeMillis());

        mDatabase.child("Orders").child(uid).setValue(order).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Buddy is on the way!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}