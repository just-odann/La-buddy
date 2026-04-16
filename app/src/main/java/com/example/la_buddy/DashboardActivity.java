package com.example.la_buddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String currentUid;

    private TextView tvUserName, tvOrderId, tvWeight, tvPrice, tvStatusBanner, tvSecurityCode;
    private View readyBanner;
    private ImageView btnLogout;

    // Timeline Steps
    private View step1, step2, step3, step4, step5;

    // Bottom Nav Containers
    private LinearLayout navHome, navDashboard, navHistory, navProfile, navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Firebase Initialization
        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            goToLogin();
            return;
        }
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Orders").child(currentUid);

        // 2. Bind Views (Timeline Steps)
        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        step4 = findViewById(R.id.step4);
        step5 = findViewById(R.id.step5);

        // 3. Bind Header & Details
        tvUserName = findViewById(R.id.tvUserName);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvWeight = findViewById(R.id.tvWeight);
        tvPrice = findViewById(R.id.tvPrice);
        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvSecurityCode = findViewById(R.id.tvSecurityCode);
        readyBanner = findViewById(R.id.readyBanner);
        btnLogout = findViewById(R.id.btnLogout);

        // 4. Bind Bottom Navigation
        navHome = findViewById(R.id.navHome);
        navDashboard = findViewById(R.id.navDashboard);
        navHistory = findViewById(R.id.navHistory);
        navProfile = findViewById(R.id.navProfile);
        navSettings = findViewById(R.id.navSettings);

        setupStepStaticContent();


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
                    String name = snapshot.child("name").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);
                    String weight = snapshot.child("weight").getValue(String.class);
                    String price = snapshot.child("price").getValue(String.class);
                    String orderId = snapshot.child("orderId").getValue(String.class);

                    if (name != null) tvUserName.setText("Hello, " + name + "!");
                    if (weight != null) tvWeight.setText("Total Weight: " + weight + " kg");
                    if (price != null) tvPrice.setText("Price: ₱" + price);
                    if (orderId != null) tvOrderId.setText("Order #" + orderId);

                    updateTimelineUI(status);

                    if ("Ready".equalsIgnoreCase(status)) {
                        readyBanner.setVisibility(View.VISIBLE);
                        handleReadyState();
                    } else {
                        readyBanner.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupStepStaticContent() {
        // Matches the IDs in your item_timeline_step.xml
        setStepData(step1, R.drawable.ic_clipboard, "Received");
        setStepData(step2, R.drawable.ic_washing_machine, "Washing");
        setStepData(step3, R.drawable.ic_drying, "Drying");
        setStepData(step4, R.drawable.ic_folding, "Folding");
        setStepData(step5, R.drawable.ic_check_circle, "Ready");

        // Hide the line on the very last step
        View line5 = step5.findViewById(R.id.timelineLine);
        if (line5 != null) line5.setVisibility(View.GONE);
    }

    private void setStepData(View view, int iconRes, String title) {
        ((ImageView) view.findViewById(R.id.stepIcon)).setImageResource(iconRes);
        ((TextView) view.findViewById(R.id.tvStepTitle)).setText(title);
    }

    private void updateTimelineUI(String currentStatus) {
        resetAllSteps();
        if (currentStatus == null) return;

        // Uses fall-through switch to highlight all completed stages
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
        int color = isFinal ? Color.parseColor("#2E7D32") : Color.parseColor("#1A3E6D");
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
                tvSecurityCode.setText("Code: " + task.getResult().getValue().toString());
                tvSecurityCode.setVisibility(View.VISIBLE);
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}