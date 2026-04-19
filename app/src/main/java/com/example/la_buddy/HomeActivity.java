package com.example.la_buddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String currentUid;

    private TextView tvUserName, tvOrderId, tvWeight, tvPrice, tvSecurityCode;
    private View activeOrderCard, timelineContainer;
    private ImageView btnLogout;
    private FloatingActionButton fabContact;

    private View btnBookWash;
    private CardView cardPriceList, cardStoreInfo;

    private View step1, step2, step3, step4, step5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            goToLogin();
            return;
        }

        // Points to the Orders node for this specific user
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Orders").child(currentUid);

        // 1. BIND VIEWS
        timelineContainer = findViewById(R.id.timelineContainer);
        activeOrderCard = findViewById(R.id.activeOrderCard);

        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        step4 = findViewById(R.id.step4);
        step5 = findViewById(R.id.step5);

        tvUserName = findViewById(R.id.userName);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvWeight = findViewById(R.id.tvWeight);
        tvPrice = findViewById(R.id.tvPrice);
        tvSecurityCode = findViewById(R.id.tvSecurityCode);

        btnLogout = findViewById(R.id.btnLogout);
        fabContact = findViewById(R.id.fabContact);
        btnBookWash = findViewById(R.id.btnBookWash);
        cardPriceList = findViewById(R.id.cardPriceList);
        cardStoreInfo = findViewById(R.id.cardStoreInfo);

        // INITIAL STATE: Everything hidden until data is confirmed
        hideBookingUI();
        setupStepStaticContent();

        // 2. LISTENERS
        if (btnBookWash != null) {
            btnBookWash.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(HomeActivity.this, BookingActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    // This Toast will tell us the REAL reason (e.g., ActivityNotFound or NullPointer)
                    Toast.makeText(HomeActivity.this, "Crash Reason: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                goToLogin();
            });
        }

        if (cardStoreInfo != null) {
            cardStoreInfo.setOnClickListener(v -> {
                // Geo-coordinates for Paracale area
                Uri gmmIntentUri = Uri.parse("geo:14.2833,122.7833?q=Laundry+Shop");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            });
        }

        // 3. FIREBASE LIVE LISTENER
        mDatabase.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        String status = data.child("status").getValue(String.class);

                        // ONLY SHOW if Admin has updated the status to something valid
                        if (status != null && !status.equalsIgnoreCase("None") && !status.isEmpty()) {
                            showBookingUI();
                            updateTimelineUI(status);

                            String weight = data.child("weight").getValue(String.class);
                            String price = data.child("price").getValue(String.class);

                            if (tvWeight != null) tvWeight.setText("Weight: " + (weight != null ? weight : "0") + " kg");
                            if (tvPrice != null) tvPrice.setText("₱" + (price != null ? price : "0"));
                            if (tvOrderId != null) tvOrderId.setText("Order #" + data.getKey().substring(1, 7).toUpperCase());

                            if ("Ready".equalsIgnoreCase(status)) {
                                handleReadyState(data);
                            } else if (tvSecurityCode != null) {
                                tvSecurityCode.setVisibility(View.GONE);
                            }
                        } else {
                            hideBookingUI();
                        }
                    }
                } else {
                    hideBookingUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showBookingUI() {
        if (timelineContainer != null) timelineContainer.setVisibility(View.VISIBLE);
        if (activeOrderCard != null) activeOrderCard.setVisibility(View.VISIBLE);
    }

    private void hideBookingUI() {
        if (timelineContainer != null) timelineContainer.setVisibility(View.GONE);
        if (activeOrderCard != null) activeOrderCard.setVisibility(View.GONE);
    }

    private void setupStepStaticContent() {
        setStepData(step1, R.drawable.ic_clipboard, "Stage 1", "Received");
        setStepData(step2, R.drawable.ic_washing_machine, "Stage 2", "Washing");
        setStepData(step3, R.drawable.ic_drying, "Stage 3", "Drying");
        setStepData(step4, R.drawable.ic_folding, "Stage 4", "Folding");
        setStepData(step5, R.drawable.ic_check_circle, "Stage 5", "Ready");
    }

    private void setStepData(View view, int iconRes, String stageLabel, String title) {
        if (view == null) return;
        ImageView icon = view.findViewById(R.id.stepIcon);
        TextView label = view.findViewById(R.id.tvStepLabel);
        TextView stepTitle = view.findViewById(R.id.tvStepTitle);

        if (icon != null) icon.setImageResource(iconRes);
        if (label != null) label.setText(stageLabel);
        if (stepTitle != null) stepTitle.setText(title);
    }

    private void updateTimelineUI(String currentStatus) {
        resetAllSteps();
        if (currentStatus == null) return;

        switch (currentStatus) {
            case "Ready": highlightStep(step5, true);
            case "Folding": highlightStep(step4, false);
            case "Drying": highlightStep(step3, false);
            case "Washing": highlightStep(step2, false);
            case "Received": highlightStep(step1, false);
                break;
        }
    }

    private void highlightStep(View view, boolean isFinal) {
        if (view == null) return;
        int color = isFinal ? Color.parseColor("#2E7D32") : Color.parseColor("#1A73E8");
        View dot = view.findViewById(R.id.stepDot);
        if (dot != null && dot.getBackground() != null) dot.getBackground().setTint(color);
        View line = view.findViewById(R.id.timelineLine);
        if (line != null) line.setBackgroundColor(color);
    }

    private void resetAllSteps() {
        int grey = Color.parseColor("#D1D9E6");
        View[] steps = {step1, step2, step3, step4, step5};
        for (View s : steps) {
            if (s == null) continue;
            View dot = s.findViewById(R.id.stepDot);
            if (dot != null && dot.getBackground() != null) dot.getBackground().setTint(grey);
            View line = s.findViewById(R.id.timelineLine);
            if (line != null) line.setBackgroundColor(grey);
        }
    }

    private void handleReadyState(DataSnapshot data) {
        if (tvSecurityCode == null) return;
        String code = data.child("pickupCode").getValue(String.class);
        if (code != null) {
            tvSecurityCode.setText("Claim Code: " + code);
            tvSecurityCode.setVisibility(View.VISIBLE);
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}