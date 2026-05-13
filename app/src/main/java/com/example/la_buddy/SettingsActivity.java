package com.example.la_buddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

public class SettingsActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private ImageView imgProfile;
    private String currentUid;

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

        // 2. Load User Data from Firebase
        if (currentUid != null) {
            loadUserData();
        }

        // 3. Setup Custom Rows (Text and Icons)
        setupSettingsRows();

        // 4. Button Listeners
        btnBack.setOnClickListener(v -> finish());

        btnEditImage.setOnClickListener(v ->
                Toast.makeText(this, "Image Picker coming soon!", Toast.LENGTH_SHORT).show()
        );

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
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @SuppressLint("SetTextI18n")
    private void setupSettingsRows() {
        // Find the <include> views by their IDs in activity_settings.xml
        View rowProfile = findViewById(R.id.rowProfile);
        View rowNotifications = findViewById(R.id.rowNotifications);
        View rowSettings = findViewById(R.id.rowSettings);
        View rowHelp = findViewById(R.id.rowHelp);

        // Customize Row 1: Profile
        ((TextView) rowProfile.findViewById(R.id.rowText)).setText("My Account");
        ((ImageView) rowProfile.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_profile);

        // Customize Row 2: Notifications
        ((TextView) rowNotifications.findViewById(R.id.rowText)).setText("Notifications");
        ((ImageView) rowNotifications.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_notification);

        // Customize Row 3: App Settings
        ((TextView) rowSettings.findViewById(R.id.rowText)).setText("App Settings");
        ((ImageView) rowSettings.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_settings);

        // Customize Row 4: Help
        ((TextView) rowHelp.findViewById(R.id.rowText)).setText("Help & Support");
        ((ImageView) rowHelp.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_help);
    }
}