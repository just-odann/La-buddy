package com.example.la_buddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

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
import android.view.View;
import android.widget.Button;

public class HomeActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String currentUid;

    private TextView tvUserName, tvOrderId, tvWeight, tvPrice, tvSecurityCode;
    private View activeOrderCard, timelineContainer;
    private ImageView btnLogout;
    private FloatingActionButton fabContact;
    private View btnBookWash;
    private CardView cardStoreInfo, cardServiceMenu;
    private View step1, step2, step3, step4, step5, step6, step7, step8;
    private TextView tvAdminMessage;
    private Button btnConfirmReceipt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnConfirmReceipt = findViewById(R.id.btnConfirmReceipt);

        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            goToLogin();
            return;
        }

        // 1. BIND VIEWS
        tvUserName = findViewById(R.id.userName);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvWeight = findViewById(R.id.tvWeight);
        tvPrice = findViewById(R.id.tvPrice);
        tvAdminMessage = findViewById(R.id.tvAdminMessage);
        tvSecurityCode = findViewById(R.id.tvSecurityCode);
        timelineContainer = findViewById(R.id.timelineContainer);
        activeOrderCard = findViewById(R.id.activeOrderCard);

        btnLogout = findViewById(R.id.btnLogout);
        fabContact = findViewById(R.id.fabContact);
        btnBookWash = findViewById(R.id.btnBookWash);
        cardServiceMenu = findViewById(R.id.cardServiceMenu);
        cardStoreInfo = findViewById(R.id.cardStoreInfo);

        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        step4 = findViewById(R.id.step4);
        step5 = findViewById(R.id.step5);
        step6 = findViewById(R.id.step6);
        step7 = findViewById(R.id.step7);
        step8 = findViewById(R.id.step8);

        // 2. USER GREETING
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUid);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String realName = task.getResult().child("name").getValue(String.class);
                if (tvUserName != null && realName != null) {
                    tvUserName.setText("Good morning, " + realName + "!");
                }
            }
        });

        // 3. ATTACH BUTTON LISTENERS
        setupButtonListeners();


        // 4. FIREBASE LIVE LISTENER
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Orders").child(currentUid);
        hideBookingUI();
        setupStepStaticContent();

        mDatabase.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    String serviceType = snapshot.child("serviceType").getValue(String.class);

                    if (status != null && !status.equalsIgnoreCase("None")) {
                        showBookingUI();
                        updateTimelineUI(status, serviceType != null ? serviceType : "Pickup");

                        // --- 1. THE "ORDER COMPLETE" BUTTON LOGIC ---
                        // Show the button ONLY if the laundry is actually with them or ready
                        if (btnConfirmReceipt != null) {
                            if ("Ready".equalsIgnoreCase(status) ||
                                    "Delivered".equalsIgnoreCase(status) ||
                                    "Picked Up".equalsIgnoreCase(status)) {

                                btnConfirmReceipt.setVisibility(View.VISIBLE);

                                btnConfirmReceipt.setOnClickListener(v -> {
                                    snapshot.getRef().child("status").setValue("Completed");
                                    snapshot.getRef().child("adminMessage").setValue("Order Finished. Thank you!");

                                    // Use HomeActivity.this instead of getContext()
                                    Toast.makeText(HomeActivity.this, "Transaction Completed!", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                btnConfirmReceipt.setVisibility(View.GONE);
                            }
                        }
                        // --- FETCH THE ADMIN MESSAGE ---
                        String adminMsg = snapshot.child("adminMessage").getValue(String.class);
                        if (tvAdminMessage != null) {
                            if (adminMsg != null && !adminMsg.isEmpty()) {
                                tvAdminMessage.setText("Admin Note: " + adminMsg);
                                tvAdminMessage.setVisibility(View.VISIBLE);
                            } else {
                                tvAdminMessage.setVisibility(View.GONE);
                            }
                        }

                        // --- YOUR EXISTING PRICE & WEIGHT LOGIC ---
                        Object weightObj = snapshot.child("weight").getValue();
                        Object priceObj = snapshot.child("price").getValue();

                        String weight = (weightObj != null) ? String.valueOf(weightObj) : "0";
                        String price = (priceObj != null) ? String.valueOf(priceObj) : "0";

                        if (tvWeight != null) tvWeight.setText("Weight: " + weight + " kg");
                        if (tvPrice != null) tvPrice.setText("₱" + price);
                        if (tvOrderId != null) tvOrderId.setText("Order Tracking Active");

                        if ("Ready".equalsIgnoreCase(status)) {
                            handleReadyState(snapshot);
                        } else if (tvSecurityCode != null) {
                            tvSecurityCode.setVisibility(View.GONE);
                        }
                    } else {
                        hideBookingUI();
                    }
                } else {
                    hideBookingUI();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupButtonListeners() {
        // 1. CHAT BUTTON (The one you just asked about)
        if (fabContact != null) {
            fabContact.setOnClickListener(v -> {
                // This creates a quick popup for the user to type
                EditText messageInput = new EditText(this);
                messageInput.setHint("e.g., Please use extra fabcon...");

                new android.app.AlertDialog.Builder(this)
                        .setTitle("Message to Admin")
                        .setView(messageInput)
                        .setPositiveButton("Send", (dialog, which) -> {
                            String userMsg = messageInput.getText().toString().trim();
                            if (!userMsg.isEmpty()) {
                                // This sends the message to the EXACT same place
                                // your Admin App is watching.
                                mDatabase.child("userMessage").setValue(userMsg);
                                Toast.makeText(this, "Message sent to Admin!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // 2. BOOKING BUTTON
        if (btnBookWash != null) {
            btnBookWash.setOnClickListener(v -> startActivity(new Intent(this, BookingActivity.class)));
        }

        // 3. SERVICE MENU
        if (cardServiceMenu != null) {
            cardServiceMenu.setOnClickListener(v -> new PriceMenuSheet().show(getSupportFragmentManager(), "PriceMenuSheet"));
        }

        // 4. MAP / STORE INFO
        if (cardStoreInfo != null) {
            cardStoreInfo.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("geo:14.1167,122.9500?q=Laundry+Shop");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            });
        }

        // 5. LOGOUT
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                goToLogin();
            });
        }

        // Navigation Bar
        findViewById(R.id.navHome).setOnClickListener(v -> Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navHistory).setOnClickListener(v -> Toast.makeText(this, "History Coming Soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navSettings).setOnClickListener(v -> Toast.makeText(this, "Settings Coming Soon", Toast.LENGTH_SHORT).show());
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
        setStepData(step6, R.drawable.ic_delivery, "Stage 6", "Out for Delivery");
        setStepData(step7, R.drawable.ic_home, "Stage 7", "Delivered");
        setStepData(step8, R.drawable.ic_check_circle, "Stage 8", "Completed");
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

    private void updateTimelineUI(String currentStatus, String serviceType) {
        resetAllSteps();

        // Hide steps if it's a Pickup service
        if ("Pickup".equalsIgnoreCase(serviceType)) {
            if (step6 != null) step6.setVisibility(View.GONE);
            if (step7 != null) step7.setVisibility(View.GONE);
            setStepData(step8, R.drawable.ic_check_circle, "Stage 6", "Picked Up");
        } else {
            if (step6 != null) step6.setVisibility(View.VISIBLE);
            if (step7 != null) step7.setVisibility(View.VISIBLE);
            setupStepStaticContent();
        }

        switch (currentStatus.toUpperCase()) {
            case "PICKED UP": case "COMPLETED": highlightStep(step8, true);
            case "DELIVERED": if (step7 != null && step7.getVisibility() == View.VISIBLE) highlightStep(step7, false);
            case "OUT FOR DELIVERY": if (step6 != null && step6.getVisibility() == View.VISIBLE) highlightStep(step6, false);
            case "READY": highlightStep(step5, false);
            case "FOLDING": highlightStep(step4, false);
            case "DRYING": highlightStep(step3, false);
            case "WASHING": highlightStep(step2, false);
            case "RECEIVED": highlightStep(step1, false);
                break;
        }
    }

    private void highlightStep(View view, boolean isFinal) {
        if (view == null) return;
        int color = isFinal ? Color.parseColor("#2E7D32") : Color.parseColor("#1A73E8");
        View dot = view.findViewById(R.id.stepDot);
        View line = view.findViewById(R.id.timelineLine);
        if (dot != null) dot.getBackground().setTint(color);
        if (line != null) line.setBackgroundColor(color);
    }

    private void resetAllSteps() {
        int grey = Color.parseColor("#D1D9E6");
        View[] steps = {step1, step2, step3, step4, step5, step6, step7, step8};
        for (View s : steps) {
            if (s == null) continue;
            View dot = s.findViewById(R.id.stepDot);
            View line = s.findViewById(R.id.timelineLine);
            if (dot != null) dot.getBackground().setTint(grey);
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