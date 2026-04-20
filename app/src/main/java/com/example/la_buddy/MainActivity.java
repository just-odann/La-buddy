package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        // This points to the very top of your Firebase URL
        mDatabase = FirebaseDatabase.getInstance().getReference();

        final EditText emailField = findViewById(R.id.emailAddress);
        final TextInputEditText passwordField = findViewById(R.id.passwordEditText);
        final EditText phoneField = findViewById(R.id.phoneNumber);

        AppCompatButton btnSignUp = findViewById(R.id.btnSignUp);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        // 1. SIGN UP ACTION
        if (btnSignUp != null) {
            btnSignUp.setOnClickListener(v -> {
                // Disable button to prevent the "Email in use" double-click error
                btnSignUp.setEnabled(false);
                btnSignUp.setText("Connecting Buddy...");

                String email = emailField.getText().toString().trim();
                String password = passwordField.getText().toString().trim();
                String phone = phoneField.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText("Sign Up");
                    return;
                }

                // Firebase Auth Call
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                                // This is where we save to the "Users" folder
                                saveUserToDatabase(mAuth.getCurrentUser().getUid(), email, phone);
                            } else {
                                // If it actually fails, let them try again
                                btnSignUp.setEnabled(true);
                                btnSignUp.setText("Sign Up");
                                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            });
        }

        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                // FORCE SIGN OUT: This ensures LoginActivity won't
                // automatically redirect back to HomeActivity.
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);

                // Optional: Use finish() if you don't want the user
                // to go back to the Sign Up screen using the back button.
                finish();
            });
        }
    }

    private void saveUserToDatabase(String uid, String email, String phone) {
        // 1. Prepare the Profile Data
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid); // Unique ID from Gmail/Auth
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("name", email.split("@")[0]);

        // 2. ADD THE "MAP CONNECTION" FIELDS
        // These act as placeholders so the Map knows where to save data later
        userMap.put("latitude", 14.1167);  // Default to Daet/CNSC area coordinates
        userMap.put("longitude", 122.9500);
        userMap.put("address", "No address set yet");

        // 3. THE DATABASE PATH
        // Saving to "Users" keeps the Admin App clean, but prepares the Map data
        mDatabase.child("Users").child(uid).setValue(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Gmail Profile Connected!", Toast.LENGTH_SHORT).show();

                    // GO TO HOME
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Connection Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

}