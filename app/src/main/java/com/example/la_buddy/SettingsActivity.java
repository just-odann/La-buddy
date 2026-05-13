package com.example.la_buddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private ImageView imgProfile;
    private String currentUid;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 1. Initialize Views
        tvName = findViewById(R.id.tvSettingsUserName);
        tvEmail = findViewById(R.id.tvUserEmail);
        imgProfile = findViewById(R.id.imgSettingsProfile);
        ImageView btnBack = findViewById(R.id.btnBack);
        FloatingActionButton btnEditImage = findViewById(R.id.btnEditImage);
        LinearLayout btnLogOut = findViewById(R.id.btnLogOut);

        currentUid = FirebaseAuth.getInstance().getUid();

        // 2. Register Image Picker Launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        imgProfile.setImageURI(selectedImageUri);
                        uploadImageToFirebase(selectedImageUri);
                    }
                }
        );

        if (currentUid != null) {
            loadUserData();
        }

        setupSettingsRows();

        // 3. Button Listeners
        btnBack.setOnClickListener(v -> finish());

        btnEditImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnLogOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUid);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    if (name != null) tvName.setText(name);
                    if (email != null) tvEmail.setText(email);

                    if (imageUrl != null && !isDestroyed()) {
                        Glide.with(SettingsActivity.this)
                                .load(imageUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile)
                                .into(imgProfile);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void uploadImageToFirebase(Uri uri) {
        if (uri == null) return;

        StorageReference fileRef = FirebaseStorage.getInstance().getReference("ProfilePics")
                .child(currentUid + ".jpg");

        fileRef.putFile(uri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                FirebaseDatabase.getInstance().getReference("Users")
                        .child(currentUid)
                        .child("profileImageUrl")
                        .setValue(downloadUri.toString());

                Toast.makeText(this, "Profile Picture Updated!", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("SetTextI18n")
    private void setupSettingsRows() {
        // --- 1. EDIT PROFILE DETAILS ---
        View rowProfile = findViewById(R.id.rowProfile);
        // Ensure the included layout is clickable
        rowProfile.setClickable(true);
        rowProfile.setFocusable(true);
        ((TextView) rowProfile.findViewById(R.id.rowText)).setText("Edit Profile Details");
        ((ImageView) rowProfile.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_profile);
        rowProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        // --- 2. SAVED ADDRESSES ---
        View rowNotifications = findViewById(R.id.rowNotifications);
        rowNotifications.setClickable(true);
        rowNotifications.setFocusable(true);
        ((TextView) rowNotifications.findViewById(R.id.rowText)).setText("Saved Addresses");
        ((ImageView) rowNotifications.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_home);
        rowNotifications.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, AddressManagerActivity.class));
        });

        // --- 3. PASSWORD & SECURITY ---
        View rowSettings = findViewById(R.id.rowSettings);
        rowSettings.setClickable(true);
        rowSettings.setFocusable(true);
        ((TextView) rowSettings.findViewById(R.id.rowText)).setText("Password & Security");
        ((ImageView) rowSettings.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_lock);
        rowSettings.setOnClickListener(v -> showSecurityDialog());

        // --- 4. ABOUT LA-BUDDY ---
        View rowHelp = findViewById(R.id.rowHelp);
        rowHelp.setClickable(true);
        rowHelp.setFocusable(true);
        ((TextView) rowHelp.findViewById(R.id.rowText)).setText("About La-buddy");
        ((ImageView) rowHelp.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_info);
        rowHelp.setOnClickListener(v -> showAboutDialog());
    }

    private void showSecurityDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Security");
        builder.setMessage("Would you like us to send a password reset link to your registered email?");

        builder.setPositiveButton("Send Link", (dialog, which) -> {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            if (email != null) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Reset link sent to your email!", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAboutDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog aboutSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        View sheetView = getLayoutInflater().inflate(R.layout.layout_about_sheet, null);

        Button btnCall = sheetView.findViewById(R.id.btnCallSupport);
        btnCall.setOnClickListener(v -> {
            String phoneNumber = "09123456789";
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open dialer.", Toast.LENGTH_SHORT).show();
            }
        });

        aboutSheet.setContentView(sheetView);
        aboutSheet.show();
    }
}