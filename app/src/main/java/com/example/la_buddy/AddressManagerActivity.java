package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddressManagerActivity extends AppCompatActivity {

    private RecyclerView rvAddresses;
    private TextView tvEmptyState;
    private DatabaseReference addressRef;
    private String currentUid;
    // You will need to create an Adapter class to handle the list items
    // private AddressAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_manager);

        rvAddresses = findViewById(R.id.rvAddresses);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ImageView btnBack = findViewById(R.id.btnBack);
        View btnAdd = findViewById(R.id.btnAddAddress);

        currentUid = FirebaseAuth.getInstance().getUid();

        // Logical path: Addresses -> UserID -> UniqueID for each address
        addressRef = FirebaseDatabase.getInstance().getReference("Addresses").child(currentUid);

        rvAddresses.setLayoutManager(new LinearLayoutManager(this));

        btnBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v -> {
            // Navigate to an activity where users can type a new address
            // startActivity(new Intent(this, AddAddressActivity.class));
            Toast.makeText(this, "Opening Add Address form...", Toast.LENGTH_SHORT).show();
        });

        loadAddresses();
    }

    private void loadAddresses() {
        addressRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvEmptyState.setVisibility(View.GONE);
                    // Logic to populate RecyclerView would go here
                } else {
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddressManagerActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}