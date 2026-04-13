package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText; // Correct import for standard fields
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
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // FIX: Match the XML tags exactly
        final EditText emailField = findViewById(R.id.emailAddress);
        final TextInputEditText passwordField = findViewById(R.id.passwordEditText);
        final EditText phoneField = findViewById(R.id.phoneNumber);

        AppCompatButton btnSignUp = findViewById(R.id.btnSignUp);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        if (btnSignUp != null) {
            btnSignUp.setOnClickListener(v -> {
                // Null checks to prevent those yellow warnings
                if (emailField == null || passwordField == null || phoneField == null) return;

                String email = emailField.getText().toString().trim();
                String password = passwordField.getText().toString().trim();
                String phone = phoneField.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                                String uid = mAuth.getCurrentUser().getUid();
                                saveUserToDatabase(uid, email, phone);
                            } else {
                                String error = (task.getException() != null) ? task.getException().getMessage() : "Unknown Error";
                                Toast.makeText(this, "Registration Failed: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
            });
        }

        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LoginActivity.class)));
        }
    }

    private void saveUserToDatabase(String uid, String email, String phone) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("status", "Received");
        userMap.put("name", email.split("@")[0]);

        mDatabase.child("Orders").child(uid).setValue(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Account Registered!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                    finish();
                });
    }
}