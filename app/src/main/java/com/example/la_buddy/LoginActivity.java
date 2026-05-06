package com.example.la_buddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// NEW IMPORTS FOR THE TRAFFIC COP
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // 1. If already logged in, check role instead of just going to Home
        if (currentUser != null) {
            checkUserRole();
            return;
        }

        setContentView(R.layout.activity_login);

        final EditText emailField = findViewById(R.id.loginEmail);
        final TextInputLayout passwordLayout = findViewById(R.id.passwordInputLayout);
        final TextInputEditText passwordEditText = findViewById(R.id.passwordEditText);
        AppCompatButton loginButton = findViewById(R.id.btnLogin);
        TextView signUpRedirect = findViewById(R.id.tvSignUpLink);

        if (passwordLayout != null && passwordEditText != null) {
            passwordLayout.setEndIconVisible(false);
            passwordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    passwordLayout.setEndIconVisible(s.length() > 0);
                }
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (signUpRedirect != null) {
            signUpRedirect.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            });
        }

        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                String email = emailField.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Welcome back, Buddy!", Toast.LENGTH_SHORT).show();
                                // 2. Check role upon successful login
                                checkUserRole();
                            } else {
                                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login Failed";
                                Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
            });
        }

        // GOOGLE SIGN-IN PLACEHOLDER
        View btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> {
                Toast.makeText(LoginActivity.this, "Google Sign-In is currently in development!", Toast.LENGTH_LONG).show();
            });
        }
    }

    // --- PHASE 2: THE TRAFFIC COP METHOD ---
    private void checkUserRole() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        // Look into the Users table to find this specific person's profile
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {

                // Grab the role (it might be null if they are a regular user)
                String role = task.getResult().child("role").getValue(String.class);

                Intent intent;
                // If they have the admin badge, send them to the Dashboard
                if (role != null && role.equalsIgnoreCase("admin")) {
                    intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                } else {
                    // Otherwise, send them to the regular Customer view
                    intent = new Intent(LoginActivity.this, HomeActivity.class);
                }

                // Clear the backstack so they can't hit "Back" and go to the login screen
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();

            } else {
                // FALLBACK: If there's an internet error or their node is missing, default to regular Customer
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}