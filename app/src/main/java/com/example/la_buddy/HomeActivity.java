package com.example.la_buddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

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
    private View readyBanner, activeOrderCard;
    private ImageView btnLogout;

    // Timeline Steps
    private View step1, step2, step3, step4, step5;

    // Bottom Nav (Updated to 3 items)
    private LinearLayout navHome, navHistory, navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Firebase Initialization
        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            goToLogin();
            return;
        }
        // Assuming your Firebase structure is: Orders -> Uid
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Orders").child(currentUid);

        // 2. Bind Views (Timeline Steps)
        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        step4 = findViewById(R.id.step4);
        step5 = findViewById(R.id.step5);

        // 3. Bind Header & Details
        tvUserName = findViewById(R.id.userName); // Matches your XML id
        tvOrderId = findViewById(R.id.tvOrderId);
        tvWeight = findViewById(R.id.tvWeight);
        tvPrice = findViewById(R.id.tvPrice);
        tvSecurityCode = findViewById(R.id.tvSecurityCode);
        readyBanner = findViewById(R.id.readyBanner);
        activeOrderCard = findViewById(R.id.activeOrderCard);
        btnLogout = findViewById(R.id.btnLogout);

        // 4. Bind Bottom Navigation (Simplified to 3)
        navHome = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navSettings = findViewById(R.id.navSettings);

        // Initialize Step Content with alternating logic
        setupStepStaticContent();

        // Logout Logic
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                goToLogin();
            });
        }

        // 5. Live Data Listener
        mDatabase.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    activeOrderCard.setVisibility(View.VISIBLE); // Show if order exists

                    String name = snapshot.child("name").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);
                    String weight = snapshot.child("weight").getValue(String.class);
                    String price = snapshot.child("price").getValue(String.class);
                    String orderId = snapshot.child("orderId").getValue(String.class);

                    if (name != null) tvUserName.setText(name + "!");
                    if (weight != null) tvWeight.setText("Total Weight: " + weight + " kg");
                    if (price != null) tvPrice.setText("Price: ₱" + price);
                    if (orderId != null) tvOrderId.setText("Order #" + orderId);

                    updateTimelineUI(status);

                    // Show claim code only at Stage 5
                    if ("Ready".equalsIgnoreCase(status)) {
                        handleReadyState();
                    } else {
                        tvSecurityCode.setVisibility(View.GONE);
                    }
                } else {
                    activeOrderCard.setVisibility(View.GONE); // Hide if no active order
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupStepStaticContent() {
        setStepData(step1, R.drawable.ic_clipboard, "Stage 1", "Received", false);
        setStepData(step2, R.drawable.ic_washing_machine, "Stage 2", "Washing", true); // Flip to Left
        setStepData(step3, R.drawable.ic_drying, "Stage 3", "Drying", false);
        setStepData(step4, R.drawable.ic_folding, "Stage 4", "Folding", true); // Flip to Left
        setStepData(step5, R.drawable.ic_check_circle, "Stage 5", "Ready", false);

        // Final line removal
        View line5 = step5.findViewById(R.id.timelineLine);
        if (line5 != null) line5.setVisibility(View.GONE);
    }

    private void setStepData(View view, int iconRes, String stageLabel, String title, boolean flipToLeft) {
        ((ImageView) view.findViewById(R.id.stepIcon)).setImageResource(iconRes);
        ((TextView) view.findViewById(R.id.tvStepLabel)).setText(stageLabel);
        ((TextView) view.findViewById(R.id.tvStepTitle)).setText(title);

        if (flipToLeft) {
            ConstraintLayout layout = (ConstraintLayout) view;
            ConstraintSet set = new ConstraintSet();
            set.clone(layout);

            // Move Label to left of icon
            set.clear(R.id.tvStepLabel, ConstraintSet.START);
            set.connect(R.id.tvStepLabel, ConstraintSet.END, R.id.stepIcon, ConstraintSet.START, 32);

            // Move Title to left of icon
            set.clear(R.id.tvStepTitle, ConstraintSet.START);
            set.connect(R.id.tvStepTitle, ConstraintSet.END, R.id.stepIcon, ConstraintSet.START, 32);

            set.applyTo(layout);
        }
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
        // Lively Blue for active stages, Dark Green for finalized
        int color = isFinal ? Color.parseColor("#2E7D32") : Color.parseColor("#1A73E8");
        ((TextView) view.findViewById(R.id.tvStepTitle)).setTextColor(color);
        view.findViewById(R.id.timelineLine).setBackgroundColor(color);
        ((ImageView) view.findViewById(R.id.stepIcon)).setColorFilter(color);
    }

    private void resetAllSteps() {
        int grey = Color.parseColor("#D1D9E6");
        View[] steps = {step1, step2, step3, step4, step5};
        for (View s : steps) {
            ((TextView) s.findViewById(R.id.tvStepTitle)).setTextColor(Color.GRAY);
            s.findViewById(R.id.timelineLine).setBackgroundColor(grey);
            ((ImageView) s.findViewById(R.id.stepIcon)).setColorFilter(grey);
        }
    }

    private void handleReadyState() {
        mDatabase.child("pickupCode").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                tvSecurityCode.setText("Claim Code: " + task.getResult().getValue().toString());
                tvSecurityCode.setVisibility(View.VISIBLE);
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}