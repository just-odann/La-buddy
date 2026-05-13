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
import android.widget.Button;

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

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class HomeActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String currentUid;

    private TextView tvUserName, tvOrderId, tvWeight, tvPrice, tvSecurityCode;
    private View activeOrderCard, timelineContainer;
    private ImageView btnLogout, imgProfilePicture;
    private FloatingActionButton fabContact;
    private View btnBookWash;
    private CardView cardStoreInfo, cardServiceMenu;
    private View step1, step2, step3, step4, step5, step6, step7, step8;
    private TextView tvAdminMessage;
    private Button btnConfirmReceipt;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            goToLogin();
            return;
        }

        // 1. BIND VIEWS
        btnConfirmReceipt = findViewById(R.id.btnConfirmReceipt);
        tvUserName = findViewById(R.id.userName);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
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

        // 2. IMAGE PICKER SETUP
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        imgProfilePicture.setImageURI(selectedImageUri);
                        uploadImageToFirebase(selectedImageUri);
                    }
                }
        );

        // 3. SMART GREETING & LIVE USER DATA
        java.util.Calendar c = java.util.Calendar.getInstance();
        int timeOfDay = c.get(java.util.Calendar.HOUR_OF_DAY);
        String greetingText = "Good Evening";
        if (timeOfDay < 12) greetingText = "Good Morning";
        else if (timeOfDay < 17) greetingText = "Good Afternoon";

        final String finalGreeting = greetingText;
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUid);

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("name").getValue(String.class);
                    if (tvUserName != null) {
                        if (fullName != null && !fullName.isEmpty()) {
                            String firstName = fullName.split(" ")[0];
                            tvUserName.setText(finalGreeting + ",\n" + firstName + "! 👋");
                        } else {
                            tvUserName.setText(finalGreeting + ",\nUser! 👋");
                        }
                    }
                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (imageUrl != null && !isDestroyed()) {
                        Glide.with(HomeActivity.this).load(imageUrl).circleCrop().placeholder(R.drawable.ic_profile).into(imgProfilePicture);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 4. ATTACH BUTTON LISTENERS
        setupButtonListeners();

        // 5. FIREBASE LIVE LISTENER
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Orders").child(currentUid);
        hideBookingUI();
        setupStepStaticContent();

        mDatabase.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    String method = snapshot.hasChild("method") ? snapshot.child("method").getValue(String.class) : snapshot.child("serviceType").getValue(String.class);

                    if (status != null && !status.equalsIgnoreCase("None") && !status.equalsIgnoreCase("Completed")) {
                        showBookingUI();
                        updateTimelineUI(status, method != null ? method : "Walk-in");

                        if (btnConfirmReceipt != null) {
                            boolean shouldShow = false;
                            if (method != null && method.contains("Delivery")) {
                                if ("Delivered".equalsIgnoreCase(status)) shouldShow = true;
                            } else {
                                if ("Ready".equalsIgnoreCase(status) || "Picked Up".equalsIgnoreCase(status)) shouldShow = true;
                            }
                            btnConfirmReceipt.setVisibility(shouldShow ? View.VISIBLE : View.GONE);

                            btnConfirmReceipt.setOnClickListener(v -> {
                                btnConfirmReceipt.setVisibility(View.GONE);
                                handleFinalizeOrder(snapshot);
                            });
                        }

                        Object weightObj = snapshot.child("weight").getValue();
                        Object priceObj = snapshot.child("price").getValue();
                        if (tvWeight != null) tvWeight.setText("Weight: " + (weightObj != null ? weightObj : "0"));
                        if (tvPrice != null) tvPrice.setText("₱" + (priceObj != null ? priceObj : "0"));

                        if ("Ready".equalsIgnoreCase(status)) handleReadyState(snapshot);
                        else if (tvSecurityCode != null) tvSecurityCode.setVisibility(View.GONE);
                    } else {
                        hideBookingUI();
                    }
                } else {
                    hideBookingUI();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupButtonListeners() {
        if (imgProfilePicture != null) {
            imgProfilePicture.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            });
        }

        if (fabContact != null) {
            fabContact.setOnClickListener(v -> {
                com.google.android.material.bottomsheet.BottomSheetDialog chatSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
                View sheetView = getLayoutInflater().inflate(R.layout.layout_chat_sheet, null);
                chatSheet.setContentView(sheetView);

                // Keyboard fix
                chatSheet.setOnShowListener(dialog -> {
                    android.widget.FrameLayout bottomSheet = chatSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    if (bottomSheet != null) {
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet).setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                    }
                });

                androidx.recyclerview.widget.RecyclerView rvChat = sheetView.findViewById(R.id.rvChat);
                EditText etMsg = sheetView.findViewById(R.id.etChatMessage);
                android.widget.ImageButton btnSend = sheetView.findViewById(R.id.btnSendChat);

                java.util.List<ChatMessage> chatList = new java.util.ArrayList<>();
                ChatAdapter chatAdapter = new ChatAdapter(chatList, "customer");
                rvChat.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
                rvChat.setAdapter(chatAdapter);

                DatabaseReference chatRef = mDatabase.child("messages");
                chatRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        chatList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ChatMessage m = ds.getValue(ChatMessage.class);
                            if (m != null) chatList.add(m);
                        }
                        chatAdapter.notifyDataSetChanged();
                        if (chatList.size() > 0) rvChat.scrollToPosition(chatList.size() - 1);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

                btnSend.setOnClickListener(view -> {
                    String text = etMsg.getText().toString().trim();
                    if (!text.isEmpty()) {
                        long currentTime = System.currentTimeMillis();
                        ChatMessage newMessage = new ChatMessage(text, "customer", currentTime);
                        chatRef.push().setValue(newMessage);

                        DatabaseReference userUpdateRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUid);
                        java.util.HashMap<String, Object> update = new java.util.HashMap<>();
                        update.put("lastMessage", text);
                        update.put("hasUnread", true);
                        update.put("lastMessageTimestamp", currentTime);
                        userUpdateRef.updateChildren(update);
                        etMsg.setText("");
                    }
                });
                chatSheet.show();
            });
        }

        if (btnBookWash != null) btnBookWash.setOnClickListener(v -> startActivity(new Intent(this, BookingActivity.class)));
        if (cardServiceMenu != null) cardServiceMenu.setOnClickListener(v -> new PriceMenuSheet().show(getSupportFragmentManager(), "PriceMenuSheet"));
        if (cardStoreInfo != null) {
            cardStoreInfo.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("geo:14.1167,122.9500?q=Laundry+Shop");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            });
        }
        if (btnLogout != null) btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            goToLogin();
        });

        findViewById(R.id.navHome).setOnClickListener(v -> Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navHistory).setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, OrderHistoryActivity.class)));
        findViewById(R.id.navSettings).setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));
    }

    private void handleFinalizeOrder(DataSnapshot s) {
        String histItems = s.hasChild("items") ? String.valueOf(s.child("items").getValue()) : String.valueOf(s.child("item").getValue());
        String histCategory = s.hasChild("category") ? String.valueOf(s.child("category").getValue()) : String.valueOf(s.child("laundryType").getValue());
        String histMethod = s.hasChild("method") ? String.valueOf(s.child("method").getValue()) : String.valueOf(s.child("serviceType").getValue());

        java.util.HashMap<String, Object> historyMap = new java.util.HashMap<>();
        historyMap.put("date", new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date()));
        historyMap.put("category", histCategory);
        historyMap.put("items", histItems);
        historyMap.put("method", histMethod);
        historyMap.put("addons", (s.child("detergent").getValue() != null ? s.child("detergent").getValue() : "None") + " | " + (s.child("fabcon").getValue() != null ? s.child("fabcon").getValue() : "None"));
        historyMap.put("weight", s.hasChild("weight") ? String.valueOf(s.child("weight").getValue()) : "Pending");
        historyMap.put("price", "₱" + String.valueOf(s.child("price").getValue()).replace("₱", ""));
        historyMap.put("status", "Picked Up");
        historyMap.put("name", s.hasChild("name") ? String.valueOf(s.child("name").getValue()) : "User");

        DatabaseReference histRef = FirebaseDatabase.getInstance().getReference("OrderHistory").child(currentUid);
        String historyId = s.child("historyId").getValue(String.class);
        DatabaseReference finalHistRef = (historyId != null) ? histRef.child(historyId) : histRef.push();

        finalHistRef.setValue(historyMap).addOnCompleteListener(task -> {
            java.util.HashMap<String, Object> updateStatus = new java.util.HashMap<>();
            updateStatus.put("status", "Completed");
            s.getRef().updateChildren(updateStatus).addOnSuccessListener(aVoid -> {
                resetAllSteps();
                hideBookingUI();
                Toast.makeText(HomeActivity.this, "Order Finalized!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void uploadImageToFirebase(Uri uri) {
        if (uri == null) return;
        StorageReference fileRef = FirebaseStorage.getInstance().getReference("ProfilePics").child(currentUid + ".jpg");
        fileRef.putFile(uri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                FirebaseDatabase.getInstance().getReference("Users").child(currentUid).child("profileImageUrl").setValue(downloadUri.toString());
                Toast.makeText(this, "Profile Picture Updated!", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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