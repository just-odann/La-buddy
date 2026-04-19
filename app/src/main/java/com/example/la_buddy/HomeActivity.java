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
    private View activeOrderCard;
    private ImageView btnLogout;
    private FloatingActionButton fabContact;

    // Added for new UI elements
    private View btnBookWash;
    private CardView cardPriceList, cardStoreInfo;

    private View step1, step2, step3, step4, step5;
    private LinearLayout navHome, navHistory, navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            goToLogin();
            return;
        }
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Orders").child(currentUid);

        // 1. BIND VIEWS
        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        step4 = findViewById(R.id.step4);
        step5 = findViewById(R.id.step5);

        tvUserName = findViewById(R.id.userName);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvWeight = findViewById(R.id.tvWeight);
        tvPrice = findViewById(R.id.tvPrice);

        // RE-ENABLED: This is needed for the Stage 5 Ready state
        tvSecurityCode = findViewById(R.id.tvSecurityCode);

        activeOrderCard = findViewById(R.id.activeOrderCard);
        btnLogout = findViewById(R.id.btnLogout);
        fabContact = findViewById(R.id.fabContact);

        // BIND NEW BUTTONS
        btnBookWash = findViewById(R.id.btnBookWash);
        cardPriceList = findViewById(R.id.cardPriceList);
        cardStoreInfo = findViewById(R.id.cardStoreInfo);

        navHome = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navSettings = findViewById(R.id.navSettings);

        if (activeOrderCard != null) activeOrderCard.setVisibility(View.GONE);
        setupStepStaticContent();

        // 2. LISTENERS
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                goToLogin();
            });
        }

        // NEW: Book a Wash Listener
        if (btnBookWash != null) {
            btnBookWash.setOnClickListener(v -> {
                // Create the Intent to go from Home to Booking
                Intent intent = new Intent(HomeActivity.this, BookingActivity.class);
                startActivity(intent);
            });
        }

        // NEW: Price List Listener (Using the Bottom Sheet we discussed)
        if (cardPriceList != null) {
            cardPriceList.setOnClickListener(v -> {
                // This triggers the Price Menu sliding panel
                PriceMenuSheet priceSheet = new PriceMenuSheet();
                priceSheet.show(getSupportFragmentManager(), "PriceMenu");
            });
        }

        // NEW: Store Info / Map Listener
        if (cardStoreInfo != null) {
            cardStoreInfo.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("geo:14.2833,122.7833?q=Laundry+Shop");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            });
        }

        if (fabContact != null) {
            fabContact.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:09123456789"));
                startActivity(intent);
            });
        }

        // 3. FIREBASE LIVE LISTENER
        mDatabase.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    activeOrderCard.setVisibility(View.VISIBLE);

                    String name = snapshot.child("name").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);
                    String weight = snapshot.child("weight").getValue(String.class);
                    String price = snapshot.child("price").getValue(String.class);
                    String orderId = snapshot.child("orderId").getValue(String.class);

                    if (name != null) tvUserName.setText("Good morning, " + name + "!");
                    if (weight != null) tvWeight.setText("Weight: " + weight + " kg");
                    if (price != null) tvPrice.setText("₱" + price);
                    if (orderId != null) tvOrderId.setText("Order #" + orderId);

                    updateTimelineUI(status);

                    // Show claim code only if Ready
                    if ("Ready".equalsIgnoreCase(status)) {
                        handleReadyState();
                    } else if (tvSecurityCode != null) {
                        tvSecurityCode.setVisibility(View.GONE);
                    }
                } else {
                    activeOrderCard.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ... (rest of the helper methods: setupStepStaticContent, updateTimelineUI, etc. remain the same)

    private void setupStepStaticContent() {
        setStepData(step1, R.drawable.ic_clipboard, "Stage 1", "Received");
        setStepData(step2, R.drawable.ic_washing_machine, "Stage 2", "Washing");
        setStepData(step3, R.drawable.ic_drying, "Stage 3", "Drying");
        setStepData(step4, R.drawable.ic_folding, "Stage 4", "Folding");
        setStepData(step5, R.drawable.ic_check_circle, "Stage 5", "Ready");

        View line5 = step5.findViewById(R.id.timelineLine);
        if (line5 != null) line5.setVisibility(View.GONE);
    }

    private void setStepData(View view, int iconRes, String stageLabel, String title) {
        if (view == null) return;
        ((ImageView) view.findViewById(R.id.stepIcon)).setImageResource(iconRes);
        ((TextView) view.findViewById(R.id.tvStepLabel)).setText(stageLabel);
        ((TextView) view.findViewById(R.id.tvStepTitle)).setText(title);
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
        if (dot != null && dot.getBackground() != null) {
            dot.getBackground().setTint(color);
        }

        View line = view.findViewById(R.id.timelineLine);
        if (line != null) line.setBackgroundColor(color);

        ((TextView) view.findViewById(R.id.tvStepTitle)).setTextColor(color);
        view.findViewById(R.id.stepIcon).setAlpha(1.0f);
    }

    private void resetAllSteps() {
        int grey = Color.parseColor("#D1D9E6");
        View[] steps = {step1, step2, step3, step4, step5};
        for (View s : steps) {
            if (s == null) continue;
            ((TextView) s.findViewById(R.id.tvStepTitle)).setTextColor(Color.GRAY);
            s.findViewById(R.id.timelineLine).setBackgroundColor(grey);

            View dot = s.findViewById(R.id.stepDot);
            if (dot != null && dot.getBackground() != null) {
                dot.getBackground().setTint(grey);
            }
            s.findViewById(R.id.stepIcon).setAlpha(0.4f);
        }
    }

    private void handleReadyState() {
        if (tvSecurityCode == null) return;
        mDatabase.child("pickupCode").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                tvSecurityCode.setText("Claim Code: " + task.getResult().getValue().toString());
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