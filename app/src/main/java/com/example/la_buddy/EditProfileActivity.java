package com.example.la_buddy;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etEmail, etAge;
    private AutoCompleteTextView spinnerGender;
    private DatabaseReference userRef;
    private String currentUid;
    private String originalEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 1. Initialize Views
        etName = findViewById(R.id.etEditName);
        etPhone = findViewById(R.id.etEditPhone);
        etEmail = findViewById(R.id.etEditEmail);
        etAge = findViewById(R.id.etEditAge);
        spinnerGender = findViewById(R.id.spinnerGender);
        Button btnSave = findViewById(R.id.btnSaveChanges);
        ImageView btnBack = findViewById(R.id.btnBack);

        // 2. Setup Gender Dropdown
        String[] genders = {"Male", "Female", "Prefer not to say"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genders);
        spinnerGender.setAdapter(adapter);

        // 3. Firebase Setup
        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUid);
            loadCurrentData();
        }

        // 4. Click Listeners
        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String gender = spinnerGender.getText().toString().trim();
            String age = etAge.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();

            if (!name.isEmpty() && !phone.isEmpty()) {
                updateProfile(name, phone, gender, age);

                // Trigger verification if email was changed
                if (!newEmail.equals(originalEmail)) {
                    handleEmailChange(newEmail);
                }
            } else {
                Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCurrentData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            originalEmail = user.getEmail();
            etEmail.setText(originalEmail); // Automatically fills email
        }

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String age = snapshot.child("age").getValue(String.class);

                    if (name != null) etName.setText(name);
                    if (phone != null) etPhone.setText(phone);
                    if (gender != null) spinnerGender.setText(gender, false);
                    if (age != null) etAge.setText(age);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateProfile(String name, String phone, String gender, String age) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("gender", gender);
        updates.put("age", age);

        userRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Profile Details Updated!", Toast.LENGTH_SHORT).show();
            if (etEmail.getText().toString().trim().equals(originalEmail)) {
                finish();
            }
        });
    }

    private void handleEmailChange(String newEmail) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Sends verification to the new email address
            user.verifyBeforeUpdateEmail(newEmail)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Verification sent to " + newEmail, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}