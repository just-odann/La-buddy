package com.example.la_buddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String currentUid;

    // UI Elements: Profile & Order Details
    private TextView tvUserName, tvOrderId, tvWeight, tvPrice, tvStatusBanner, tvSecurityCode;
    private View readyBanner;
    private ImageView btnLogout;

    // UI Elements: Timeline Steps
    private View step1, step2, step3, step4, step5;

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

        // 3. Bind Views (Header & Details)
        tvUserName = findViewById(R.id.tvUserName);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvWeight = findViewById(R.id.tvWeight);
        tvPrice = findViewById(R.id.tvPrice);
        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvSecurityCode = findViewById(R.id.tvSecurityCode);
        readyBanner = findViewById(R.id.readyBanner);
        btnLogout = findViewById(R.id.btnLogout);

        setupStepStaticContent();

        // 4. Logout Logic
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
                    // Extract data from Firebase
                    String name = snapshot.child("name").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);
                    String weight = snapshot.child("weight").getValue(String.class);
                    String price = snapshot.child("price").getValue(String.class);
                    String orderId = snapshot.child("orderId").getValue(String.class);

                    // Update UI Details
                    if (name != null) tvUserName.setText("Hello, " + name + "!");
                    if (weight != null) tvWeight.setText("Total Weight: " + weight + " kg");
                    if (price != null) tvPrice.setText("Price: ₱" + price);
                    if (orderId != null) tvOrderId.setText("Order #" + orderId);

                    // Update Timeline Visuals
                    updateTimelineUI(status);

                    // Handle "Ready" state logic
                    if ("Ready".equalsIgnoreCase(status)) {
                        readyBanner.setVisibility(View.VISIBLE);
                        tvStatusBanner.setText("Order #" + (orderId != null ? orderId : "") + " is READY!");
                        handleReadyState();
                    } else {
                        readyBanner.setVisibility(View.GONE);
                        tvSecurityCode.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleReadyState() {
        // 1. Manage the 6-Digit Code
        mDatabase.child("pickupCode").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Object code = task.getResult().getValue();
                if (code != null) {
                    tvSecurityCode.setText("Security Code: " + code.toString());
                    tvSecurityCode.setVisibility(View.VISIBLE);
                } else {
                    String newCode = String.valueOf((int)(Math.random() * 900000) + 100000);
                    mDatabase.child("pickupCode").setValue(newCode);
                }
            }
        });

        // 2. Return Method Selection
        mDatabase.child("returnMethod").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() == null) {
                showReturnMethodDialog();
            }
        });
    }

    private void showReturnMethodDialog() {
        String[] options = {"Self Pickup", "Home Delivery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Laundry Ready! Choose Return Method:");
        builder.setItems(options, (dialog, which) -> {
            String choice = options[which];
            mDatabase.child("returnMethod").setValue(choice);
            Toast.makeText(this, "You selected: " + choice, Toast.LENGTH_SHORT).show();
        });
        builder.setCancelable(false);
        builder.show();
    }

    // --- UI Helpers for Timeline ---

    private void setupStepStaticContent() {
        setStepData(step1, R.drawable.ic_clipboard, "Received");
        setStepData(step2, R.drawable.ic_washing_machine, "Washing");
        setStepData(step3, R.drawable.ic_drying, "Drying");
        setStepData(step4, R.drawable.ic_folding, "Folding");
        setStepData(step5, R.drawable.ic_check_circle, "Ready");
        View line5 = step5.findViewById(R.id.line);
        if (line5 != null) line5.setVisibility(View.GONE);
    }

    private void setStepData(View view, int iconRes, String title) {
        ((ImageView) view.findViewById(R.id.stageIcon)).setImageResource(iconRes);
        ((TextView) view.findViewById(R.id.stepTitle)).setText(title);
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
        int color = getResources().getColor(isFinal ? R.color.la_buddy_green : R.color.la_buddy_blue);
        view.findViewById(R.id.statusIcon).setBackgroundResource(isFinal ? R.drawable.dot_green : R.drawable.dot_blue);
        ((TextView) view.findViewById(R.id.stepTitle)).setTextColor(color);
        view.findViewById(R.id.line).setBackgroundColor(color);
    }

    private void resetAllSteps() {
        int grey = getResources().getColor(R.color.grey_inactive);
        View[] steps = {step1, step2, step3, step4, step5};
        for (View s : steps) {
            s.findViewById(R.id.statusIcon).setBackgroundResource(R.drawable.dot_grey);
            ((TextView) s.findViewById(R.id.stepTitle)).setTextColor(grey);
            s.findViewById(R.id.line).setBackgroundColor(grey);
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}